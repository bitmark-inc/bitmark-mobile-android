/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.apiservice.utils.record.AssetRecord
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AssetClaimingInfoResponse(
    @Expose
    @SerializedName("asset")
    val asset: AssetRecord,

    @Expose
    @SerializedName("totalEditionLeft")
    val totalEditionLeft: Int,

    @Expose
    @SerializedName("limitedEdition")
    val limitedEdition: Int,

    @Expose
    @SerializedName("issuer")
    val issuer: String?
) : Response