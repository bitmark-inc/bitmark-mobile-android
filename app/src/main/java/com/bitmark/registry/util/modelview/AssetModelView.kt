/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.modelview

import android.os.Parcelable
import com.bitmark.registry.data.model.AssetData
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AssetModelView(
    val id: String,
    val fingerprint: String,
    val metadata: Map<String, String>? = null,
    val name: String? = null,
    val registrant: String? = null,
    val status: AssetData.Status? = null,
    val fileName: String? = null,
    val filePath: String? = null,
    val registered: Boolean = false
) : Parcelable