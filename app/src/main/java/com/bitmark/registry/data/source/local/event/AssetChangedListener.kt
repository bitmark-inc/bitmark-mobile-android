package com.bitmark.registry.data.source.local.event

import com.bitmark.registry.data.model.AssetData
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

interface AssetChangedListener

interface AssetFileSavedListener : AssetChangedListener {

    fun onAssetFileSaved(assetId: String, file: File)
}

interface AssetSavedListener : AssetChangedListener {

    fun onAssetSaved(asset: AssetData, isNewRecord: Boolean)
}

interface AssetTypeChangedListener : AssetChangedListener {

    fun onAssetTypeChanged(assetId: String, type: AssetData.Type)
}