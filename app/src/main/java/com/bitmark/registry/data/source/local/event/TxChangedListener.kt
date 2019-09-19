package com.bitmark.registry.data.source.local.event

import com.bitmark.registry.data.model.TransactionData


/**
 * @author Hieu Pham
 * @since 2019-07-27
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

interface TxChangedListener

interface TxSavedListener : TxChangedListener {

    fun onTxSaved(tx: TransactionData)
}