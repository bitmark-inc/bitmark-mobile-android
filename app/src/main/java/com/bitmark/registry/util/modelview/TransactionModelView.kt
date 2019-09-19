package com.bitmark.registry.util.modelview

import android.os.Parcelable
import com.bitmark.registry.data.model.entity.AssetClaimingData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.util.DateTimeUtil
import com.bitmark.registry.util.DateTimeUtil.Companion.ISO8601_SIMPLE_FORMAT
import com.bitmark.registry.util.DateTimeUtil.Companion.OFFICIAL_DATE_TIME_FORMAT
import kotlinx.android.parcel.Parcelize


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Parcelize
data class TransactionModelView(
    val id: String? = null,
    val confirmedAt: String? = null,
    val owner: String? = null,
    val previousOwner: String? = null,
    val assetName: String? = null,
    val status: String? = null,
    val accountNumber: String? = null,
    val offset: Long? = null,
    val claimId: String? = null,
    val from: String? = null,
    val to: String? = null
) : Parcelable {

    companion object {
        fun newInstance(
            tx: TransactionData,
            accountNumber: String? = null
        ) = TransactionModelView(
            id = tx.id,
            confirmedAt = tx.block?.createdAt,
            owner = tx.owner,
            previousOwner = tx.previousOwner,
            assetName = tx.asset?.name,
            status = tx.status.value,
            accountNumber = accountNumber,
            offset = tx.offset
        )

        fun newInstance(
            assetClaiming: AssetClaimingData,
            accountNumber: String? = null
        ) = TransactionModelView(
            claimId = assetClaiming.id,
            confirmedAt = assetClaiming.createdAt,
            from = assetClaiming.from,
            to = assetClaiming.asset?.registrant,
            assetName = assetClaiming.asset?.name,
            status = assetClaiming.status.value,
            accountNumber = accountNumber
        )
    }

    fun isPending() =
        status == TransactionData.Status.PENDING.value

    fun isAssetClaimingAccepted() =
        status == AssetClaimingData.Status.ACCEPTED.value

    fun isAssetClaimingRejected() =
        status == AssetClaimingData.Status.REJECTED.value

    fun isAssetClaimingPending() =
        status == AssetClaimingData.Status.PENDING.value

    fun confirmedAt(format: String = ISO8601_SIMPLE_FORMAT) =
        if (confirmedAt == null) "" else DateTimeUtil.stringToString(
            confirmedAt,
            format,
            OFFICIAL_DATE_TIME_FORMAT
        )

    fun isIssuance() = previousOwner == null

    fun isOwning() = owner == accountNumber

    fun isAssetClaiming() = claimId != null
}