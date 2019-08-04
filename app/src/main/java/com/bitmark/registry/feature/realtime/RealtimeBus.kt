package com.bitmark.registry.feature.realtime

import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.local.*
import io.reactivex.subjects.PublishSubject


/**
 * @author Hieu Pham
 * @since 2019-07-14
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RealtimeBus(
    bitmarkRepo: BitmarkRepository,
    accountRepo: AccountRepository
) : Bus(),
    BitmarkSavedListener, BitmarkDeletedListener,
    BitmarkStatusChangedListener, AssetFileSavedListener,
    ActionRequiredDeletedListener, TxsSavedListener, BitmarkSeenListener {


    val bitmarkDeletedPublisher =
        Publisher(PublishSubject.create<List<String>>())

    val bitmarkStatusChangedPublisher =
        Publisher(PublishSubject.create<Triple<String, BitmarkData.Status, BitmarkData.Status>>())

    val bitmarkSavedPublisher =
        Publisher(PublishSubject.create<List<BitmarkData>>())

    val assetFileSavedPublisher = Publisher(PublishSubject.create<String>())

    val actionRequiredDeletedPublisher =
        Publisher(PublishSubject.create<ActionRequired.Id>())

    val txsSavedPublisher =
        Publisher(PublishSubject.create<List<TransactionData>>())

    val bitmarkSeenPublisher = Publisher(PublishSubject.create<String>())

    init {
        bitmarkRepo.setBitmarkDeletedListener(this)
        bitmarkRepo.setBitmarkSavedListener(this)
        bitmarkRepo.setBitmarkStatusChangedListener(this)
        bitmarkRepo.setAssetFileSavedListener(this)
        bitmarkRepo.setTxsSavedListener(this)
        bitmarkRepo.setBitmarkSeenListener(this)
        accountRepo.setActionRequiredDeletedListener(this)
    }

    override fun onChanged(
        bitmarkId: String,
        oldStatus: BitmarkData.Status,
        newStatus: BitmarkData.Status
    ) {
        bitmarkStatusChangedPublisher.publisher.onNext(
            Triple(
                bitmarkId,
                oldStatus,
                newStatus
            )
        )
    }

    override fun onDeleted(bitmarkIds: List<String>) {
        bitmarkDeletedPublisher.publisher.onNext(bitmarkIds)
    }

    override fun onBitmarksSaved(bitmarks: List<BitmarkData>) {
        bitmarkSavedPublisher.publisher.onNext(bitmarks)
    }

    override fun onSaved(assetId: String) {
        assetFileSavedPublisher.publisher.onNext(assetId)
    }

    override fun onDeleted(actionId: ActionRequired.Id) {
        actionRequiredDeletedPublisher.publisher.onNext(actionId)
    }

    override fun onTxsSaved(txs: List<TransactionData>) {
        txsSavedPublisher.publisher.onNext(txs)
    }

    override fun onSeen(bitmarkId: String) {
        bitmarkSeenPublisher.publisher.onNext(bitmarkId)
    }
}