package com.jugurdzija.homeshelf.ui.detail

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine

object BitmapDetailHolder {
    var pending: Bitmap? = null
    var pendingGuideLines: List<GuideLine> = emptyList()
    var pendingReferenceFilePath: String? = null
}
