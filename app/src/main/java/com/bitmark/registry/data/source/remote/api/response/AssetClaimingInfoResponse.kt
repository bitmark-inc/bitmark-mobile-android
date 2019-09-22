package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.apiservice.utils.record.AssetRecord
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-08-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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