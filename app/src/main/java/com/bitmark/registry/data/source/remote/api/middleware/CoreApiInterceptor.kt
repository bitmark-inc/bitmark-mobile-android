package com.bitmark.registry.data.source.remote.api.middleware

import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class CoreApiInterceptor @Inject constructor() : Interceptor() {

    override fun getTag(): String? = "CoreApi"
}