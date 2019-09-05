package com.bitmark.registry.feature.sync

import android.util.Log
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.google_drive.GoogleDriveService
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.UniqueConcurrentLinkedDeque
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import java.util.concurrent.atomic.AtomicBoolean


/**
 * @author Hieu Pham
 * @since 2019-08-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

class AssetSynchronizer(
    private val googleDriveService: GoogleDriveService,
    private val bitmarkRepo: BitmarkRepository,
    private val accountRepo: AccountRepository,
    private val realtimeBus: RealtimeBus
) {

    companion object {
        private const val TAG = "AssetSynchronizer"

        private const val QUOTA_EXCEEDED_BUFFER = 200 * 1024 * 1024 // 200 MB
    }

    private val taskQueue = UniqueConcurrentLinkedDeque<Completable>()

    private val isProcessing = AtomicBoolean(false)

    private val isPaused = AtomicBoolean(false)

    private val compositeDisposable = CompositeDisposable()

    private var taskProcessListener: TaskProcessListener? = null

    private var cloudServiceListener: CloudServiceListener? = null

    fun setTaskProcessingListener(listener: TaskProcessListener?) {
        this.taskProcessListener = listener
    }

    fun setCloudServiceListener(listener: CloudServiceListener?) {
        this.cloudServiceListener = listener
    }

    fun start() {
        Log.d(TAG, "starting...")
        googleDriveService.setServiceReadyListener(object :
            GoogleDriveService.ServiceReadyListener {
            override fun onReady() {
                resume()
            }

            override fun onPause() {
                pause()
            }

        })

        realtimeBus.assetFileSavedPublisher.subscribe(this) { assetId ->
            Log.d(TAG, "asset file save for $assetId, process to upload")
            process(assetId, upload(assetId))
        }

        realtimeBus.assetsSavedPublisher.subscribe(this) { p ->
            val isNewRec = p.second
            if (isNewRec) {
                val assetId = p.first.id
                process(assetId, download(assetId))
            }
        }

        googleDriveService.start()

    }

    fun stop() {
        compositeDisposable.dispose()
        realtimeBus.unsubscribe(this)
        googleDriveService.setServiceReadyListener(null)
        taskQueue.clear()
        isProcessing.set(false)
        isPaused.set(false)
        googleDriveService.stop()
        Log.d(TAG, "stopped")
    }

    fun resume() {
        Log.d(TAG, "resume")
        isPaused.set(false)
        taskQueue.addFirst(element = sync())
        taskQueue.addFirst(element = checkServiceQuotaExceeded())
        execute()
    }

    fun pause() {
        isPaused.set(true)
        Log.d(TAG, "paused")
    }

    private fun checkServiceQuotaExceeded() =
        googleDriveService.checkQuota().map { p ->
            val limit = p.first
            val usage = p.second
            Log.d(
                TAG,
                "quota: limit is ${limit / 1024 / 1024}MB and usage is ${usage / 1024 / 1024}MB"
            )
            val exceeded = usage >= limit
            val almostExceeded = usage + QUOTA_EXCEEDED_BUFFER >= limit
            when {
                exceeded -> {
                    cloudServiceListener?.onQuotaExceeded()
                }
                almostExceeded -> {
                    cloudServiceListener?.onQuotaAlmostExceeded()
                }
                else -> {
                    // quota is enough to use
                }
            }
        }.ignoreElement()

    private fun process(taskId: String, task: Completable) {
        if (isProcessing.get() || isPaused.get()) {
            if (taskQueue.add(taskId, task)) {
                Log.d(TAG, "added task with id $taskId to the pending queue")
            }
            return
        }
        Log.d(TAG, "start processing for task id $taskId...")
        subscribe(task.onErrorResumeNext { e ->
            if (e is IllegalStateException) {
                // service is not ready, re-add the task
                taskQueue.add(taskId, task)
            }
            Completable.error(e)
        }.doOnSubscribe { isProcessing.set(true) }
            .doAfterTerminate {
                isProcessing.set(false)
                execute()
            }.doOnDispose {
                isProcessing.set(false)
            }.subscribe(
                { taskProcessListener?.onSuccess() },
                { e ->
                    if (e is CompositeException) {
                        e.exceptions.forEach { ex ->
                            taskProcessListener?.onError(ex)
                            Log.e(
                                TAG,
                                "${ex.javaClass}-${ex.message}"
                            )
                        }
                    } else {
                        taskProcessListener?.onError(e)
                        Log.e(TAG, "${e.javaClass}-${e.message}")
                    }
                })
        )
    }

    private fun sync() = Completable.mergeArrayDelayError(download(), upload())

    private fun execute() {
        if (taskQueue.isEmpty()) return
        val task = taskQueue.poll()
        process(task.first, task.second)
    }

    private fun download(assetId: String) =
        accountRepo.getAccountNumber().flatMapCompletable { accountNumber ->
            bitmarkRepo.checkAssetFile(accountNumber, assetId)
                .map { p -> p.second != null }.flatMapCompletable { existing ->
                    if (existing) {
                        Log.d(TAG, "local file for asset $assetId is existing")
                        Completable.complete()
                    } else {
                        googleDriveService.listAppDataFiles(
                            folderName = accountNumber,
                            partialName = assetId
                        ).flatMapCompletable { files ->
                            if (files.isEmpty()) {
                                Log.d(
                                    TAG,
                                    "remote file for asset $assetId is not existing"
                                )
                                Completable.complete()
                            } else {
                                val file = files[0]
                                val fileId = file.id
                                val parsedName =
                                    parseCloudStorageFileName(file.name)
                                Log.d(
                                    TAG,
                                    "start downloading file id $fileId, name ${file.name}..."
                                )
                                googleDriveService.download(fileId)
                                    .flatMapCompletable { content ->
                                        Log.d(
                                            TAG,
                                            "downloaded file id $fileId with size ${content.size / 1024} KB"
                                        )
                                        bitmarkRepo.saveAssetFile(
                                            accountNumber,
                                            parsedName.first,
                                            parsedName.second,
                                            content
                                        ).ignoreElement()
                                    }
                            }
                        }
                    }
                }
        }

    private fun download() =
        determineDownloadAssets().flatMapCompletable { assetIds ->
            if (assetIds.isEmpty()) {
                Completable.complete()
            } else {
                val downloadStreams = mutableListOf<Completable>()

                assetIds.forEach { assetId ->
                    downloadStreams.add(download(assetId))
                }

                Completable.mergeDelayError(downloadStreams)
            }
        }

    private fun upload(assetId: String) =
        accountRepo.getAccountNumber().flatMapCompletable { accountNumber ->
            bitmarkRepo.checkAssetFile(accountNumber, assetId)
                .flatMapCompletable { p ->
                    val assetId = p.first
                    val file = p.second
                    if (file == null) {
                        Log.d(TAG, "local file for asset $assetId is null")
                        Completable.complete()
                    } else {
                        googleDriveService.checkExistingFile(
                            accountNumber,
                            assetId
                        ).flatMapCompletable { existing ->
                            if (existing) {
                                Log.d(
                                    TAG,
                                    "remote file for asset id $assetId is existing"
                                )
                                Completable.complete()
                            } else {
                                val fileName =
                                    canonicalCloudStorageFileName(
                                        assetId,
                                        file.name
                                    )
                                Log.d(
                                    TAG,
                                    "start uploading $fileName with size ${file.length() / 1024} KB ..."
                                )
                                googleDriveService.upload(
                                    fileName,
                                    accountNumber,
                                    file.absolutePath
                                ).doAfterSuccess {
                                    Log.d(
                                        TAG,
                                        "uploaded file id ${it.id} with size ${file.length() / 1024} KB"
                                    )
                                }.ignoreElement()
                            }
                        }
                    }
                }
        }

    private fun upload() =
        determineUploadAssets().flatMapCompletable { assetIds ->
            if (assetIds.isEmpty()) {
                Completable.complete()
            } else {
                val uploadStreams = mutableListOf<Completable>()

                assetIds.forEach { assetId ->
                    uploadStreams.add(upload(assetId))
                }

                Completable.mergeDelayError(uploadStreams)
            }
        }

    private fun determineUploadAssets() =
        accountRepo.getAccountNumber().flatMap { accountNumber ->
            Single.zip(
                listCloudStoredAssetIds(accountNumber),
                listLocalStoredAssetIdsStream(accountNumber),
                BiFunction<List<String>, List<String>, List<String>> { remote, local ->
                    val uploadAssetIds = mutableListOf<String>()
                    local.forEach { id ->
                        if (!remote.contains(id)) uploadAssetIds.add(
                            id
                        )
                    }
                    uploadAssetIds.toList()
                })
        }

    private fun determineDownloadAssets() =
        accountRepo.getAccountNumber().flatMap { accountNumber ->
            Single.zip(
                listCloudStoredAssetIds(accountNumber),
                listLocalStoredAssetIdsStream(accountNumber),
                listAssetIdsRefOwnedBitmark(accountNumber),
                Function3<List<String>, List<String>, List<String>, List<String>> { remoteIds, localIds, needIds ->
                    val expect = mutableListOf<String>()
                    needIds.forEach { id ->
                        if (!localIds.contains(id) && remoteIds.contains(id)) {
                            expect.add(id)
                        }
                    }
                    expect.toList()
                })
        }

    private fun listCloudStoredAssetIds(owner: String) =
        googleDriveService.listAppDataFiles(owner)
            .map { files ->
                if (files.isEmpty()) listOf() else files.map { f ->
                    parseCloudStorageFileName(
                        f.name
                    ).first
                }
            }

    private fun listLocalStoredAssetIdsStream(owner: String) =
        bitmarkRepo.listStoredAssetFile(owner)
            .map { files -> if (files.isEmpty()) listOf() else files.map { f -> f.name } }

    private fun listAssetIdsRefOwnedBitmark(owner: String) =
        bitmarkRepo.listStoredOwnedBitmarks(owner)
            .map { bitmarks ->
                if (bitmarks.isEmpty()) listOf()
                else {
                    bitmarks.distinctBy { b -> b.assetId }
                        .map { b -> b.assetId }
                }
            }

    // return a pair of asset id and file name
    private fun parseCloudStorageFileName(name: String): Pair<String, String> {
        val splitName = name.split("-")
        if (splitName.size < 2) throw IllegalArgumentException("invalid name")
        return Pair(splitName[0], splitName[1])
    }

    private fun canonicalCloudStorageFileName(assetId: String, name: String) =
        "$assetId-$name"

    private fun subscribe(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    interface TaskProcessListener {

        fun onSuccess() {}

        fun onError(e: Throwable) {}
    }

    interface CloudServiceListener {

        fun onQuotaExceeded()

        fun onQuotaAlmostExceeded()
    }

}