package com.bitmark.registry.util.modelview

import android.os.Parcelable
import com.bitmark.registry.R
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.source.Constant.OMNISCIENT_ASSET_ID
import com.bitmark.registry.util.DateTimeUtil
import com.bitmark.registry.util.DateTimeUtil.Companion.OFFICIAL_DATE_FORMAT
import kotlinx.android.parcel.Parcelize
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Parcelize
class BitmarkModelView constructor(
    val id: String,
    val name: String?,
    private val confirmedAt: String?,
    private val createdAt: String? = null,
    val issuer: String,
    val readableIssuer: String?,
    val headId: String,
    val metadata: Map<String, String>?,
    val accountNumber: String,
    var seen: Boolean = false,
    var assetType: AssetData.Type,
    var status: BitmarkData.Status,
    var assetFile: File? = null,
    val assetId: String,
    var previousOwner: String? = null,
    val offset: Long,
    val edition: Int? = null,
    val totalEdition: Int? = null
) : Parcelable {

    companion object {

        fun newInstance(
            bitmark: BitmarkData,
            accountNumber: String
        ): BitmarkModelView {
            val assetFile = bitmark.asset?.file

            return BitmarkModelView(
                id = bitmark.id,
                name = bitmark.asset?.name ?: "",
                confirmedAt = bitmark.confirmedAt,
                createdAt = bitmark.createdAt,
                issuer = bitmark.issuer,
                readableIssuer = bitmark.readableIssuer,
                headId = bitmark.headId,
                metadata = bitmark.asset?.metadata ?: mapOf(),
                accountNumber = accountNumber,
                seen = bitmark.seen,
                assetType = bitmark.asset?.type ?: AssetData.Type.UNKNOWN,
                status = bitmark.status,
                assetFile = assetFile,
                assetId = bitmark.assetId,
                offset = bitmark.offset,
                edition = bitmark.edition,
                totalEdition = bitmark.totalEdition
            )
        }
    }

    fun confirmedAt() =
        if (!confirmedAt.isNullOrEmpty()) DateTimeUtil.stringToString(
            confirmedAt
        ) else null

    fun createdAt() =
        if (!createdAt.isNullOrEmpty()) DateTimeUtil.stringToString(
            createdAt,
            OFFICIAL_DATE_FORMAT
        ) else null

    fun isSettled() = status == BitmarkData.Status.SETTLED

    fun isPending() =
        status == BitmarkData.Status.TRANSFERRING || status == BitmarkData.Status.ISSUING

    fun isMusicClaiming() = OMNISCIENT_ASSET_ID == assetId

    fun getThumbnailRes(): Int {
        return when (assetType) {
            AssetData.Type.IMAGE -> R.drawable.ic_asset_image
            AssetData.Type.VIDEO -> R.drawable.ic_asset_video
            AssetData.Type.HEALTH -> R.drawable.ic_asset_health_data
            AssetData.Type.MEDICAL -> R.drawable.ic_asset_medical_record
            AssetData.Type.ZIP -> R.drawable.ic_asset_zip
            AssetData.Type.DOC -> R.drawable.ic_asset_doc
            AssetData.Type.UNKNOWN -> R.drawable.ic_asset_unknow
        }
    }
}