package com.bitmark.registry.logging

import io.sentry.event.Breadcrumb
import io.sentry.event.Event


/**
 * @author Hieu Pham
 * @since 2019-09-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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