package com.example.recognitionproject.common.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log

/**
 * Created by fangzicheng
 */
object BitmapUtils {

    // 旋转 bitmap
    fun adjustPhotoRotation(bm: Bitmap, orientationDegree: Float): Bitmap? {
        val m = Matrix()
        m.setRotate(orientationDegree, bm.width.toFloat() / 2, bm.height.toFloat() / 2)
        try {
            return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
        } catch (ex: OutOfMemoryError) {
        }
        return null
    }

    /**
     *     缩放 bitmap 尺寸为模型期望的输入尺寸参数
     *     newWidth, newHeight 为期望缩放的尺寸
      */
    fun resizeBitmap(bitmap: Bitmap?, newWidth: Int?, newHeight: Int?): Bitmap? {
        if (bitmap == null || newWidth == null || newWidth <= 0 || newHeight == null || newHeight <= 0) return bitmap
        val width = bitmap.width
        val height = bitmap.height
        Log.d("fzc", "width = $width  height = $height")

        // 计算缩放比例
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height

        // 创建矩阵并执行缩放
        val matrix = Matrix()
        val scaleValue = if (scaleWidth >= scaleHeight) scaleHeight else scaleWidth
        matrix.postScale(scaleValue, scaleValue)

        // 创建新的位图
        val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
        resizedBitmap.setHasAlpha(true)

        // 如果缩放后的位图大小不足 newXXX，将其周围补空白裁剪至 newXXX
        if (resizedBitmap.width < newWidth || resizedBitmap.height < newHeight) {
            val dx = (newWidth - resizedBitmap.width) / 2
            val dy = (newHeight - resizedBitmap.height) / 2
            val finalBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(finalBitmap)
            canvas.drawARGB(255, 255, 255, 255)
            canvas.drawBitmap(resizedBitmap, dx.toFloat(), dy.toFloat(), null)
            return finalBitmap
        }
        return resizedBitmap
    }
}