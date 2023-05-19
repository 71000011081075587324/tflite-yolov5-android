package com.example.recognitionproject.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.tflite_yolov5.classifier.Classifier


/**
 * Created by fangzicheng on 5/14/23
 * @author fangzicheng@bytedance.com




 */
class RecognitionSurfaceView(context: Context?, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var mHolder: SurfaceHolder? = null

    //用于绘图的canvas
    private var canvas: Canvas? = null
    private var paint: Paint? = null
    private var cameraBitmap : Bitmap? = null

    //子线程标志位
    private var mIsDrawing: Boolean = false

    @Volatile
    private var lastRecognitions : ArrayList<Classifier.Recognition>? = null

    @Volatile
    private var newRecognitions : ArrayList<Classifier.Recognition>? = null


    init {
        initHolder()
        initView()
        initPaint()
    }

    private fun initHolder() {
        mHolder = getHolder()
        mHolder?.addCallback(this)
    }

    private fun initView() {
        // 指定View是否可以获得焦点
        setFocusable(false)
        setFocusableInTouchMode(false)
    }

    private fun initPaint() {
        if (paint == null) {
            paint = Paint()

            // 初始化画笔
            paint?.color = Color.RED                // 长方形框的颜色
            paint?.strokeWidth = 10f                 // 长方形框的边框宽度
            paint?.style = Paint.Style.STROKE       // 长方形框的样式
            paint?.isAntiAlias = true               // 是否开启抗锯齿
        }
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        mIsDrawing = true
        // 开启线程
        Thread(this).start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mIsDrawing = false
    }

    // 线程执行操作
    override fun run() {
        while (mIsDrawing) {
            if (lastRecognitions == newRecognitions) {
                // 暂停当前正在执行的线程对象 ，并去执行其他线程
                Thread.yield()
            }
            lastRecognitions = newRecognitions
            draw()
        }
    }

    private fun draw() {
        try {
            // 这里需要注意，获取到的 Canvas 对象还是继续上次的 Canvas 对象，而不是一个新的 Canvas 对象
            // 因此，之前的绘图操作都会被保留。如果需要擦除，则可以在绘制前，通过 drawColor() 方法来进行清屏操作
            canvas = mHolder?.lockCanvas()
//            canvas?.drawColor(Color.TRANSPARENT)
            initPaint()
            Log.d("fzc", "fzc draw")
//            canvas?.setBitmap(cameraBitmap)

            var finalScale = 1f

            cameraBitmap?.let { bitmap ->
                canvas?.let { c ->
                    val heightScale = c.height / bitmap.height.toFloat()
                    val weightScale = c.width / bitmap.width.toFloat()
                    finalScale = if (heightScale > weightScale) weightScale else heightScale
                    val matrix = Matrix().apply {
                        setScale(finalScale, finalScale)
                    }
//                    val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * finalScale).toInt(), (bitmap.height * finalScale).toInt(), false)
                    c.drawBitmap(scaledBitmap, 0f, 0f, null)
                }

//                canvas?.drawBitmap(bitmap, 0f, 0f, null)
            }
            Log.d("fzc", "fzc draw cameraBitmap?.height =  ${cameraBitmap?.height} cameraBitmap?.width =  ${cameraBitmap?.width}")
            Log.d("fzc", "fzc draw canvas?.height =  ${canvas?.height} canvas?.width =  ${canvas?.width}")


            lastRecognitions?.forEach {
                Log.d("fzc", "fzc draw  location = ${it.location}")
                canvas?.drawRect(
                    it.location.left * finalScale,
                    it.location.top * finalScale,
                    it.location.right * finalScale,
                    it.location.bottom * finalScale,
                    paint!!
                )
            }
        } catch (e : Exception) {

        } finally {
            if (null != canvas) {
                mHolder?.unlockCanvasAndPost(canvas)
            }
        }

    }

    fun setNewRecognitions(newRecognitions : ArrayList<Classifier.Recognition>?) {
        this.newRecognitions = newRecognitions
    }

    fun setCameraBitmap(cameraBitmap : Bitmap?) {
        this.cameraBitmap = cameraBitmap
    }

}