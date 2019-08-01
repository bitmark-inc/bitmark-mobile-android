package com.bitmark.registry.util.modelview

import android.os.Parcelable
import com.bitmark.registry.data.model.AssetData
import kotlinx.android.parcel.Parcelize


/**
 * @author Hieu Pham
 * @since 2019-07-31
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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