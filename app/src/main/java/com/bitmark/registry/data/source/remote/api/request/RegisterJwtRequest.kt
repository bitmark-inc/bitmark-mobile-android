package com.bitmark.registry.data.source.remote.api.request

import com.google.gson.annotations.Expose


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class RegisterJwtRequest(
    @Expose
    val timestamp: String,
    @Expose
    val signature: String,
    @Expose
    val requester: String
) :
    Request