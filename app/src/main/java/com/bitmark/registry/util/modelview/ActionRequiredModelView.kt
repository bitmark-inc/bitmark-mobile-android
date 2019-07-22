package com.bitmark.registry.util.modelview

import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.util.DateTimeUtil


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class ActionRequiredModelView(
    val id: ActionRequired.Id,
    val type: ActionRequired.Type,
    val titleStringResName: String,
    val descriptionStringResName: String,
    val date: String
) {
    fun getShortenDate() =
        DateTimeUtil.stringToString(date, DateTimeUtil.OFFICIAL_DATE_FORMAT)

}