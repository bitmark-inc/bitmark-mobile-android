package com.bitmark.registry.feature.notification

import android.app.IntentService
import android.content.Intent
import com.google.firebase.iid.FirebaseInstanceId


/**
 * @author Hieu Pham
 * @since 2019-07-30
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class DeleteFirebaseInstanceIdService(name: String) : IntentService(name) {

    constructor() : this("DeleteFirebaseInstanceIdService")

    override fun onHandleIntent(intent: Intent?) {
        FirebaseInstanceId.getInstance().deleteInstanceId()
    }
}