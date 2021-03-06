/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.issuance.issuance

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.params.IssuanceParams
import com.bitmark.apiservice.params.RegistrationParams
import com.bitmark.apiservice.utils.Address
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.io.File

class IssuanceViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel(lifecycle) {

    private val registerPropertyLiveData = CompositeLiveData<Any>()

    internal val progressLiveData = MutableLiveData<Int>()

    private val getAccountNumberLiveData =
        CompositeLiveData<Pair<String, String>>()

    internal fun registerPropertyLiveData() =
        registerPropertyLiveData.asLiveData()

    internal fun getAccountNumberLiveData() =
        getAccountNumberLiveData.asLiveData()

    internal fun getAccountInfo() {
        getAccountNumberLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountNumber(),
                    accountRepo.getKeyAlias(),
                    BiFunction { a, k ->
                        Pair(a, k)
                    })
            )
        )
    }

    internal fun registerProperty(
        assetId: String,
        name: String,
        metadata: Map<String, String>,
        file: File,
        quantity: Int,
        registered: Boolean,
        keyPair: KeyPair
    ) {
        registerPropertyLiveData.add(
            rxLiveDataTransformer.completable(
                registerPropertyStream(
                    assetId,
                    name,
                    metadata,
                    file,
                    quantity,
                    registered,
                    keyPair
                )
            )
        )
    }

    private fun registerPropertyStream(
        assetId: String,
        name: String,
        metadata: Map<String, String>,
        file: File,
        quantity: Int,
        registered: Boolean,
        keyPair: KeyPair
    ): Completable {

        val streamCount = 3
        var progress = 0

        val registrationParams = RegistrationParams(name, metadata)
        registrationParams.setFingerprintFromFile(file)
        registrationParams.sign(keyPair)

        val registerAssetStream =
            if (registered) {
                Single.just(assetId)
            } else {
                bitmarkRepo.registerAsset(registrationParams)
            }

        return registerAssetStream.observeOn(Schedulers.io()).flatMap { id ->
            accountRepo.getAccountNumber().map { a -> Pair(a, id) }
        }
            .doOnSuccess { progressLiveData.set(++progress * 100 / streamCount) }
            .flatMap { p ->
                val issuanceParams = IssuanceParams(
                    p.second,
                    Address.fromAccountNumber(p.first),
                    quantity
                )
                issuanceParams.sign(keyPair)
                bitmarkRepo.issueBitmark(issuanceParams).andThen(Single.just(p))
                    .doOnSuccess { progressLiveData.set(++progress * 100 / streamCount) }
            }.observeOn(Schedulers.io()).flatMap { p ->
                bitmarkRepo.saveAssetFile(
                    p.first,
                    p.second,
                    file.name,
                    file.readBytes()
                )
                    .doOnSuccess { progressLiveData.set(++progress * 100 / streamCount) }
            }.ignoreElement()
    }

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }


}