/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.encryption

import com.bitmark.cryptography.crypto.Chacha20Poly1305

class AssetEncryption : SecretKeyEncryption {

    companion object {
        val NONCE = ByteArray(12)
    }

    private var secretKey: ByteArray

    constructor() {
        secretKey = Chacha20Poly1305.generateIetfKey()
    }

    constructor(secretKey: ByteArray) {
        this.secretKey = secretKey
    }

    override fun encrypt(
        message: ByteArray,
        receiverPubKey: ByteArray
    ): ByteArray {
        return Chacha20Poly1305.aeadIetfEncrypt(message, null, NONCE, secretKey)
    }

    override fun decrypt(cipher: ByteArray): ByteArray =
        Chacha20Poly1305.aeadIetfDecrypt(cipher, null, NONCE, secretKey)

    override fun getAlgorithm(): String = "chacha20poly1305"

    fun getSessionData(
        receiverPubKey: ByteArray,
        keyEncryptor: PublicKeyEncryption
    ) = SessionData.from(
        secretKey,
        getAlgorithm(),
        receiverPubKey,
        keyEncryptor
    )
}