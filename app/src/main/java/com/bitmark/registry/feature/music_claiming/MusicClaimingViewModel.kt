package com.bitmark.registry.feature.music_claiming

import androidx.lifecycle.Lifecycle
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.remote.api.response.DownloadAssetFileResponse
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.encryption.AssetEncryption
import com.bitmark.registry.util.encryption.BoxEncryption
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.AssetClaimingModelView
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-08-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MusicClaimingViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel(lifecycle) {

    private val getMusicClaimingInfoLiveData =
        CompositeLiveData<AssetClaimingModelView>()

    private val downloadAssetLiveData = CompositeLiveData<File>()

    private val prepareDownloadLiveData =
        CompositeLiveData<Pair<String, String>>()

    internal val downloadProgressLiveData = BufferedLiveData<Int>(lifecycle)

    internal val bitmarksSavedLiveData =
        BufferedLiveData<List<BitmarkData>>(lifecycle)

    internal val bitmarkDeletedLiveData =
        BufferedLiveData<Pair<String, BitmarkData.Status>>(lifecycle)

    internal fun getMusicClaimingInfoLiveData() =
        getMusicClaimingInfoLiveData.asLiveData()

    internal fun downloadAssetLiveData() = downloadAssetLiveData.asLiveData()

    internal fun prepareDownloadLiveData() =
        prepareDownloadLiveData.asLiveData()

    internal fun getMusicClaimingInfo(
        assetId: String,
        bitmarkEdition: Int?
    ) =
        getMusicClaimingInfoLiveData.add(
            rxLiveDataTransformer.single(
                bitmarkRepo.getAssetClaimingInfo(assetId).map { claimInfo ->
                    AssetClaimingModelView(
                        assetId,
                        claimInfo.totalEditionLeft,
                        claimInfo.limitedEdition,
                        bitmarkEdition
                    )
                })
        )

    internal fun prepareDownload() {
        prepareDownloadLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountNumber(),
                    accountRepo.getKeyAlias(),
                    BiFunction { accountNumber, keyAlias ->
                        Pair(
                            accountNumber,
                            keyAlias
                        )
                    })
            )
        )
    }

    internal fun downloadAssetFile(
        assetId: String,
        receiver: String,
        receiverEncKeyPair: KeyPair
    ) {
        downloadAssetLiveData.add(
            rxLiveDataTransformer.single(
                bitmarkRepo.getDownloadableAssets(receiver).flatMap { fileIds ->
                    val index =
                        fileIds.indexOfFirst { id -> id.contains(assetId) }
                    if (index != -1) {
                        val fileId = fileIds[index]
                        val sender = fileId.split("/")[1]
                        downloadAssetFileStream(
                            assetId,
                            sender,
                            receiver,
                            receiverEncKeyPair
                        )
                    } else {
                        Single.error<File>(Throwable("This asset is not permitted to download"))
                    }
                }
            )
        )
    }

    private fun downloadAssetFileStream(
        assetId: String,
        sender: String,
        receiver: String,
        receiverEncKeyPair: KeyPair
    ): Single<File> {

        val progress: (Int) -> Unit = { percent ->
            downloadProgressLiveData.set(percent)
        }

        val downloadAssetStream = bitmarkRepo.downloadAssetFile(
            assetId,
            sender,
            receiver,
            progress
        )
        val getSenderEncKeyStream = accountRepo.getEncPubKey(sender)
        return Single.zip(
            downloadAssetStream,
            getSenderEncKeyStream,
            BiFunction<DownloadAssetFileResponse, ByteArray, Pair<DownloadAssetFileResponse, ByteArray>> { downloadRes, senderEncPubKey ->
                Pair(downloadRes, senderEncPubKey)
            }).flatMap { p ->

            val downloadRes = p.first
            val senderEncPubKey = p.second

            val keyDecryptor =
                BoxEncryption(receiverEncKeyPair.privateKey().toBytes())
            val secretKey =
                downloadRes.sessionData.getRawKey(keyDecryptor, senderEncPubKey)
            val assetEncryption = AssetEncryption(secretKey)
            val fileContent = downloadRes.fileContent

            val rawContent = assetEncryption.decrypt(fileContent)

            bitmarkRepo.saveAssetFile(
                receiver,
                assetId,
                downloadRes.fileName,
                rawContent
            )
        }.flatMap { file ->
            bitmarkRepo.deleteRemoteAssetFile(assetId, sender, receiver)
                .andThen(Single.just(file))
        }
    }

    override fun onCreate() {
        super.onCreate()
        realtimeBus.bitmarkSavedPublisher.subscribe(this) { bitmarks ->
            bitmarksSavedLiveData.set(bitmarks)
        }

        realtimeBus.bitmarkDeletedPublisher.subscribe(this) { p ->
            bitmarkDeletedLiveData.set(p)
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        super.onDestroy()
    }
}