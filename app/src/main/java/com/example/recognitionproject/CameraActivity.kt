package com.example.recognitionproject

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.recognitionproject.activity.BaseActivity
import com.example.recognitionproject.common.ui.LButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : BaseActivity() {

    private var viewFinder : PreviewView? = null
    private var lbtnBack : LButton? = null
    private var cameraExecutor: ExecutorService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        cameraExecutor = Executors.newSingleThreadExecutor()
        initView()
        startCamera()
    }

    private fun initView() {
        viewFinder = findViewById(R.id.viewFinder)
        lbtnBack = findViewById(R.id.lbtn_camera_back)

        lbtnBack?.setOnClickListener {
            finish()
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
            // Preview
            // 初始化 Preview 对象，在其上调用 build，从取景器中获取 Surface 提供程序，然后在预览上进行设置
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder?.surfaceProvider)
                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    cameraExecutor?.let { executor ->
                        it.setAnalyzer(executor, CameraAnalyzer {
                            // 传入参数为摄像头 bitmap

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
                    this, cameraSelector, preview, imageAnalyzer)

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
            return adjustPhotoRotation(bitmap, 90f)
        }

        // 旋转 bitmap
        private fun adjustPhotoRotation(bm: Bitmap, orientationDegree: Float): Bitmap? {
            val m = Matrix()
            m.setRotate(orientationDegree, bm.width.toFloat() / 2, bm.height.toFloat() / 2)
            try {
                return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
            } catch (ex: OutOfMemoryError) {
            }
            return null
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }

}