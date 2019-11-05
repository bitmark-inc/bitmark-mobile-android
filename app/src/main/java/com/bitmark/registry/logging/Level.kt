/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.logging

import io.sentry.event.Breadcrumb
import io.sentry.event.Event

enum class Level {

    DEBUG,

    INFO,

    WARNING,

    ERROR
}

fun Level.toBreadcrumbLevel() = when (this) {
    Level.DEBUG -> Breadcrumb.Level.DEBUG
    Level.WARNING -> Breadcrumb.Level.WARNING
    Level.INFO -> Breadcrumb.Level.INFO
    Level.ERROR -> Breadcrumb.Level.ERROR
}

fun Level.toEventLevel() = when (this) {
    Level.DEBUG -> Event.Level.DEBUG
    Level.WARNING -> Event.Level.WARNING
    Level.INFO -> Event.Level.INFO
    Level.ERROR -> Event.Level.ERROR
}