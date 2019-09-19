package com.bitmark.registry.data.source.local.event

import com.bitmark.registry.data.model.entity.ActionRequired


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

interface AccountChangedListener

interface ActionRequiredDeletedListener : AccountChangedListener {
    fun onDeleted(actionId: ActionRequired.Id)
}

interface ActionRequiredAddedListener : AccountChangedListener {
    fun onAdded(actionIds: List<ActionRequired.Id>)
}