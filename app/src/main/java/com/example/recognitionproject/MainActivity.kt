package com.example.recognitionproject

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.recognitionproject.activity.BaseActivity
import com.example.recognitionproject.common.ui.LButton
import com.example.recognitionproject.common.utils.PermissionUtils

class MainActivity : BaseActivity() {

    private var lbtnCameraStart : LButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        lbtnCameraStart = findViewById(R.id.lbtn_camera_start)
        lbtnCameraStart?.setOnClickListener {
            if (PermissionUtils.getPermission(context = this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)) {
                startCameraActivity()
            }
        }
    }

    private fun startCameraActivity() {
        startActivity(Intent(this, CameraActivity::class.java))
        Log.d("fzc", "startCamera()")
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("fzc", "onRequestPermissionsResult    requestCode = $requestCode")
        when(requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (PermissionUtils.checkPermission(this, REQUIRED_PERMISSIONS)) {
                    startCameraActivity()
                } else {
                    Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}