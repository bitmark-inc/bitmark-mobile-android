package com.bitmark.registry.feature.transfer

import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.encryption.AssetEncryption
import com.bitmark.registry.util.encryption.BoxEncryption
import com.bitmark.registry.util.encryption.SessionData
import com.bitmark.registry.util.extension.set
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
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel() {

    private val transferLiveData = CompositeLiveData<Any>()

    internal val transferProgressLiveData = MutableLiveData<Int>()

    internal fun transferLiveData() = transferLiveData.asLiveData()

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
        encKeyPair: KeyPair
    ): Completable {
        val receiver = params.owner.address!!

        return prepareTransferStream(
            assetId,
            sender,
            receiver,
            encKeyPair
        ).doOnComplete {
            transferProgressLiveData.set(50)
        }.andThen(bitmarkRepo.transferBitmark(
            params,
            bitmarkId,
            assetId
        ).doOnComplete {
            transferProgressLiveData.set(50)
        })

    }

    private fun prepareTransferStream(
        assetId: String,
        accountNumber: String,
        receiver: String,
        encKeyPair: KeyPair
    ) =
        bitmarkRepo.checkExistingRemoteAssetFile(
            assetId,
            accountNumber
        ).flatMapCompletable { res ->
            val sessionData = res.sessionData
            if (sessionData != null) {
                // asset file is existing
                accountRepo.getEncPubKey(receiver)
                    .flatMapCompletable { receiverPubKey ->
                        val algorithm = AssetEncryption().getAlgorithm()
                        val senderPubKey = encKeyPair.publicKey().toBytes()
                        val senderPrivKey = encKeyPair.privateKey().toBytes()
                        val keyMaster = BoxEncryption(senderPrivKey)
                        val secretKey =
                            sessionData.getRawKey(keyMaster, receiverPubKey)
                        val newSessionData = SessionData.from(
                            secretKey,
                            algorithm,
                            senderPubKey,
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
                    }
            } else uploadStream(assetId, accountNumber, receiver, encKeyPair)
        }

    private fun uploadStream(
        assetId: String,
        accountNumber: String,
        receiver: String,
        encKeyPair: KeyPair
    ) = bitmarkRepo.checkAssetFile(accountNumber, assetId).flatMap { p ->
        val file = p.second
        if (file == null) Single.just(Pair(null, null))
        else accountRepo.getEncPubKey(receiver).map { k -> Pair(file, k) }
    }.flatMapCompletable { p ->
        val file = p.first
        val receiverPubKey = p.second

        if (file == null || receiverPubKey == null) Completable.complete()
        else {
            val keyEncryptor = BoxEncryption(encKeyPair.privateKey().toBytes())
            val assetEncryptor = AssetEncryption()

            val encryptData =
                assetEncryptor.encrypt(
                    file.readBytes(),
                    receiverPubKey,
                    keyEncryptor
                )

            val sessionData = encryptData.first
            val encryptedFileBytes = encryptData.second
            val access = "%s:%s".format(receiver, sessionData.encryptedKey)

            bitmarkRepo.uploadAssetFile(
                assetId,
                accountNumber,
                sessionData,
                access,
                file.name,
                encryptedFileBytes
            )
        }

    }

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}