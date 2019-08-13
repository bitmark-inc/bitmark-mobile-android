package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.apiservice.utils.record.AssetRecord
import com.google.gson.annotations.Expose


/**
 * @author Hieu Pham
 * @since 2019-08-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class AssetClaimingInfoResponse(
    @Expose
    val asset: AssetRecord,

    @Expose
    val totalEditionLeft: Int,

    @Expose
    val limitedEdition: Int,

    @Expose
    val issuer: String?
) : Response