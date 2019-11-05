/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.event

import com.bitmark.registry.data.model.AssetData
import java.io.File

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