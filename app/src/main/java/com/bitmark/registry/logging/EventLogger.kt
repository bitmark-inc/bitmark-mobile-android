package com.bitmark.registry.logging


/**
 * @author Hieu Pham
 * @since 2019-09-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface EventLogger {

    fun logEvent(
        event: Event,
        level: Level = Level.DEBUG,
        metadata: Map<String, String>? = null
    )

    fun logError(event: Event, error: Throwable?)
}