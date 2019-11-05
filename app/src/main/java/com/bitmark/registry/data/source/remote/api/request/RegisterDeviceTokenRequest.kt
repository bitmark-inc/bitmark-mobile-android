/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

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