package com.example.recognitionproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.recognitionproject.common.ui.LButton

class MainActivity : AppCompatActivity() {
    var ltbnCameraStart : LButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 设置状态栏文字颜色
        val wic = WindowCompat.getInsetsController(window, window.decorView);
        if (wic != null) {
            // true表示Light Mode，状态栏字体呈黑色，反之呈白色
            wic.isAppearanceLightStatusBars = true;
        }

        initView()
    }

    private fun initView() {
        ltbnCameraStart = findViewById(R.id.lbtn_camera_start)

        ltbnCameraStart?.setOnClickListener {
        }
    }
}