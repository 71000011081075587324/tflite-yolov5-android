package com.example.recognitionproject.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.recognitionproject.R

/**
 * Created by fangzicheng
 */
open class BaseActivity: AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置状态栏文字颜色
        val wic = WindowCompat.getInsetsController(window, window.decorView);
        if (wic != null) {
            // true表示Light Mode，状态栏字体呈黑色，反之呈白色
            wic.isAppearanceLightStatusBars = true;
        }
    }


}