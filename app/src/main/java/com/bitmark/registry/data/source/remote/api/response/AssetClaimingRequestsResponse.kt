package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.registry.data.model.AssetClaimingData
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-08-12
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class AssetClaimingRequestsResponse(
    @Expose
    @SerializedName("claim_requests")
    val incomingRequests: List<AssetClaimingData>,

    @Expose
    @SerializedName("my_submitted_claim_requests")
    val outgoingRequests: List<AssetClaimingData>
) : Response