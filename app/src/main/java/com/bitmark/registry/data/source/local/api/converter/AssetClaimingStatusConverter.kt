package com.bitmark.registry.data.source.local.api.converter

import androidx.room.TypeConverter
import com.bitmark.registry.data.model.entity.AssetClaimingData


/**
 * @author Hieu Pham
 * @since 2019-08-12
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AssetClaimingStatusConverter {

    @TypeConverter
    fun toString(status: AssetClaimingData.Status) = status.value

    @TypeConverter
    fun fromString(status: String) = AssetClaimingData.Status.from(status)
}