package com.bitmark.registry.util.modelview


/**
 * @author Hieu Pham
 * @since 2019-08-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class AssetClaimingModelView(
    val assetId: String,
    val totalEditionLeft: Int,
    val limitedEdition: Int,
    val editionNumber: Int?
)