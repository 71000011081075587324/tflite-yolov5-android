package exclude

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader


/**
 * Created by fangzicheng
 */


fun main(args: Array<String>) {
    // 读取文本文件
    val inputFileName = "class"
    val inputFile = File("./tflite_yolov5/src/main/assets/$inputFileName.txt")
    val reader = BufferedReader(InputStreamReader(FileInputStream(inputFile), "UTF-8"))

    // 准备输出文件
    val outputFile = File("./tflite_yolov5/src/main/assets/${inputFileName + "_cn.txt"}")
    val writer = BufferedWriter(FileWriter(outputFile))

    // 逐行拼接文本
    // 使用 "," 分割每个英文单词，翻译会将 ","  翻译为 "、"，因此针对翻译结果通过 "," split 将每个结果分开
    // 如果返回结果 "," 翻译出现问题，则不采用逐行拼接一次处理翻译的方式，将下方的 onceTranlate 设置 false
    val onceTranlate = true
    var classNums = 0
    if (onceTranlate) {
        var lineStr: String? = null
        var allLineString: String? = null
        while (reader.readLine().also { lineStr = it } != null) {
            allLineString = if (allLineString == null) {
                lineStr
            } else {
                "$allLineString，$lineStr"
            }
            classNums++
        }
        println(allLineString)
        // 进行翻译
        allLineString?.let {
            // 调用翻译 API
            val translatedStr = YouDaoTranslationUtil.translate(it)
            println(translatedStr)
            val allLineTranslated = translatedStr.split("、")
            println(allLineTranslated.size)
            if (allLineTranslated.size != classNums) {
                println("翻译前后种类个数匹配不上，翻译出错")
            }

            allLineTranslated.forEach { lineCnStr ->
                // 将翻译后的内容写入输出文件
                println(lineCnStr)
                writer.write(lineCnStr)
                writer.newLine()
            }
        }
    } else {

    }


    // 关闭输入输出流
    reader.close()
    writer.close()
}