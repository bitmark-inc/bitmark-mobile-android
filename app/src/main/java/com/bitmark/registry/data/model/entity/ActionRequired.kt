/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.model.entity

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ActionRequired(
    @Expose
    val id: Id,

    @Expose
    val type: Type,

    @Expose
    @SerializedName("title_string_res_name")
    val titleStringResName: String = "",

    @Expose
    @SerializedName("description_string_res_name")
    val desStringResName: String = "",

    @Expose
    val date: String
) {

    enum class Type {
        @Expose
        @SerializedName("security_alert")
        SECURITY_ALERT
    }

    enum class Id {
        @Expose
        @SerializedName("recovery_phrase")
        RECOVERY_PHRASE,

        @Expose
        @SerializedName("cloud_service_authorization")
        CLOUD_SERVICE_AUTHORIZATION
    }
}