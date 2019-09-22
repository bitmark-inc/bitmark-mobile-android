package com.bitmark.registry.data.source.remote.api.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class RegisterJwtRequest(
    @Expose
    @SerializedName("timestamp")
    val timestamp: String,

    @Expose
    @SerializedName("signature")
    val signature: String,

    @Expose
    @SerializedName("requester")
    val requester: String
) :
    Request