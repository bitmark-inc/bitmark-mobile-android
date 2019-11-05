/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.notification

import android.app.IntentService
import android.content.Intent
import com.google.firebase.iid.FirebaseInstanceId

class DeleteFirebaseInstanceIdService(name: String) : IntentService(name) {

    constructor() : this("DeleteFirebaseInstanceIdService")

    override fun onHandleIntent(intent: Intent?) {
        FirebaseInstanceId.getInstance().deleteInstanceId()
    }
}