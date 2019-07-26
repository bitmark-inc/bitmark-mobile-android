package com.bitmark.registry.data.source.local.api

import android.content.Context
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SharedPrefApi @Inject constructor(
    context: Context, gson: Gson
) {

    private val sharePrefGateway = SharePrefGateway(context, gson)

    companion object {
        const val ACCOUNT_NUMBER = "active_account_number"
        const val AUTH_REQUIRED = "auth_required"
        const val ACTION_REQUIRED = "action_required"
        const val KEY_ALIAS = "encryption_key_alias"
        const val ACCESS_REMOVED = "access_removed"
    }

    fun <T> rxSingle(action: (SharePrefGateway) -> T): Single<T> {
        return Single.create(SingleOnSubscribe<T> { e ->
            try {
                e.onSuccess(action.invoke(sharePrefGateway))
            } catch (ex: Exception) {
                e.onError(ex)
            }
        }).subscribeOn(Schedulers.io())
    }

    fun rxCompletable(action: (SharePrefGateway) -> Unit): Completable {
        return Completable.create { e ->
            try {
                action.invoke(sharePrefGateway)
                e.onComplete()
            } catch (ex: Exception) {
                e.onError(ex)
            }

        }.subscribeOn(Schedulers.io())
    }
}