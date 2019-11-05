/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.modelview

import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.util.DateTimeUtil

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