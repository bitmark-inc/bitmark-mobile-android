package com.bitmark.registry.feature.realtime

import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.local.BitmarkDeletedListener
import com.bitmark.registry.data.source.local.BitmarkInsertedListener
import com.bitmark.registry.data.source.local.BitmarkStatusChangedListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlin.reflect.KClass


/**
 * @author Hieu Pham
 * @since 2019-07-14
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RealtimeBus(bitmarkRepo: BitmarkRepository) :
    BitmarkInsertedListener, BitmarkDeletedListener,
    BitmarkStatusChangedListener {

    private val observerMap = mutableMapOf<KClass<*>, MutableList<Disposable>>()

    val bitmarkDeletedPublisher =
        Publisher(PublishSubject.create<List<String>>())

    val bitmarkStatusChangedPublisher =
        Publisher(PublishSubject.create<Triple<String, BitmarkData.Status, BitmarkData.Status>>())

    val bitmarkInsertedPublisher =
        Publisher(PublishSubject.create<List<String>>())

    init {
        bitmarkRepo.setBitmarkDeletedListener(this)
        bitmarkRepo.setBitmarkInsertedListener(this)
        bitmarkRepo.setBitmarkStatusChangedListener(this)
    }

    fun <H : Any> unsubscribe(host: H) {
        val observers = observerMap[host::class]
        if (observers.isNullOrEmpty()) return
        observers.forEach { o -> o.dispose() }
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

    override fun onInserted(bitmarkIds: List<String>) {
        bitmarkInsertedPublisher.publisher.onNext(bitmarkIds)
    }

    inner class Publisher<T>(internal val publisher: PublishSubject<T>) {

        fun <H : Any> subscribe(host: H, consumer: (T) -> Unit) {
            val disposable = publisher.observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer)
            val kclass = host::class
            if (observerMap[kclass] == null) {
                observerMap[kclass] = mutableListOf()
            }
            observerMap[kclass]?.add(disposable)
        }
    }
}