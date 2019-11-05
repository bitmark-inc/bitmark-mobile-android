/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.event

import com.bitmark.registry.data.model.entity.ActionRequired

interface AccountChangedListener

interface ActionRequiredDeletedListener : AccountChangedListener {
    fun onDeleted(actionId: ActionRequired.Id)
}

interface ActionRequiredAddedListener : AccountChangedListener {
    fun onAdded(actionIds: List<ActionRequired.Id>)
}