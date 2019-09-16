package com.bitmark.registry.feature.realtime

import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.local.event.*
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
    BitmarkSavedListener,
    BitmarkDeletedListener,
    BitmarkStatusChangedListener,
    AssetFileSavedListener,
    ActionRequiredDeletedListener,
    TxsSavedListener,
    BitmarkSeenListener,
    AssetSavedListener,
    ActionRequiredAddedListener {

    val bitmarkDeletedPublisher =
        Publisher(PublishSubject.create<Pair<String, BitmarkData.Status>>())

    val bitmarkStatusChangedPublisher =
        Publisher(PublishSubject.create<Triple<String, BitmarkData.Status, BitmarkData.Status>>())

    val bitmarkSavedPublisher =
        Publisher(PublishSubject.create<BitmarkData>())

    val assetFileSavedPublisher = Publisher(PublishSubject.create<String>())

    val actionRequiredDeletedPublisher =
        Publisher(PublishSubject.create<ActionRequired.Id>())

    val txsSavedPublisher =
        Publisher(PublishSubject.create<List<TransactionData>>())

    val bitmarkSeenPublisher = Publisher(PublishSubject.create<String>())

    val assetsSavedPublisher =
        Publisher(PublishSubject.create<Pair<AssetData, Boolean>>())

    val actionRequiredAddedPublisher =
        Publisher(PublishSubject.create<List<ActionRequired.Id>>())

    init {
        bitmarkRepo.setBitmarkDeletedListener(this)
        bitmarkRepo.setBitmarkSavedListener(this)
        bitmarkRepo.setBitmarkStatusChangedListener(this)
        bitmarkRepo.setAssetFileSavedListener(this)
        bitmarkRepo.setTxsSavedListener(this)
        bitmarkRepo.setBitmarkSeenListener(this)
        bitmarkRepo.setAssetSavedListener(this)
        accountRepo.setActionRequiredDeletedListener(this)
        accountRepo.setActionRequiredAddedListener(this)
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

    override fun onDeleted(bitmarkId: String, lastStatus: BitmarkData.Status) {
        bitmarkDeletedPublisher.publisher.onNext(Pair(bitmarkId, lastStatus))
    }

    override fun onBitmarkSaved(bitmark: BitmarkData) {
        bitmarkSavedPublisher.publisher.onNext(bitmark)
    }

    override fun onSaved(assetId: String) {
        assetFileSavedPublisher.publisher.onNext(assetId)
    }

    override fun onDeleted(actionId: ActionRequired.Id) {
        actionRequiredDeletedPublisher.publisher.onNext(actionId)
    }

    override fun onAdded(actionIds: List<ActionRequired.Id>) {
        actionRequiredAddedPublisher.publisher.onNext(actionIds)
    }

    override fun onTxsSaved(txs: List<TransactionData>) {
        txsSavedPublisher.publisher.onNext(txs)
    }

    override fun onSeen(bitmarkId: String) {
        bitmarkSeenPublisher.publisher.onNext(bitmarkId)
    }

    override fun onAssetSaved(asset: AssetData, isNewRecord: Boolean) {
        assetsSavedPublisher.publisher.onNext(Pair(asset, isNewRecord))
    }
}