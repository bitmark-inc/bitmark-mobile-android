package com.bitmark.registry.feature.realtime

import com.bitmark.apiservice.BitmarkWebSocket
import com.bitmark.apiservice.BitmarkWebSocketService
import com.bitmark.apiservice.WebSocket
import com.bitmark.apiservice.utils.Address
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.AppLifecycleHandler
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.logging.Tracer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject


/**
 * @author Hieu Pham
 * @since 2019-07-26
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class WebSocketEventBus(
    private val accountRepo: AccountRepository,
    appLifecycleHandler: AppLifecycleHandler
) : Bus(), AppLifecycleHandler.AppStateChangedListener,
    WebSocket.ConnectionEvent {

    companion object {
        private const val TAG = "WebSocketEventBus"
    }

    private var connectListener: ((Throwable?) -> Unit)? = null

    private var disconnectListener: (() -> Unit)? = null

    private val webSocketService = BitmarkWebSocketService(this)

    private val compositeDisposable = CompositeDisposable()

    val newPendingTxPublisher =
        Publisher(PublishSubject.create<Map<String, String>>())

    val bitmarkChangedPublisher =
        Publisher(PublishSubject.create<Map<String, Any>>())

    val newPendingIssuancePublisher = Publisher(PublishSubject.create<String>())

    init {
        appLifecycleHandler.addAppStateChangedListener(this)
    }

    fun setConnectListener(listener: ((Throwable?) -> Unit)?) {
        this.connectListener = listener
    }

    fun setDisconnectListener(listener: (() -> Unit)?) {
        this.disconnectListener = listener
    }

    fun connect(keyPair: KeyPair) {
        webSocketService.connect(keyPair)
    }

    fun disconnect(onDone: (() -> Unit)? = null) {
        unsubscribeEvents {
            webSocketService.disconnect()
            onDone?.invoke()
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

    private fun subscribeNewPendingIssuance(issuer: String) {
        webSocketService.subscribeNewPendingIssuance(
            Address.fromAccountNumber(
                issuer
            ), object : BitmarkWebSocket.NewPendingIssuanceEvent() {
                override fun onNewPendingIssuance(bitmarkId: String?) {
                    newPendingIssuancePublisher.publisher.onNext(
                        bitmarkId ?: return
                    )
                }
            })
    }

    private fun unsubscribeNewPendingIssuance(issuer: String) {
        webSocketService.unsubscribeNewPendingIssuance(
            Address.fromAccountNumber(
                issuer
            )
        )
    }

    override fun onConnected() {
        Tracer.DEBUG.log(TAG, "onConnected")
        connectListener?.invoke(null)
        subscribeEvents()
    }

    private fun subscribeEvents() {
        getAccountNumber { accountNumber ->
            try {
                subscribeBitmarkChanged(accountNumber)
                subscribeNewPendingTx(accountNumber)
                subscribeNewPendingIssuance(accountNumber)
                Tracer.DEBUG.log(TAG, "subscribe events")
            } catch (e: Throwable) {
                Tracer.ERROR.log(TAG, "subscribe events error: $e message ${e.message}")
            }
        }
    }

    private fun unsubscribeEvents(onDone: (() -> Unit)? = null) {
        getAccountNumber { accountNumber ->
            try {
                unsubscribeNewPendingIssuance(accountNumber)
                unsubscribeNewPendingTx(accountNumber)
                unsubscribeBitmarkChanged(accountNumber)
                Tracer.DEBUG.log(TAG, "unsubscribe events")
                onDone?.invoke()
            } catch (e: Throwable) {
                Tracer.ERROR.log(TAG, "unsubscribe events error: $e message ${e.message}")
            }
        }
    }

    override fun onConnectionError(e: Throwable?) {
        Tracer.DEBUG.log(TAG, "onConnectionError: $e message ${e?.message}")
        connectListener?.invoke(e)
    }

    override fun onDisconnected() {
        Tracer.DEBUG.log(TAG, "onDisconnected")
        disconnectListener?.invoke()
    }

    private fun getAccountNumber(
        callback: (String) -> Unit
    ) {
        compositeDisposable.add(accountRepo.getAccountNumber()
            .subscribe { accountNumber, e ->
                if (e == null) {
                    callback.invoke(accountNumber)
                }
            })
    }

    override fun onForeground() {
        subscribeEvents()
    }

    override fun onBackground() {
        unsubscribeEvents()
    }
}