package com.bitmark.registry.data.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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