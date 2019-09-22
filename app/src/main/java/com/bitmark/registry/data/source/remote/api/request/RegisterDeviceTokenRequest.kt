package com.bitmark.registry.data.source.remote.api.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-07-26
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class RegisterDeviceTokenRequest(
    @Expose
    @SerializedName("platform")
    val platform: String,

    @Expose
    @SerializedName("token")
    val token: String,

    @Expose
    @SerializedName("client")
    val client: String,

    @Expose
    @SerializedName("intercom_user_id")
    val intercomId: String?
) : Request