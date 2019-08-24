package com.bitmark.registry.data.source.local.event

import com.bitmark.registry.data.model.AssetData


/**
 * @author Hieu Pham
 * @since 2019-07-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

interface AssetChangedListener

interface AssetFileSavedListener : AssetChangedListener {
    fun onSaved(assetId: String)
}

interface AssetSavedListener : AssetChangedListener {

    fun onAssetSaved(asset: AssetData, isNewRecord: Boolean)
}