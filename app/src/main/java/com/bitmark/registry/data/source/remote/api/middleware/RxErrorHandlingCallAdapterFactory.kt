package com.bitmark.registry.data.source.remote.api.middleware

import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RxErrorHandlingCallAdapterFactory : CallAdapter.Factory(){

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}