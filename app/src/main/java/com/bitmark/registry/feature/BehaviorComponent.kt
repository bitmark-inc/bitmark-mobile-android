/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature

interface BehaviorComponent {

    /**
     * Refresh stuff like view, data or something
     */
    fun refresh() {}

    fun onBackPressed(): Boolean = false
}