package com.bitmark.registry.util.modelview

import android.os.Parcelable
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.util.DateTimeUtil
import com.bitmark.registry.util.DateTimeUtil.Companion.ISO8601_SIMPLE_FORMAT
import com.bitmark.registry.util.DateTimeUtil.Companion.OFFICIAL_DATE_TIME_FORMAT
import kotlinx.android.parcel.Parcelize


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Parcelize
data class TransactionModelView(
    val confirmedAt: String,
    val owner: String,
    val status: TransactionData.Status? = null
) : Parcelable {

    fun isPending() = status == TransactionData.Status.PENDING

    fun confirmedAt() = DateTimeUtil.stringToString(
        confirmedAt,
        ISO8601_SIMPLE_FORMAT,
        OFFICIAL_DATE_TIME_FORMAT
    )
}