package com.bitmark.registry.util.encryption


/**
 * @author Hieu Pham
 * @since 2019-07-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface PublicKeyEncryption {

    fun encrypt(message: ByteArray, receiverPubKey: ByteArray): ByteArray

    fun decrypt(cipher: ByteArray, senderPubKey: ByteArray): ByteArray
}