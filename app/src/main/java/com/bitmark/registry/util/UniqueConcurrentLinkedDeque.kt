package com.bitmark.registry.util

import com.bitmark.cryptography.crypto.Random
import com.bitmark.cryptography.crypto.Sha3256
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import java.util.concurrent.ConcurrentLinkedDeque


/**
 * @author Hieu Pham
 * @since 2019-08-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

class UniqueConcurrentLinkedDeque<T> :
    ConcurrentLinkedDeque<Pair<String, T>>() {

    override fun add(element: Pair<String, T>): Boolean {
        if (contains(element.first)) return false
        return super.add(element)
    }

    override fun addFirst(e: Pair<String, T>) {
        if (contains(e.first)) return
        super.addFirst(e)
    }

    override fun addLast(e: Pair<String, T>) {
        if (contains(e.first)) return
        super.addLast(e)
    }

    fun add(id: String = randomId(), element: T): Boolean {
        return add(Pair(id, element))
    }

    fun addFirst(id: String = randomId(), element: T): Boolean {
        if (contains(id)) return false
        addFirst(Pair(id, element))
        return true
    }

    fun addLast(id: String = randomId(), element: T): Boolean {
        if (contains(id)) return false
        addLast(Pair(id, element))
        return true
    }

    override fun addAll(elements: Collection<Pair<String, T>>): Boolean {
        val uniqueElements = elements.distinctBy { p -> p.first }
        return super.addAll(uniqueElements)
    }

    private fun contains(id: String) = this.find { p -> p.first == id } != null

    private fun randomId() = HEX.encode(Sha3256.hash(Random.randomBytes(32)))
}