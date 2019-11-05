/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.registry.data.model.entity.AssetClaimingData
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AssetClaimingRequestsResponse(
    @Expose
    @SerializedName("claim_requests")
    val incomingRequests: List<AssetClaimingData>,

    @Expose
    @SerializedName("my_submitted_claim_requests")
    val outgoingRequests: List<AssetClaimingData>
) : Response