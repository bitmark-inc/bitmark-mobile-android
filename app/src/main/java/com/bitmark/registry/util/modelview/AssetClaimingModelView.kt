/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.modelview

data class AssetClaimingModelView(
    val assetId: String,
    val totalEditionLeft: Int,
    val limitedEdition: Int,
    val editionNumber: Int?
)