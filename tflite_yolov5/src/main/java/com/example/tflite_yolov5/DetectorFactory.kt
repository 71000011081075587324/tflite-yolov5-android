package com.example.tflite_yolov5

import android.content.Context
import com.example.tflite_yolov5.classifier.YoloV5Classifier
import java.io.IOException

/**
 * Created by fangzicheng
 */
object DetectorFactory {
//    // 模型期望的输入尺寸参数
//    // 不同模型的期望输入尺寸参数可能不同，需要提前查看 .tflite 模型的期望尺寸参数，否则应用会 crash
//    const val INPUT_SIZE = 416

    /**
     * @param context: 用于获取 assetManager 进而获取 assets 下的文件
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    @JvmStatic
    fun getDetector(context: Context): YoloV5Classifier? {
        val assetManager = context.applicationContext.assets
        val modelFilename = "yolov5s-fp16.tflite"
        val labelFilename = "class_cn.txt"
        val isQuantized = false
        val isNNAPI = false
        val isGPU = false
        //        int[] output_width = new int[]{0};
//        int[][] masks = new int[][]{{0}};
//        int[] anchors = new int[]{0};
        return YoloV5Classifier.create(
            assetManager, modelFilename, labelFilename, isQuantized,
            isNNAPI, isGPU
        )
    }

    /**
     * @param context: 用于获取 assetManager 进而获取 assets 下的文件
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    @JvmStatic
    fun getDetector(context: Context, modelFilename : String, labelFilename : String): YoloV5Classifier? {
        val assetManager = context.applicationContext.assets
        val modelFilename = modelFilename
        val labelFilename = labelFilename
        val isQuantized = false
        val isNNAPI = false
        val isGPU = false
        //        int[] output_width = new int[]{0};
//        int[][] masks = new int[][]{{0}};
//        int[] anchors = new int[]{0};
        return YoloV5Classifier.create(
            assetManager, modelFilename, labelFilename, isQuantized,
            isNNAPI, isGPU
        )
    }

}