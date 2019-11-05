/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.encryption

import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SessionData(
    @Expose
    @SerializedName("enc_data_key")
    val encryptedKey: String,

    @Expose
    @SerializedName("data_key_alg")
    val algorithm: String
) {

    companion object {

        fun from(
            sessionKey: ByteArray,
            algorithm: String,
            receiverPubKey: ByteArray,
            encryptor: PublicKeyEncryption
        ): SessionData {
            val encryptedKey = encryptor.encrypt(sessionKey, receiverPubKey)
            return SessionData(HEX.encode(encryptedKey), algorithm)
        }
    }

    fun getRawKey(
        decryptor: PublicKeyEncryption,
        senderPubKey: ByteArray
    ): ByteArray = decryptor.decrypt(HEX.decode(encryptedKey), senderPubKey)
}