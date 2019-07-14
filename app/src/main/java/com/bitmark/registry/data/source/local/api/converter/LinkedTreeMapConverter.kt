package com.bitmark.registry.data.source.local.api.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class LinkedTreeMapConverter {

    @TypeConverter
    fun toString(map: Map<String, String>?): String? {
        return if (map == null || map.isEmpty()) null else Gson().toJsonTree(
            map
        ).asJsonObject.toString()
    }

    @TypeConverter
    fun fromString(str: String?): Map<String, String>? {
        return if (str.isNullOrEmpty()) null else Gson().fromJson<Map<String, String>>(
            str, object : TypeToken<Map<String, String>>() {}.type
        )
    }
}