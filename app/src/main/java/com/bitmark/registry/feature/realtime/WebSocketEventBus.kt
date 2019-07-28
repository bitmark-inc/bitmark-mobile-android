package com.bitmark.registry.feature.realtime

import com.bitmark.apiservice.BitmarkWebSocket
import com.bitmark.apiservice.BitmarkWebSocketService
import com.bitmark.apiservice.WebSocket
import com.bitmark.apiservice.utils.Address
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.source.AccountRepository
import io.github.centrifugal.centrifuge.DuplicateSubscriptionException
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject


/**
 * @author Hieu Pham
 * @since 2019-07-26
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class WebSocketEventBus(private val accountRepo: AccountRepository) : Bus(),
    WebSocket.ConnectionEvent {

    private var connectListener: ((Throwable?) -> Unit)? = null

    private var disconnectListener: (() -> Unit)? = null

    private val webSocketService = BitmarkWebSocketService(this)

    private val compositeDisposable = CompositeDisposable()

    val newPendingTxPublisher =
        Publisher(PublishSubject.create<Map<String, String>>())

    val bitmarkChangedPublisher =
        Publisher(PublishSubject.create<Map<String, Any>>())

    fun setConnectListener(listener: ((Throwable?) -> Unit)?) {
        this.connectListener = listener
    }

    fun setDisconnectListener(listener: (() -> Unit)?) {
        this.disconnectListener = listener
    }

    fun connect(keyPair: KeyPair) {
        webSocketService.connect(keyPair)
    }

    fun disconnect() {
        // synchronize for completely destroy along with view lifecycle
        getAccountNumber() { accountNumber ->
            unsubscribeNewPendingTx(accountNumber)
            unsubscribeBitmarkChanged(accountNumber)
            webSocketService.disconnect()
        }
    }

    private fun subscribeBitmarkChanged(owner: String) {
        webSocketService.subscribeBitmarkChanged(
            Address.fromAccountNumber(owner),
            object : BitmarkWebSocket.BitmarkChangedEvent() {
                override fun onChanged(
                    bitmarkId: String?,
                    txId: String?,
                    presence: Boolean
                ) {
                    bitmarkChangedPublisher.publisher.onNext(
                        mapOf(
                            "bitmark_id" to bitmarkId!!,
                            "tx_id" to txId!!,
                            "presence" to presence
                        )
                    )
                }

            })
    }

    private fun unsubscribeBitmarkChanged(owner: String) {
        webSocketService.unsubscribeBitmarkChanged(
            Address.fromAccountNumber(
                owner
            )
        )
    }

    private fun subscribeNewPendingTx(stakeholder: String) {
        webSocketService.subscribeNewPendingTx(
            Address.fromAccountNumber(
                stakeholder
            ), object : BitmarkWebSocket.NewPendingTxEvent() {
                override fun onNewPendingIx(
                    txId: String?,
                    owner: String?,
                    prevTxId: String?,
                    prevOwner: String?
                ) {
                    newPendingTxPublisher.publisher.onNext(
                        mapOf(
                            "tx_id" to txId!!,
                            "owner" to owner!!,
                            "prev_tx_id" to prevTxId!!,
                            "prev_owner" to prevOwner!!
                        )
                    )
                }

            })
    }

    private fun unsubscribeNewPendingTx(stakeholder: String) {
        webSocketService.unsubscribeNewPendingTx(
            Address.fromAccountNumber(
                stakeholder
            )
        )
    }

    override fun onConnected() {
        connectListener?.invoke(null)
        getAccountNumber { accountNumber ->
            try {
                subscribeBitmarkChanged(accountNumber)
                subscribeNewPendingTx(accountNumber)
            } catch (ignore: DuplicateSubscriptionException) {
            }
        }
    }

    override fun onConnectionError(e: Throwable?) {
        connectListener?.invoke(e)
    }

    override fun onDisconnected() {
        disconnectListener?.invoke()
    }

    private fun getAccountNumber(
        callback: (String) -> Unit
    ) {
        compositeDisposable.add(accountRepo.getAccountInfo().map { a -> a.first }
            .subscribe { accountNumber, e ->
                if (e == null) {
                    callback.invoke(accountNumber)
                }
            })
    }
}