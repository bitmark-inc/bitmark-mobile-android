package com.bitmark.registry.feature.issuance.selection

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.params.RegistrationParams
import com.bitmark.apiservice.utils.error.HttpException
import com.bitmark.cryptography.crypto.Sha3512
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.AssetModelView
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-31
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AssetSelectionViewModel(
    lifecycle: Lifecycle,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel(lifecycle) {

    private val getAssetInfoLiveData = CompositeLiveData<AssetModelView>()

    internal val progressLiveData = MutableLiveData<Int>()

    internal fun getAssetInfoLiveData() = getAssetInfoLiveData.asLiveData()

    internal fun getAssetInfo(file: File) {
        getAssetInfoLiveData.add(
            rxLiveDataTransformer.single(
                getAssetInfoStream(
                    file
                )
            )
        )
    }

    private fun getAssetInfoStream(file: File): Single<AssetModelView> {

        var progress = 0
        val streamCount = 2

        val computeAssetIdentifier =
            Single.create<Pair<String, String>> { emt ->

                try {
                    val fileContent = file.readBytes()
                    val fingerprint =
                        RegistrationParams.computeFingerprint(fileContent)
                    val assetId = Sha3512.hash(RAW.decode(fingerprint))
                    emt.onSuccess(Pair(HEX.encode(assetId), fingerprint))

                } catch (e: Throwable) {
                    emt.onError(e)
                }

            }.subscribeOn(Schedulers.computation()).doOnSuccess {
                progressLiveData.set(++progress * 100 / streamCount)
            }

        val getAssetInfoStream: (String, String) -> Single<AssetModelView> =
            { assetId, fp ->
                bitmarkRepo.getAsset(assetId).map(mapAsset(file))
                    .onErrorResumeNext { e ->
                        if (e is HttpException && e.statusCode == 404) {
                            // asset not found in blockchain
                            Single.just(
                                AssetModelView(
                                    id = assetId,
                                    fingerprint = fp,
                                    fileName = file.name,
                                    filePath = file.absolutePath,
                                    registered = false
                                )
                            )
                        } else {
                            Single.error<AssetModelView>(e)
                        }
                    }.doOnSuccess {
                        progressLiveData.set(++progress * 100 / streamCount)
                    }
            }

        return computeAssetIdentifier.flatMap { p ->
            val assetId = p.first
            val fp = p.second

            getAssetInfoStream.invoke(assetId, fp)
        }
    }

    private fun mapAsset(file: File): (AssetData) -> AssetModelView = { asset ->
        AssetModelView(
            asset.id,
            asset.fingerprint,
            asset.metadata,
            asset.name,
            asset.registrant,
            asset.status,
            file.name,
            file.absolutePath,
            true
        )
    }

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}