package com.bitmark.registry.util.encryption

import com.bitmark.cryptography.crypto.Chacha20Poly1305


/**
 * @author Hieu Pham
 * @since 2019-07-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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