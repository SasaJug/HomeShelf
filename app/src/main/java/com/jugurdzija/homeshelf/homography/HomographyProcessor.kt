package com.jugurdzija.homeshelf.homography

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

object HomographyProcessor {

    private const val MAX_DIM = 1024
    private const val MIN_MATCHES = 20
    private const val RANSAC_THRESHOLD = 3.0
    private const val MAX_AREA_RATIO = 16.0

    fun align(captured: Bitmap, reference: Bitmap): Bitmap? {
        val scaleC = downscaleFactor(captured)
        val scaleR = downscaleFactor(reference)
        val scaledCaptured = scaledDown(captured, scaleC)
        val scaledReference = scaledDown(reference, scaleR)

        val grayCapture = toGray(scaledCaptured)
        val grayRef = toGray(scaledReference)

        val orb = ORB.create(2000)
        val kpCapture = MatOfKeyPoint(); val descCapture = Mat()
        val kpRef = MatOfKeyPoint(); val descRef = Mat()
        orb.detectAndCompute(grayCapture, Mat(), kpCapture, descCapture)
        orb.detectAndCompute(grayRef, Mat(), kpRef, descRef)

        if (descCapture.empty() || descRef.empty()) {
            releaseAll(grayCapture, grayRef, descCapture, descRef)
            return null
        }

        val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING)
        val knnMatches = mutableListOf<MatOfDMatch>()
        matcher.knnMatch(descCapture, descRef, knnMatches, 2)

        val goodMatches = knnMatches.mapNotNull { m ->
            val arr = m.toArray()
            if (arr.size >= 2 && arr[0].distance < 0.75f * arr[1].distance) arr[0] else null
        }

        if (goodMatches.size < MIN_MATCHES) {
            releaseAll(grayCapture, grayRef, descCapture, descRef)
            return null
        }

        val kpCaptureList = kpCapture.toList()
        val kpRefList = kpRef.toList()

        val srcPts = MatOfPoint2f(*goodMatches.map {
            Point(kpCaptureList[it.queryIdx].pt.x, kpCaptureList[it.queryIdx].pt.y)
        }.toTypedArray())
        val dstPts = MatOfPoint2f(*goodMatches.map {
            Point(kpRefList[it.trainIdx].pt.x, kpRefList[it.trainIdx].pt.y)
        }.toTypedArray())

        val h = Calib3d.findHomography(srcPts, dstPts, Calib3d.RANSAC, RANSAC_THRESHOLD)

        if (h.empty() || !isHomographyValid(h, scaledCaptured.width, scaledCaptured.height, scaledReference.width, scaledReference.height)) {
            releaseAll(grayCapture, grayRef, descCapture, descRef, h)
            return null
        }

        val hFull = buildFullResHomography(h, scaleC, scaleR)

        val colorCapture = Mat()
        Utils.bitmapToMat(captured, colorCapture)

        val warped = Mat()
        Imgproc.warpPerspective(
            colorCapture, warped, hFull,
            Size(reference.width.toDouble(), reference.height.toDouble())
        )

        val result = createBitmap(reference.width, reference.height)
        Utils.matToBitmap(warped, result)

        releaseAll(grayCapture, grayRef, descCapture, descRef, colorCapture, warped, h, hFull)
        return result
    }

    private fun buildFullResHomography(h: Mat, scaleC: Float, scaleR: Float): Mat {
        val sc = Mat.zeros(3, 3, CvType.CV_64F)
        sc.put(0, 0, scaleC.toDouble())
        sc.put(1, 1, scaleC.toDouble())
        sc.put(2, 2, 1.0)

        val srInv = Mat.zeros(3, 3, CvType.CV_64F)
        srInv.put(0, 0, 1.0 / scaleR)
        srInv.put(1, 1, 1.0 / scaleR)
        srInv.put(2, 2, 1.0)

        val tmp = Mat()
        val hFull = Mat()
        Core.gemm(h, sc, 1.0, Mat(), 0.0, tmp)
        Core.gemm(srInv, tmp, 1.0, Mat(), 0.0, hFull)

        releaseAll(sc, srInv, tmp)
        return hFull
    }

    private fun isHomographyValid(h: Mat, srcW: Int, srcH: Int, dstW: Int, dstH: Int): Boolean {
        val corners = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(srcW.toDouble(), 0.0),
            Point(srcW.toDouble(), srcH.toDouble()),
            Point(0.0, srcH.toDouble())
        )
        val mapped = MatOfPoint2f()
        Core.perspectiveTransform(corners, mapped, h)
        val pts = mapped.toArray()

        val area = polygonArea(pts)
        val refArea = dstW.toDouble() * dstH
        val ratio = area / refArea
        if (ratio < 1.0 / MAX_AREA_RATIO || ratio > MAX_AREA_RATIO) return false

        val crossSigns = (0..3).map { i ->
            val p0 = pts[i]; val p1 = pts[(i + 1) % 4]; val p2 = pts[(i + 2) % 4]
            val cross = (p1.x - p0.x) * (p2.y - p1.y) - (p1.y - p0.y) * (p2.x - p1.x)
            cross > 0
        }
        return crossSigns.all { it } || crossSigns.none { it }
    }

    private fun polygonArea(pts: Array<Point>): Double {
        var area = 0.0
        val n = pts.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += pts[i].x * pts[j].y - pts[j].x * pts[i].y
        }
        return kotlin.math.abs(area) / 2.0
    }

    private fun downscaleFactor(bitmap: Bitmap): Float {
        val max = maxOf(bitmap.width, bitmap.height)
        return if (max <= MAX_DIM) 1.0f else MAX_DIM.toFloat() / max
    }

    private fun scaledDown(bitmap: Bitmap, scale: Float): Bitmap {
        if (scale >= 1.0f) return bitmap
        return bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
    }

    private fun toGray(bitmap: Bitmap): Mat {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        val gray = Mat(rgba.rows(), rgba.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()
        return gray
    }

    private fun releaseAll(vararg mats: Mat) = mats.forEach { it.release() }
}
