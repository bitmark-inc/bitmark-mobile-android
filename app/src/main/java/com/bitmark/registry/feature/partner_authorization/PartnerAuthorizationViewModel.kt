/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.partner_authorization

import androidx.lifecycle.Lifecycle
import com.bitmark.apiservice.utils.error.HttpException
import com.bitmark.cryptography.crypto.Ed25519
import com.bitmark.cryptography.crypto.encoder.Hex
import com.bitmark.cryptography.crypto.encoder.Raw
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.remote.api.service.ServiceGenerator
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.extension.toJson
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class PartnerAuthorizationViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) :
    BaseViewModel(lifecycle) {

    private val getAccountInfoLiveData =
        CompositeLiveData<Pair<String, String>>()

    private val authorizeLiveData = CompositeLiveData<String>()

    internal fun getAccountInfoLiveData() =
        getAccountInfoLiveData.asLiveData()

    internal fun authorizeLiveData() = authorizeLiveData.asLiveData()

    internal fun getAccountInfo() =
        getAccountInfoLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountNumber(),
                    accountRepo.getKeyAlias(),
                    BiFunction { accountNumber, keyAlias ->
                        Pair(
                            accountNumber,
                            keyAlias
                        )
                    })
            )
        )

    internal fun authorize(
        accountNumber: String,
        url: String,
        code: String,
        keyPair: KeyPair
    ) {
        authorizeLiveData.add(
            rxLiveDataTransformer.single(
                authorizeStream(accountNumber, url, code, keyPair)
            )
        )
    }

    private fun authorizeStream(
        accountNumber: String,
        url: String,
        code: String,
        keyPair: KeyPair
    ) = Single.create(SingleOnSubscribe<String> { emt ->
        val message = "Verify|$code"
        val signature = Hex.HEX.encode(
            Ed25519.sign(
                Raw.RAW.decode(message),
                keyPair.privateKey().toBytes()
            )
        )
        val params = mapOf<String, String>(
            "bitmark_account" to accountNumber,
            "code" to code,
            "signature" to signature
        )
        val requestBody = params.toJson().toRequestBody()
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody).build()

        val httpClient = ServiceGenerator.buildHttpClient()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emt.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful)
                    emt.onSuccess(url)
                else
                    emt.onError(
                        HttpException(
                            response.code,
                            response.body!!.string()
                        )
                    )
            }

        })

    }).subscribeOn(Schedulers.io())
}