package com.bitmark.registry.util.encryption

import com.bitmark.cryptography.crypto.Box
import com.bitmark.cryptography.crypto.Random.secureRandomBytes
import com.bitmark.cryptography.utils.ArrayUtils.concat
import com.bitmark.cryptography.utils.ArrayUtils.slice


/**
 * @author Hieu Pham
 * @since 2019-07-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BoxEncryption(private val privateKey: ByteArray) : PublicKeyEncryption {

    override fun encrypt(
        message: ByteArray,
        receiverPubKey: ByteArray
    ): ByteArray {
        val nonce = secureRandomBytes(Box.NONCE_BYTE_LENGTH)
        val cipher = Box.box(message, nonce, receiverPubKey, privateKey)
        return concat(nonce, cipher)
    }

    override fun decrypt(
        cipher: ByteArray,
        senderPubKey: ByteArray
    ): ByteArray {
        val nonce = slice(cipher, 0, Box.NONCE_BYTE_LENGTH)
        val rawCipher = slice(cipher, Box.NONCE_BYTE_LENGTH, cipher.size)
        return Box.unbox(rawCipher, nonce, senderPubKey, privateKey)
    }
}