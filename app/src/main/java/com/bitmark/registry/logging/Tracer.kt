package com.bitmark.registry.logging

import android.util.Log
import com.google.gson.GsonBuilder
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder


/**
 * @author Hieu Pham
 * @since 2019-09-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class Tracer(private val level: Level) {

    companion object {

        val DEBUG =
            Tracer(Level.DEBUG)

        val INFO =
            Tracer(Level.INFO)

        val WARNING =
            Tracer(Level.WARNING)

        val ERROR =
            Tracer(Level.ERROR)
    }

    fun log(tag: String, message: String) {
        when (level) {
            Level.DEBUG -> {
                Log.d(tag, message)
            }
            Level.ERROR -> {
                Log.e(tag, message)
            }
            Level.INFO -> {
                Log.i(tag, message)
            }
            Level.WARNING -> {
                Log.w(tag, message)
            }
        }
        Sentry.getContext()
            .recordBreadcrumb(
                BreadcrumbBuilder()
                    .setLevel(level.toBreadcrumbLevel())
                    .setCategory(tag)
                    .setMessage(message)
                    .build()
            )
    }

    fun log(tag: String, extras: Map<String, String>) {
        log(tag, GsonBuilder().create().toJson(extras))
    }


}