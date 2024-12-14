package exclude

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * Created by fangzicheng on 4/14/23
 */
object YouDaoTranslationUtil {
    // 有道翻译API接口地址
    // 如果无法使用，可能是账号金额已经使用完，可自行前往有道智能云平台注册，然后替换下方对应的应用 ID 与 应用密钥
    private const val YOUDAO_API_URL = "https://openapi.youdao.com/api"
    // 应用ID
    private const val APP_KEY = "45125dde7af8124e"
    // 应用密钥
    private const val APP_SECRET = "l4ZFprBwcgnS93S0EmFg9jcAh9oKzqqX"

    var connection: HttpURLConnection? = null

    @Throws(Exception::class)
    fun translate(inputText: String): String {
        // 拼接请求URL参数
        val salt = System.currentTimeMillis().toString()
        val sign = md5(APP_KEY + inputText + salt + APP_SECRET)
        val urlParameters = String.format(
            "?q=%s&from=auto&to=zh-CHS&appKey=%s&salt=%s&sign=%s",
            URLEncoder.encode(inputText, "UTF-8"), APP_KEY, salt, sign
        )

        // 发送HTTP POST请求
        val url = URL(YOUDAO_API_URL + urlParameters)
        connection = url.openConnection() as HttpURLConnection
        connection!!.requestMethod = "POST"
        connection!!.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection!!.connect()
        val reader = BufferedReader(
            InputStreamReader(
                connection!!.inputStream, "UTF-8"
            )
        )

        // 解析翻译结果
        val resultBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            resultBuilder.append(line)
        }
        reader.close()
        val resultJson = resultBuilder.toString()
        val translatedText = getTranslatedText(resultJson, "translation")
        println(translatedText)
        println(resultJson)
        connection!!.disconnect()
        return translatedText
    }

    // 获取翻译结果中的中文翻译
    private fun getTranslatedText(resultJson: String, fieldName: String): String {
        val regex = "\"$fieldName\":\\[\"(.+?)\"\\]"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(resultJson)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            ""
        }
    }

    // 将原始文本中的英文替换为中文
    private fun replaceEnWithZh(inputText: String, translatedText: String): String {
        return inputText.replace("[a-zA-Z]+".toRegex(), translatedText)
    }

    // 对字符串进行MD5加密
    @Throws(Exception::class)
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        val builder = StringBuilder()
        for (b in bytes) {
            builder.append(String.format("%02x", b))
        }
        return builder.toString()
    }

//    @JvmStatic
//    fun main(args: Array<String>) {
//        try {
//            translate("test")
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
}