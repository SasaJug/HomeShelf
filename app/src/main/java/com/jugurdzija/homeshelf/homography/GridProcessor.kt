package com.jugurdzija.homeshelf.homography

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GridCell
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridProcessor @Inject constructor() {

    fun extract(bitmap: Bitmap, hPixels: List<Float>, vPixels: List<Float>): List<GridCell> {
        if (hPixels.size < 2 || vPixels.size < 2) return emptyList()
        val cells = mutableListOf<GridCell>()
        hPixels.zipWithNext().forEachIndexed { rowIdx, (top, bottom) ->
            vPixels.zipWithNext().forEachIndexed { colIdx, (left, right) ->
                val x = left.toInt().coerceIn(0, bitmap.width - 1)
                val y = top.toInt().coerceIn(0, bitmap.height - 1)
                val w = (right - left).toInt().coerceIn(1, bitmap.width - x)
                val h = (bottom - top).toInt().coerceIn(1, bitmap.height - y)
                val crop = Bitmap.createBitmap(bitmap, x, y, w, h)
                cells.add(GridCell(name = "${'A' + colIdx}${rowIdx + 1}", bitmap = crop))
            }
        }
        return cells
    }
}
