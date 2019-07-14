package com.bitmark.registry.data.source.local.api.converter

import androidx.room.TypeConverter
import com.bitmark.registry.data.model.Head


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class HeadConverter {

    @TypeConverter
    fun toString(head: Head?) = head?.value

    @TypeConverter
    fun fromString(head: String?): Head? =
        if (!head.isNullOrEmpty()) Head.valueOf(head) else null
}