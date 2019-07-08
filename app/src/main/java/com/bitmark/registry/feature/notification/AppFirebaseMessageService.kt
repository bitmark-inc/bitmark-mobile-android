package com.bitmark.registry.feature.notification

import com.google.firebase.messaging.FirebaseMessagingService


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AppFirebaseMessageService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
    }

}