/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.logging

enum class Event(val value: String) {

    //region error tracking

    ACCOUNT_SIGN_UP_ERROR("account_sign_up_error"),

    ACCOUNT_RECOVER_ERROR("account_recover_error"),

    ACCOUNT_LOGOUT_ERROR("account_remove_access_error"),

    ACCOUNT_RECOVER_CLEAR_DATA_ERROR("account_recover_clear_data_error"),

    AUTH_ENABLE_ERROR("biometric_authentication_enable_error"),

    AUTH_INVALID_ERROR("biometric_authentication_invalid_error"),

    GOOGLE_DRIVE_ENABLE_ERROR("google_drive_enable_error"),

    GOOGLE_DRIVE_QUOTA_EXCEEDED("google_drive_quota_exceeded"),

    ASSET_SELECTION_FILE_ERROR("asset_selection_file_error"),

    ASSET_SELECTION_FILE_LIMIT("asset_selection_file_limited_error"),

    ASSET_SELECTION_FILE_UNSUPPORTED("asset_selection_file_unsupported_error"),

    PROP_REGISTRATION_ERROR("property_registration_error"),

    PROP_DETAIL_DOWNLOAD_ERROR("prop_detail_download_error"),

    PROP_DETAIL_DELETE_ERROR("prop_detail_delete_error"),

    PROP_TRANSFER_ERROR("prop_transfer_error"),

    MUSIC_CLAIMING_DOWNLOAD_ERROR("music_claiming_download_error"),

    MUSIC_CLAIMING_INFO_ERROR("music_claiming_info_error"),

    CHIBITRONIC_AUTHORIZE_ERROR("chibitronic_authorize_error"),

    //endregion

    //region info tracking

    DEVICE_NOT_ENCRYPTED("device_not_encrypted")

    //endregion info tracking

}