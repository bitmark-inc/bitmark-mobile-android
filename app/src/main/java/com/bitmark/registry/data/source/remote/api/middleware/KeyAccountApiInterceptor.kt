package com.bitmark.registry.data.source.remote.api.middleware

import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-09-28
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class KeyAccountApiInterceptor @Inject constructor() : Interceptor() {

    override fun getTag(): String? = "KeyAccountApi"

}