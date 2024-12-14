package com.example.tflite_yolov5.utils

import android.content.res.AssetManager
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Created by fangzicheng
 */
object FileUtils {
    /**
     * Memory-map the model file in Assets.
     */
    @Throws(IOException::class)
    @JvmStatic
    fun loadModelFile(assets: AssetManager, modelFilename: String?): MappedByteBuffer? {
        val fileDescriptor = assets.openFd(modelFilename!!)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}