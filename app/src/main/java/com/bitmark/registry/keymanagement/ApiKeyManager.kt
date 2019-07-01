package com.bitmark.registry.keymanagement

internal class ApiKeyManager {

    val bitmarkApiKey: String
        external get

    val intercomApiKey: String
        external get

    companion object {

        val API_KEY_MANAGER = ApiKeyManager()

        init {
            System.loadLibrary("api-key")
        }
    }

}
