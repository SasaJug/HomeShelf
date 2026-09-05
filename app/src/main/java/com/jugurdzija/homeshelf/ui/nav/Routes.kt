package com.jugurdzija.homeshelf.ui.nav

import java.net.URLEncoder

object Routes {
    private const val EDIT_BASE = "edit"
    private const val SCAN_BASE = "scan"
    private const val CONFIRM_CAPTURE_BASE = "confirm_capture"

    const val ARG_STORAGE_ID = "storageId"
    const val ARG_ITEM_ID = "itemId"
    const val ARG_NAME = "name"
    const val ARG_MODE = "mode"

    const val MODE_RESCAN = "rescan"

    const val REFERENCE = "reference"
    const val REFERENCE_CAPTURE = "reference_capture"
    const val REFERENCE_DETAIL = "reference_detail/{$ARG_STORAGE_ID}"
    const val SCAN = "$SCAN_BASE?$ARG_STORAGE_ID={$ARG_STORAGE_ID}&$ARG_MODE={$ARG_MODE}"
    const val REVIEW = "review/{$ARG_STORAGE_ID}"
    const val EDIT = "$EDIT_BASE?$ARG_STORAGE_ID={$ARG_STORAGE_ID}"
    const val CONFIRM_CAPTURE = "$CONFIRM_CAPTURE_BASE?$ARG_STORAGE_ID={$ARG_STORAGE_ID}"
    const val MARK_ITEMS = "mark_items/{$ARG_STORAGE_ID}"
    const val SETTINGS = "settings"
    const val SHOPPING_LIST = "shopping_list"
    const val SHOPPING_LIST_ITEM = "shopping_list_item/{$ARG_ITEM_ID}"
    const val GOLDEN_CAPTURE = "golden_capture/{$ARG_STORAGE_ID}"
    const val GOLDEN_SAVE = "golden_save"
    const val GOLDEN_MANAGE = "golden_manage/{$ARG_STORAGE_ID}"
    const val GOLDEN_DETAILS = "golden_details/{$ARG_NAME}"

    fun edit(storageId: String) = EDIT.replace("{$ARG_STORAGE_ID}", storageId)

    fun scan(storageId: String? = null, rescan: Boolean = false): String {
        if (storageId == null) return SCAN_BASE
        val mode = if (rescan) MODE_RESCAN else ""
        return "$SCAN_BASE?$ARG_STORAGE_ID=$storageId&$ARG_MODE=$mode"
    }

    fun confirmCapture(storageId: String? = null) =
        if (storageId != null) CONFIRM_CAPTURE.replace("{$ARG_STORAGE_ID}", storageId) else CONFIRM_CAPTURE_BASE

    fun referenceDetail(storageId: String) = REFERENCE_DETAIL.replace("{$ARG_STORAGE_ID}", storageId)

    fun markItems(storageId: String) = MARK_ITEMS.replace("{$ARG_STORAGE_ID}", storageId)

    fun shoppingListItem(itemId: String) = SHOPPING_LIST_ITEM.replace("{$ARG_ITEM_ID}", itemId)

    fun goldenCapture(storageId: String) = GOLDEN_CAPTURE.replace("{$ARG_STORAGE_ID}", storageId)

    fun goldenManage(storageId: String) = GOLDEN_MANAGE.replace("{$ARG_STORAGE_ID}", storageId)

    fun goldenDetails(name: String) = GOLDEN_DETAILS.replace("{$ARG_NAME}", URLEncoder.encode(name, "UTF-8"))

    fun review(storageId: String) = REVIEW.replace("{$ARG_STORAGE_ID}", storageId)

}
