/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.event

import com.bitmark.registry.data.model.BitmarkData

interface BitmarkChangedListener

interface BitmarkDeletedListener : BitmarkChangedListener {
    fun onDeleted(bitmarkId: String, lastStatus: BitmarkData.Status)
}

interface BitmarkStatusChangedListener : BitmarkChangedListener {
    fun onChanged(
        bitmarkId: String,
        oldStatus: BitmarkData.Status,
        newStatus: BitmarkData.Status
    )
}

interface BitmarkSavedListener : BitmarkChangedListener {
    fun onBitmarkSaved(bitmark: BitmarkData)
}

interface BitmarkSeenListener : BitmarkChangedListener {
    fun onSeen(bitmarkId: String)
}