package com.bitmark.registry.feature.logging

import android.os.Build
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.logging.Level
import com.bitmark.registry.data.source.logging.toEventLevel
import com.bitmark.registry.util.extension.shortenAccountNumber
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.sentry.Sentry
import io.sentry.event.EventBuilder
import io.sentry.event.UserBuilder
import io.sentry.event.interfaces.ExceptionInterface


/**
 * @author Hieu Pham
 * @since 2019-09-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SentryEventLogger(private val accountRepo: AccountRepository) :
    EventLogger {

    private val compositeDisposable = CompositeDisposable()

    fun destroy() {
        compositeDisposable.dispose()
    }

    override fun logEvent(
        event: Event,
        level: Level,
        metadata: Map<String, String>?
    ) {
        val buildEventBuilderStream = buildBaseEventBuilder(
            event,
            io.sentry.event.Event.Level.INFO
        ).map { builder ->
            if (metadata != null) {
                for (entry in metadata.entries) {
                    builder.withExtra(entry.key, entry.value)
                }
            }
            builder
        }

        compositeDisposable.add(
            Single.zip(
                buildUserBuilder(),
                buildEventBuilderStream,
                BiFunction<UserBuilder, EventBuilder, Pair<UserBuilder, EventBuilder>> { user, event ->
                    Pair(
                        user,
                        event
                    )
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe { p, e ->
                    if (e == null) {
                        val context = Sentry.getContext()
                        context.user = p.first.build()
                        Sentry.capture(p.second.withLevel(level.toEventLevel()).build())
                    }
                }
        )
    }

    override fun logError(event: Event, error: Throwable?) {
        val buildEventBuilderStream = buildBaseEventBuilder(
            event,
            io.sentry.event.Event.Level.ERROR
        ).map { builder ->
            builder.withSentryInterface(
                ExceptionInterface(
                    error ?: UnknownError()
                )
            )
        }

        compositeDisposable.addAll(
            Single.zip(
                buildUserBuilder(),
                buildEventBuilderStream,
                BiFunction<UserBuilder, EventBuilder, Pair<UserBuilder, EventBuilder>> { user, event ->
                    Pair(
                        user,
                        event
                    )
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe { p, e ->
                    if (e == null) {
                        val context = Sentry.getContext()
                        context.user = p.first.build()
                        Sentry.capture(p.second.withLevel(io.sentry.event.Event.Level.ERROR).build())
                    }
                })
    }

    private fun buildBaseEventBuilder(
        event: Event,
        level: io.sentry.event.Event.Level
    ) = Single.fromCallable {
        val e = EventBuilder().withMessage(event.value).withLevel(level)
        e.withPlatform("Android")
        e.withEnvironment(BuildConfig.APPLICATION_ID)
        e.withRelease("${BuildConfig.APPLICATION_ID}-${BuildConfig.VERSION_NAME}")
        e.withDist("${BuildConfig.VERSION_CODE}")
        e.withExtra("os", "Android SDK ${Build.VERSION.SDK_INT}")
        e.withExtra("device", "${Build.MANUFACTURER}-${Build.MODEL}")
    }.subscribeOn(Schedulers.computation())

    private fun buildUserBuilder() =
        accountRepo.getAccountNumber().map { accountNumber ->
            UserBuilder().setId(accountNumber)
                .setUsername(accountNumber.shortenAccountNumber())
        }


}