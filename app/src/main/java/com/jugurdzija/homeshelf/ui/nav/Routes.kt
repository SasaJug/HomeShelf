package com.jugurdzija.homeshelf.ui.nav

object Routes {
    private const val EDIT_BASE = "edit"

    const val REFERENCE = "reference"
    const val REFERENCE_CAPTURE = "reference_capture"
    const val COMPARE = "compare"
    const val SCAN = "scan"
    const val REVIEW = "review/{storageId}"
    const val EDIT = "$EDIT_BASE?storageId={storageId}"
    const val SETTINGS = "settings"
    const val GOLDEN_CAPTURE = "golden_capture/{storageId}"
    const val GOLDEN_SAVE = "golden_save"
    const val GOLDEN_MANAGE = "golden_manage"
    const val GOLDEN_VIEW = "golden_view"
    const val TEST_SELECT = "test_select"
    const val TEST_RUN = "test_run"
    const val TEST_RESULTS = "test_results"

    fun edit(storageId: String? = null) =
        if (storageId != null) EDIT.replace("{storageId}", storageId) else EDIT_BASE

    fun goldenCapture(storageId: String) = GOLDEN_CAPTURE.replace("{storageId}", storageId)

    fun review(storageId: String) = REVIEW.replace("{storageId}", storageId)

}
