/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.logging

import android.util.Log
import com.google.gson.GsonBuilder
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder

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