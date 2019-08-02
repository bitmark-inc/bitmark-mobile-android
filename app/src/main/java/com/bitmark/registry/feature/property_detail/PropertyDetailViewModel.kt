package com.bitmark.registry.feature.property_detail

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.remote.api.response.DownloadAssetFileResponse
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.encryption.AssetEncryption
import com.bitmark.registry.util.encryption.BoxEncryption
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.TransactionModelView
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertyDetailViewModel(
    lifecycle: Lifecycle,
    private val bitmarkRepo: BitmarkRepository,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel(lifecycle) {

    private val getProvenanceLiveData =
        CompositeLiveData<Pair<String, List<TransactionModelView>>>()

    private val syncProvenanceLiveData =
        CompositeLiveData<Pair<String, List<TransactionModelView>>>()

    private val deleteBitmarkLiveData = CompositeLiveData<Any>()

    private val downloadAssetFileLiveData =
        CompositeLiveData<File>()

    private val getExistingAssetFileLiveData = CompositeLiveData<File?>()

    private val getKeyAliasLiveData = CompositeLiveData<String>()

    internal val downloadProgressLiveData = MutableLiveData<Int>()

    internal fun getProvenanceLiveData() = getProvenanceLiveData.asLiveData()

    internal fun syncProvenanceLiveData() = syncProvenanceLiveData.asLiveData()

    internal fun deleteBitmarkLiveData() = deleteBitmarkLiveData.asLiveData()

    internal fun downloadAssetFileLiveData() =
        downloadAssetFileLiveData.asLiveData()

    internal fun getExistingAssetFileLiveData() =
        getExistingAssetFileLiveData.asLiveData()

    internal fun getKeyAliasLiveData() = getKeyAliasLiveData.asLiveData()

    internal fun getProvenance(bitmarkId: String) {
        getProvenanceLiveData.add(
            rxLiveDataTransformer.single(
                getProvenanceStream(bitmarkId)
            )
        )
    }

    internal fun syncProvenance(bitmarkId: String) {
        syncProvenanceLiveData.add(
            rxLiveDataTransformer.single(
                syncProvenanceStream(bitmarkId)
            )
        )
    }

    private fun getProvenanceStream(bitmarkId: String): Single<Pair<String, List<TransactionModelView>>> {
        val accountStream =
            accountRepo.getAccountInfo().map { a -> a.first }
        val txsStream = bitmarkRepo.listTxs(
            bitmarkId = bitmarkId,
            loadBlock = true,
            isPending = true
        ).map { txs ->
            txs.map { tx ->
                TransactionModelView(
                    id = tx.id,
                    confirmedAt = tx.block?.createdAt ?: "",
                    owner = tx.owner,
                    previousOwner = tx.previousOwner,
                    status = tx.status,
                    offset = tx.offset
                )
            }
        }

        return Single.zip(
            accountStream,
            txsStream,
            BiFunction { accountNumber, txs ->
                Pair(
                    accountNumber,
                    txs
                )
            })
    }

    private fun syncProvenanceStream(bitmarkId: String): Single<Pair<String, List<TransactionModelView>>> {
        val accountStream =
            accountRepo.getAccountInfo().map { a -> a.first }
        val txsStream = bitmarkRepo.syncTxs(
            bitmarkId = bitmarkId,
            loadBlock = true,
            isPending = true,
            loadAsset = true
        ).map { txs ->
            txs.map { tx ->
                TransactionModelView(
                    id = tx.id,
                    confirmedAt = tx.block?.createdAt ?: "",
                    previousOwner = tx.previousOwner,
                    owner = tx.owner,
                    status = tx.status,
                    offset = tx.offset
                )
            }
        }

        return Single.zip(
            accountStream,
            txsStream,
            BiFunction { accountNumber, txs ->
                Pair(
                    accountNumber,
                    txs
                )
            })
    }

    internal fun deleteBitmark(
        params: TransferParams,
        bitmarkId: String,
        assetId: String
    ) {
        deleteBitmarkLiveData.add(
            rxLiveDataTransformer.completable(
                deleteBitmarkStream(params, bitmarkId, assetId)
            )
        )
    }

    private fun deleteBitmarkStream(
        params: TransferParams,
        bitmarkId: String,
        assetId: String
    ) =
        bitmarkRepo.deleteBitmark(params, bitmarkId, assetId)

    internal fun downloadAssetFile(
        assetId: String,
        sender: String,
        receiver: String,
        encryptionKeyPair: KeyPair
    ) {
        downloadAssetFileLiveData.add(
            rxLiveDataTransformer.single(
                downloadAssetFileStream(
                    assetId,
                    sender,
                    receiver,
                    encryptionKeyPair
                )
            )
        )
    }

    private fun downloadAssetFileStream(
        assetId: String,
        sender: String,
        receiver: String,
        encryptionKeyPair: KeyPair
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
                BoxEncryption(encryptionKeyPair.privateKey().toBytes())
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

    internal fun getExistingAsset(accountNumber: String, assetId: String) =
        getExistingAssetFileLiveData.add(
            rxLiveDataTransformer.single(
                bitmarkRepo.checkAssetFile(
                    accountNumber,
                    assetId
                ).map { p -> p.second }
            )
        )

    internal fun getKeyAlias() =
        getKeyAliasLiveData.add(rxLiveDataTransformer.single(accountRepo.getKeyAlias()))


    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

}