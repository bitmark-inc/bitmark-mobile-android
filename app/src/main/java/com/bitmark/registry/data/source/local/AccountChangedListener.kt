package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.model.ActionRequired


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

interface AccountChangedListener

interface ActionRequiredDeletedListener {
    fun onDeleted(actionId: ActionRequired.Id)
}