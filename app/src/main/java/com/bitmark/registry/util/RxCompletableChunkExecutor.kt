package com.bitmark.registry.util

import com.bitmark.registry.data.source.logging.Tracer
import com.bitmark.registry.util.extension.poll
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.CompositeException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * @author Hieu Pham
 * @since 2019-08-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RxCompletableChunkExecutor(
    private val chunkSize: Int,
    private val tag: String = TAG
) {

    companion object {
        private const val TAG = "RxChunkExecutor"
    }

    private val taskQueue = ArrayDeque<Completable>()

    private val isProcessing = AtomicBoolean(false)

    private val isPaused = AtomicBoolean(false)

    private val compositeDisposable = CompositeDisposable()

    private var successCallback: (() -> Unit)? = null

    private var errorCallback: ((Throwable) -> Unit)? = null

    fun setSuccessCallback(callback: () -> Unit) {
        this.successCallback = callback
    }

    fun setErrorCallback(callback: (Throwable) -> Unit) {
        this.errorCallback = callback
    }

    fun resume() {
        isPaused.set(false)
        process()
    }

    fun pause() {
        isPaused.set(true)
    }

    fun shutdown() {
        compositeDisposable.dispose()
        isProcessing.set(false)
        isPaused.set(false)
    }

    fun execute(task: Completable, highPriority: Boolean = false) {
        if (isProcessing.get() || isPaused.get()) {
            if (highPriority) {
                taskQueue.addFirst(task)
            } else {
                taskQueue.add(task)
            }
        } else {
            compositeDisposable.add(task.doOnSubscribe { isProcessing.set(true) }
                .doAfterTerminate {
                    isProcessing.set(false)
                    process()
                }
                .doOnDispose { isProcessing.set(false) }.observeOn(
                    AndroidSchedulers.mainThread()
                ).subscribe(
                    { successCallback?.invoke() },
                    { e ->
                        errorCallback?.invoke(e)
                        if (e is CompositeException) {
                            e.exceptions.forEach { ex ->
                                Tracer.ERROR.log(
                                    tag,
                                    "${ex.javaClass}-${ex.message}"
                                )
                            }
                        } else {
                            Tracer.ERROR.log(
                                tag,
                                "${e.javaClass}-${e.message}"
                            )
                        }
                    })
            )
        }
    }

    private fun process() {
        if (taskQueue.isEmpty()) return
        execute(Completable.mergeDelayError(taskQueue.poll(chunkSize)))
    }
}