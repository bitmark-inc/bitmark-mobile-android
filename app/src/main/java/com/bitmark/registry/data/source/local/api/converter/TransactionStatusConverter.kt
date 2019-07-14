package com.bitmark.registry.data.source.local.api.converter

import androidx.room.TypeConverter
import com.bitmark.registry.data.model.TransactionData


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransactionStatusConverter {

    @TypeConverter
    fun toString(status: TransactionData.Status) = status.value

    @TypeConverter
    fun fromString(status: String) = TransactionData.Status.from(status)
}