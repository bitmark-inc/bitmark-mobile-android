package com.bitmark.registry.feature


/**
 * @author Hieu Pham
 * @since 2019-07-29
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface BehaviorComponent {

    /**
     * Refresh stuff like view, data or something
     */
    fun refresh() {}

    fun onBackPressed(): Boolean = false
}