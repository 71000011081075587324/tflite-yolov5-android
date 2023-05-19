package com.example.recognitionproject

import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.recognitionproject.activity.BaseActivity
import com.example.recognitionproject.common.ui.LButton
import com.example.recognitionproject.common.ui.RecognitionSurfaceView
import com.example.recognitionproject.common.utils.BitmapUtils
import com.example.tflite_yolov5.DetectorFactory
import com.example.tflite_yolov5.classifier.Classifier
import com.example.tflite_yolov5.classifier.YoloV5Classifier
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : BaseActivity() {

//    private var viewFinder: PreviewView? = null
    private var lbtnBack: LButton? = null
    private var cameraExecutor: ExecutorService? = null
    private var surfaceView : RecognitionSurfaceView? = null

    private var detector: YoloV5Classifier? = null

    private var textToSpeech: TextToSpeech? = null
    private var textToSpeechHasReady = false

    private var lastMap : MutableMap<String, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // 保持屏幕常亮
        setContentView(R.layout.activity_camera)
        cameraExecutor = Executors.newSingleThreadExecutor()
        initView()
        initDetector()
        initTextToSpeech()
        startCamera()
    }

    private fun initView() {
//        viewFinder = findViewById(R.id.viewFinder)
        lbtnBack = findViewById(R.id.lbtn_camera_back)
        surfaceView = findViewById(R.id.surface_view_recognition)

        lbtnBack?.setOnClickListener {
            finish()
        }
    }

    /**
     * 构造物体识别模型检测器
     */
    private fun initDetector() {
        try {
            detector = DetectorFactory.getDetector(applicationContext)
        } catch (e: IOException) {
            e.printStackTrace()
            val toast = Toast.makeText(
                applicationContext, "物体识别模型初始化失败", Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(baseContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                //设置首选语言为中文,注意，语言可能是不可用的，结果将指示此
                val result = textToSpeech?.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    //语言数据丢失或不支持该语言
                    // 一般现在手机都有默认集成一些中文语音包(如科大讯飞语音包)
                    // 如果手机不支持中文语音包，则本项目暂时未兼容，需要添加语音识别 SDK
                    Toast.makeText(this, "语言数据丢失或不支持该语言", Toast.LENGTH_SHORT).show()
                } else {
                    //检查文档中其他可能的结果代码。
                    // 例如，语言可能对区域设置可用，但对指定的国家和变体不可用
                    // TTS引擎已成功初始化。
                    textToSpeechHasReady = true
//                    textToSpeech?.speak("语音播报引擎已成功初始化，下面将开始物体识别语音播放", TextToSpeech.QUEUE_FLUSH, null, "")
                }
            } else {
                // 初始化失败
                Toast.makeText(this, "语音播放初始化失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     *  使用 CameraX api
     *  打开摄像头并对摄像头实时帧添加 Analyzer 进行分析处理
     *  官方文档: https://developer.android.com/training/camerax?hl=zh-cn
     */
    private fun startCamera() {
        // 创建 ProcessCameraProvider 的实例。这用于将相机的生命周期绑定到生命周期所有者。
        // 这消除了打开和关闭相机的任务，因为 CameraX 具有生命周期感知能力
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        //添加监听器。
        // 添加 Runnable 作为一个参数
        // 添加 ContextCompat.getMainExecutor() 作为第二个参数, 传入一个在主线程上运行的 Executor
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            // 用于将相机的生命周期绑定到应用进程中的 LifecycleOwner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//            // Preview
//            // 初始化 Preview 对象，在其上调用 build，从取景器中获取 Surface 提供程序，然后在预览上进行设置
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(viewFinder?.surfaceProvider)
//                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    cameraExecutor?.let { executor ->
                        it.setAnalyzer(executor, CameraAnalyzer {
                            if (this.isDestroyed) {
                                // 存在多线程情况，页面已经被销毁，但是该线程任务刚开始执行
                                // 因此当页面被销毁时需要释放相应资源
                                detector?.close()
                                return@CameraAnalyzer
                            }
                            if (!textToSpeechHasReady) {
                                // 语音播放未初始化成功
                                return@CameraAnalyzer
                            }
                            // 传入参数为摄像头 bitmap
                            val startTime = SystemClock.uptimeMillis()
                            val finalBitmap = BitmapUtils.resizeBitmap(it, detector?.inputSize, detector?.inputSize)
                            val results = detector?.recognizeImage(finalBitmap)
                            surfaceView?.setCameraBitmap(finalBitmap)
                            surfaceView?.setNewRecognitions(results)
                            results?.apply {
                                if (recognitionsHasDiff(results)) {
                                    Log.d("fzc", "recognizeImage process timeMs : ${SystemClock.uptimeMillis() - startTime}")
                                    recognitionsToSpeech(results, finalBitmap!!.width/2, finalBitmap.height/2 )
                                }
                            }

                        })
                    }
                }

            try {
                // Unbind use cases before rebinding
                // 确保没有任何内容绑定到 cameraProvider
                cameraProvider.unbindAll()

                // Bind use cases to camera
                // 将 cameraSelector 、预览对象、相机帧调用绑定到 cameraProvider
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalyzer)

            } catch(exc: Exception) {
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private class CameraAnalyzer(private val listener: (bitmap : Bitmap?) -> Unit) : ImageAnalysis.Analyzer {
        //  ImageProxy，它是 Media.Image 的封装容器
        override fun analyze(image: ImageProxy) {
            imageProxyToBitmap(image)
            listener(imageProxyToBitmap(image))

            // 将 ImageProxy 发布到 CameraX
            image.close()
        }

        private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
            val planes: Array<out ImageProxy.PlaneProxy> = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height, Bitmap.Config.ARGB_8888
            )
            buffer.position(0)
            bitmap.copyPixelsFromBuffer(buffer)
            return BitmapUtils.adjustPhotoRotation(bitmap, 90f)
        }

    }

    private fun recognitionsHasDiff(newRecognitions : MutableList<Classifier.Recognition>) : Boolean {
        if (lastMap == null) {
            lastMap = mutableMapOf<String, Int>().apply {
                newRecognitions.forEach { recognition ->
                    if (this.containsKey(recognition.title)) {
                        this[recognition.title] = this.getValue(recognition.title) + 1
                    } else {
                        this[recognition.title] = 1
                    }

                }
            }
            return true
        }

        val newMap = mutableMapOf<String, Int>().apply {
            newRecognitions.forEach { recognition ->
                if (this.containsKey(recognition.title)) {
                    this[recognition.title] = this.getValue(recognition.title) + 1
                } else {
                    this[recognition.title] = 1
                }

            }
        }

        var hasDiff = false
        if (Math.abs(newMap.size - lastMap!!.size) <= 1) {
            if (newMap.size >= lastMap!!.size) {
                lastMap!!.forEach {
                    if (lastMap!![it.key] != it.value) {
                        hasDiff =true
                    }
                }
            } else  {
                newMap.forEach {
                    if (lastMap!![it.key] != it.value) {
                        hasDiff =true
                    }
                }
            }
        } else {
            hasDiff = true
        }


        if (hasDiff) {
            lastMap = newMap
        }
        return hasDiff

    }

    private fun recognitionsToSpeech(recognitions: ArrayList<Classifier.Recognition>?, centerX : Int, centerY: Int) {
        var speechText = ""
        recognitions?.forEach { item ->
            Log.d("fzc", "item: id = ${item.id} confidence = ${item.confidence} title = ${item.title} location: item.location.left = ${item.location.left}  item.location.right = ${item.location.right} item.location.bottom = ${item.location.bottom} item.location.top = ${item.location.top} detectedClass = ${item.detectedClass}")
            var locate = ""
            locate += if (item.location.left >= centerX) {
                "右"
            } else if(item.location.right <= centerX) {
                "左"
            } else {
                "中间"
            }

            locate += if (item.location.bottom <= centerY) {
                "上方"
            } else if (item.location.top >= centerY) {
                "下方"
            } else if (!locate.equals("中间")) {
                "中"
            } else {
                "方"
            }
            speechText += locate + "有" + item.title + ";"
        }
        Log.d("fzc", speechText)
        textToSpeech?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun textToSpeechDestory() {
        textToSpeechHasReady = false
        textToSpeech?.shutdown()
    }

    override fun onResume() {
        super.onResume()
        initTextToSpeech()
    }

    override fun onPause() {
        super.onPause()
        textToSpeechDestory()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        textToSpeechDestory()
    }

}