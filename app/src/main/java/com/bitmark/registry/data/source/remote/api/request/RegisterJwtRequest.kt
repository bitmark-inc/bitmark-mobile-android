/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

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