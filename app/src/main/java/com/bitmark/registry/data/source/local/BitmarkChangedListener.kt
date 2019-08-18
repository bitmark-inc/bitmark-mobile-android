package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.model.BitmarkData


/**
 * @author Hieu Pham
 * @since 2019-07-14
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

interface BitmarkChangedListener

interface BitmarkDeletedListener :
    BitmarkChangedListener {
    fun onDeleted(bitmarkId: String, lastStatus: BitmarkData.Status)
}

interface BitmarkStatusChangedListener :
    BitmarkChangedListener {
    fun onChanged(
        bitmarkId: String,
        oldStatus: BitmarkData.Status,
        newStatus: BitmarkData.Status
    )
}

interface BitmarkSavedListener :
    BitmarkChangedListener {
    fun onBitmarksSaved(bitmarks: List<BitmarkData>)
}

interface BitmarkSeenListener : BitmarkChangedListener {
    fun onSeen(bitmarkId: String)
}