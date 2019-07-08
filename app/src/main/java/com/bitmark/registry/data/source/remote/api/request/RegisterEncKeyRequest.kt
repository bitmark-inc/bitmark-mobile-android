package com.bitmark.registry.data.source.remote.api.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class RegisterEncKeyRequest(
    @Expose
    @SerializedName("encryption_pubkey")
    val encPubKey: String,
    @Expose
    val signature: String
) : Request