package com.bitmark.registry.data.source.local.api.converter

import androidx.room.TypeConverter
import com.bitmark.registry.data.model.AssetData


/**
 * @author Hieu Pham
 * @since 2019-09-16
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AssetTypeConverter {
    @TypeConverter
    fun toString(type: AssetData.Type) = type.value

    @TypeConverter
    fun fromString(type: String) = AssetData.Type.from(type)
}