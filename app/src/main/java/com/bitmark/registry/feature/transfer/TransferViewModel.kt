package com.bitmark.registry.feature.transfer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.encryption.AssetEncryption
import com.bitmark.registry.util.encryption.BoxEncryption
import com.bitmark.registry.util.encryption.SessionData
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransferViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel(lifecycle) {

    private val transferLiveData = CompositeLiveData<Any>()

    internal val transferProgressLiveData = MutableLiveData<Int>()

    private val getKeyAliasLiveData = CompositeLiveData<String>()

    internal val bitmarkDeletedLiveData =
        BufferedLiveData<Pair<String, BitmarkData.Status>>(lifecycle)

    internal fun transferLiveData() = transferLiveData.asLiveData()

    internal fun getKeyAliasLiveData() = getKeyAliasLiveData.asLiveData()

    internal fun transfer(
        params: TransferParams,
        assetId: String,
        bitmarkId: String,
        accountNumber: String,
        encKeyPair: KeyPair
    ) = transferLiveData.add(
        rxLiveDataTransformer.completable(
            transferStream(
                params,
                assetId,
                bitmarkId,
                accountNumber,
                encKeyPair
            )
        )
    )

    private fun transferStream(
        params: TransferParams,
        assetId: String,
        bitmarkId: String,
        sender: String,
        senderEncKeyPair: KeyPair
    ): Completable {
        val receiver = params.owner.address!!

        return prepareTransferStream(
            assetId,
            sender,
            receiver,
            senderEncKeyPair
        ) { p ->
            transferProgressLiveData.set(p * 50 / 100)
        }.andThen(
            bitmarkRepo.transferBitmark(
                params,
                sender,
                bitmarkId,
                assetId
            ).doOnComplete {
                transferProgressLiveData.set(100)
            })

    }

    private fun prepareTransferStream(
        assetId: String,
        accountNumber: String,
        receiver: String,
        senderEncKeyPair: KeyPair,
        progress: (Int) -> Unit
    ): Completable {

        return bitmarkRepo.checkExistingRemoteAssetFile(
            assetId,
            accountNumber
        ).doOnSuccess {
            progress(50)
        }.flatMapCompletable { res ->
            val sessionData = res.sessionData
            if (sessionData != null) {
                // asset file is existing
                accountRepo.getEncPubKey(receiver)
                    .flatMapCompletable { receiverPubKey ->
                        val algorithm = AssetEncryption().getAlgorithm()
                        val senderPubKey =
                            senderEncKeyPair.publicKey().toBytes()
                        val senderPrivKey =
                            senderEncKeyPair.privateKey().toBytes()
                        val keyMaster = BoxEncryption(senderPrivKey)
                        val secretKey =
                            sessionData.getRawKey(keyMaster, senderPubKey)
                        val newSessionData = SessionData.from(
                            secretKey,
                            algorithm,
                            receiverPubKey,
                            keyMaster
                        )

                        val access = "%s:%s".format(
                            receiver,
                            newSessionData.encryptedKey
                        )
                        bitmarkRepo.grantAccessAssetFile(
                            assetId,
                            accountNumber,
                            access
                        )
                    }.doOnComplete {
                        progress(100)
                    }
            } else uploadStream(
                assetId,
                accountNumber,
                receiver,
                senderEncKeyPair
            ) { p ->
                progress(50 + (p * 50 / 100))
            }
        }
    }

    private fun uploadStream(
        assetId: String,
        accountNumber: String,
        receiver: String,
        senderEncKeyPair: KeyPair,
        progressCallback: (Int) -> Unit
    ) = bitmarkRepo.checkAssetFile(accountNumber, assetId).flatMap { p ->
        val file = p.second
        if (file == null) Single.just(Pair(null, null))
        else accountRepo.getEncPubKey(receiver).map { k -> Pair(file, k) }
    }.flatMapCompletable { p ->
        val file = p.first
        val receiverPubKey = p.second

        if (file == null || receiverPubKey == null) Completable.complete()
        else {
            val senderPubKey =
                senderEncKeyPair.publicKey().toBytes()
            val senderPrivKey =
                senderEncKeyPair.privateKey().toBytes()
            val keyEncryptor = BoxEncryption(senderPrivKey)
            val assetEncryptor = AssetEncryption()

            val receiverSessionData =
                assetEncryptor.getSessionData(receiverPubKey, keyEncryptor)
            val senderSessionData =
                assetEncryptor.getSessionData(senderPubKey, keyEncryptor)
            val encryptedFileBytes = assetEncryptor.encrypt(
                file.readBytes(),
                receiverPubKey
            )
            val access =
                "%s:%s".format(receiver, receiverSessionData.encryptedKey)

            bitmarkRepo.uploadAssetFile(
                assetId,
                accountNumber,
                senderSessionData,
                access,
                file.name,
                encryptedFileBytes,
                progressCallback
            )
        }

    }

    internal fun getKeyAlias() =
        getKeyAliasLiveData.add(rxLiveDataTransformer.single(accountRepo.getKeyAlias()))

    override fun onCreate() {
        super.onCreate()

        realtimeBus.bitmarkDeletedPublisher.subscribe(this) { p ->
            bitmarkDeletedLiveData.set(p)
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}