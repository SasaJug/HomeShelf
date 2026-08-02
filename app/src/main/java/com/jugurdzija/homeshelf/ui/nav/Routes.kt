package com.jugurdzija.homeshelf.ui.nav

import java.net.URLEncoder

object Routes {
    private const val EDIT_BASE = "edit"

    const val REFERENCE = "reference"
    const val REFERENCE_CAPTURE = "reference_capture"
    const val SCAN = "scan"
    const val REVIEW = "review/{storageId}"
    const val EDIT = "$EDIT_BASE?storageId={storageId}"
    const val MARK_ITEMS = "mark_items/{storageId}"
    const val SETTINGS = "settings"
    const val SHOPPING_LIST = "shopping_list"
    const val SHOPPING_LIST_ITEM = "shopping_list_item/{itemId}"
    const val GOLDEN_CAPTURE = "golden_capture/{storageId}"
    const val GOLDEN_SAVE = "golden_save"
    const val GOLDEN_MANAGE = "golden_manage/{storageId}"
    const val GOLDEN_DETAILS = "golden_details/{name}"

    fun edit(storageId: String? = null) =
        if (storageId != null) EDIT.replace("{storageId}", storageId) else EDIT_BASE

    fun markItems(storageId: String) = MARK_ITEMS.replace("{storageId}", storageId)

    fun shoppingListItem(itemId: String) = SHOPPING_LIST_ITEM.replace("{itemId}", itemId)

    fun goldenCapture(storageId: String) = GOLDEN_CAPTURE.replace("{storageId}", storageId)

    fun goldenManage(storageId: String) = GOLDEN_MANAGE.replace("{storageId}", storageId)

    fun goldenDetails(name: String) = GOLDEN_DETAILS.replace("{name}", URLEncoder.encode(name, "UTF-8"))

    fun review(storageId: String) = REVIEW.replace("{storageId}", storageId)

}
