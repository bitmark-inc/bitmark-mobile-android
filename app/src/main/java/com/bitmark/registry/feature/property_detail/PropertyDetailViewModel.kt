package com.bitmark.registry.feature.property_detail

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.TransactionData
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
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.modelview.TransactionModelView
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
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
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel(lifecycle) {

    private lateinit var bitmarkId: String

    private val getProvenanceLiveData =
        CompositeLiveData<List<TransactionModelView>>()

    private val syncProvenanceLiveData =
        CompositeLiveData<List<TransactionModelView>>()

    private val deleteBitmarkLiveData = CompositeLiveData<Any>()

    private val downloadAssetFileLiveData =
        CompositeLiveData<File>()

    private val getExistingAssetFileLiveData = CompositeLiveData<File?>()

    private val getKeyAliasLiveData = CompositeLiveData<String>()

    internal val downloadProgressLiveData = MutableLiveData<Int>()

    internal val bitmarkSavedLiveData =
        BufferedLiveData<BitmarkModelView>(lifecycle)

    internal val txsSavedLiveData =
        BufferedLiveData<List<TransactionModelView>>(lifecycle)

    internal fun setBitmarkId(bitmarkId: String) {
        this.bitmarkId = bitmarkId
    }

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

    private fun getProvenanceStream(bitmarkId: String): Single<List<TransactionModelView>> {
        val accountStream =
            accountRepo.getAccountInfo().map { a -> a.first }
        val txsStream = bitmarkRepo.listTxs(
            bitmarkId = bitmarkId,
            loadBlock = true,
            isPending = true
        )

        return Single.zip(
            accountStream,
            txsStream,
            BiFunction { accountNumber, txs ->
                txs.map { t ->
                    TransactionModelView.newInstance(
                        t,
                        accountNumber
                    )
                }
            })
    }

    private fun syncProvenanceStream(bitmarkId: String): Single<List<TransactionModelView>> {
        val accountStream =
            accountRepo.getAccountInfo().map { a -> a.first }
        val txsStream = bitmarkRepo.syncTxs(
            bitmarkId = bitmarkId,
            loadBlock = true,
            isPending = true,
            loadAsset = true
        )

        return Single.zip(
            accountStream,
            txsStream,
            BiFunction { accountNumber, txs ->
                txs.map { t ->
                    TransactionModelView.newInstance(
                        t,
                        accountNumber
                    )
                }
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

    override fun onCreate() {
        super.onCreate()

        realtimeBus.bitmarkSavedPublisher.subscribe(this) { bitmarks ->
            if (bitmarks.isEmpty()) return@subscribe
            val hasChanged =
                bitmarks.indexOfFirst { b -> b.id == bitmarkId } != -1
            if (!hasChanged) return@subscribe

            val getAccountNumberStream =
                accountRepo.getAccountInfo().map { a -> a.first }.toMaybe()

            val getBitmarkStream =
                bitmarkRepo.getStoredBitmarkById(bitmarks[0].id)

            subscribe(
                Maybe.zip(
                    getAccountNumberStream,
                    getBitmarkStream,
                    BiFunction<String, BitmarkData, BitmarkModelView> { accountNumber, bitmark ->
                        BitmarkModelView.newInstance(bitmark, accountNumber)
                    }).observeOn(
                    AndroidSchedulers.mainThread()
                ).subscribe({ bitmark ->
                    bitmarkSavedLiveData.setValue(bitmark)
                }, {})
            )
        }

        realtimeBus.txsSavedPublisher.subscribe(this) { txs ->
            if (txs.isEmpty()) return@subscribe
            val hasChanged =
                txs.indexOfFirst { t -> t.bitmarkId == bitmarkId } != -1
            if (!hasChanged) return@subscribe

            val getAccountNumberStream =
                accountRepo.getAccountInfo().map { a -> a.first }

            val syncTxsStream = bitmarkRepo.listTxs(
                bitmarkId = bitmarkId,
                isPending = true,
                loadBlock = true
            )

            subscribe(
                Single.zip(
                    getAccountNumberStream,
                    syncTxsStream,
                    BiFunction<String, List<TransactionData>, List<TransactionModelView>> { accountNumber, ts ->
                        ts.map { t ->
                            TransactionModelView.newInstance(
                                t,
                                accountNumber
                            )
                        }
                    }).observeOn(
                    AndroidSchedulers.mainThread()
                ).subscribe { ts, e ->
                    if (e == null) {
                        txsSavedLiveData.setValue(ts)
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

}