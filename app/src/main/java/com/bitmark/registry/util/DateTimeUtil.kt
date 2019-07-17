package com.bitmark.registry.util

import java.text.SimpleDateFormat
import java.util.*


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class DateTimeUtil {

    companion object {

        val ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"

        val ISO8601_SIMPLE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

        val OFFICIAL_DATE_TIME_FORMAT = "yyyy MMM dd HH:mm:ss"

        fun stringToString(date: String) =
            stringToString(date, OFFICIAL_DATE_TIME_FORMAT)

        fun stringToString(date: String, newFormat: String) =
            stringToString(date, ISO8601_FORMAT, newFormat)

        fun stringToString(
            date: String,
            oldFormat: String,
            newFormat: String,
            timezone : String = "UTC"
        ): String {
            return try {
                var formatter = SimpleDateFormat(oldFormat, Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone(timezone)
                val d = formatter.parse(date)
                formatter = SimpleDateFormat(newFormat, Locale.getDefault())
                formatter.format(d)
            } catch (e: Throwable) {
                ""
            }

        }
    }
}