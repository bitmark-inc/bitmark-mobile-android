package com.bitmark.registry.feature.google_drive

import android.content.Context
import android.webkit.MimeTypeMap
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class GoogleDriveService @Inject constructor(
    private val context: Context,
    private val realtimeBus: RealtimeBus
) {

    private var service: Drive? = null

    private var serviceReadyListener: ServiceReadyListener? = null

    fun setServiceReadyListener(listener: ServiceReadyListener?) {
        this.serviceReadyListener = listener
    }

    fun start() {
        realtimeBus.actionRequiredAddedPublisher.subscribe(this) { actionIds ->
            if (actionIds.contains(ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION)) {
                // end google session or skip authorize google drive
                service = null
                serviceReadyListener?.onPause()
            }
        }

        realtimeBus.actionRequiredDeletedPublisher.subscribe(this) { actionId ->
            if (actionId == ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION) {
                startService()
            }
        }

        startService()
    }

    private fun startService() {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return
        service = buildService(account)
        serviceReadyListener?.onReady()
    }

    fun stop() {
        realtimeBus.unsubscribe(this)
    }

    fun checkQuota() = Single.fromCallable {
        checkReady()
        val about = service!!.about().get().setFields("storageQuota").execute()
        if (about == null) Pair(-1L, -1L)
        else {
            val quota = about.storageQuota
            Pair(quota.limit, quota.usage)
        }
    }.subscribeOn(Schedulers.io())

    fun listAppDataFiles(folderName: String) =
        listAppDataFiles(folderName, null)

    fun listAppDataFiles(folderName: String, partialName: String? = null) =
        listAppDataFolders(folderName).flatMap { folders ->
            if (folders.isEmpty()) Single.just(listOf())
            else {
                val folderIds = folders.map { f -> f.id }
                val streams = mutableListOf<Single<List<File>>>()

                folderIds.forEach { id ->
                    var query =
                        "'$id' in parents and mimeType != '${MimeType.MIME_TYPE_FOLDER}'"
                    if (partialName != null) query += "and name contains '$partialName' "
                    streams.add(listAppDataComponents(query))
                }

                Single.zip(streams) { array ->
                    val files = mutableListOf<File>()
                    array.forEach { a ->
                        files.addAll(
                            (a as? List<File>) ?: return@forEach
                        )
                    }
                    files.toList()
                }
            }

        }.subscribeOn(Schedulers.io())

    fun checkExistingFile(folderName: String, partialName: String?) =
        listAppDataFiles(
            folderName,
            partialName
        ).map { files -> files.isNotEmpty() }

    private fun listAppDataFolders(folderName: String) =
        listAppDataComponents("name = '$folderName' and mimeType = '${MimeType.MIME_TYPE_FOLDER}'")

    private fun listAppDataComponents(query: String) = Single.fromCallable {
        checkReady()
        val files = mutableListOf<File>()
        var pageToken: String? = null

        do {
            val fileList =
                service!!.files().list()
                    .setSpaces(Folder.APP_DATA)
                    .setQ(query)
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken).execute()
            files.addAll(fileList.files)

            pageToken = fileList.nextPageToken

        } while (pageToken != null)
        files.toList()
    }.subscribeOn(Schedulers.io())

    fun download(fileId: String) =
        Single.fromCallable {
            checkReady()
            val os = ByteArrayOutputStream()
            service!!.files().get(fileId).executeMediaAndDownloadTo(os)
            os.toByteArray()
        }.subscribeOn(Schedulers.io())

    fun upload(fileName: String, folderName: String, uploadFilePath: String) =
        queryFolder(folderName).map { f -> f.id }.flatMap { folderId ->
            Single.fromCallable {
                checkReady()
                val fileMetadata = File()
                fileMetadata.name = fileName
                fileMetadata.parents = Collections.singletonList(folderId)
                val uploadFile = java.io.File(uploadFilePath)
                val mime =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        uploadFile.extension
                    )
                        ?: "*/*"
                val fileContent = FileContent(mime, uploadFile)
                val file = service!!.files().create(fileMetadata, fileContent)
                    .setFields("id").execute()
                file
            }
        }.subscribeOn(Schedulers.io())

    private fun queryFolder(folderName: String) =
        listAppDataFolders(folderName).flatMap { folders ->
            val folderNames = folders.map { f -> f.name }
            if (folderNames.isEmpty() || !folderNames.contains(folderName)) createAppDataFolder(
                folderName
            )
            else Single.just(folders.first())
        }

    private fun createAppDataFolder(folderName: String) =
        Single.fromCallable {
            checkReady()
            val fileMetadata = File()
            fileMetadata.name = folderName
            fileMetadata.mimeType = MimeType.MIME_TYPE_FOLDER
            fileMetadata.parents = Collections.singletonList(Folder.APP_DATA)
            val file =
                service!!.files().create(fileMetadata).setFields("id").execute()
            file
        }.subscribeOn(Schedulers.io())

    private fun checkReady() {
        if (service != null) return
        throw IllegalStateException(
            "service is not ready"
        )
    }

    private fun buildService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(BuildConfig.APPLICATION_ID).build()
    }

    interface ServiceReadyListener {

        fun onReady()

        fun onPause()
    }
}