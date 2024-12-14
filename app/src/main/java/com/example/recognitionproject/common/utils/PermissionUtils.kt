package com.example.recognitionproject.common.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * 仅方便调用 checkSelfPermission 和 requestPermissions
 * 不要忘记在调用的 Activity 或者 Fragment 中重写 onRequestPermissionsResult() 用于接收回调
 */

object PermissionUtils {

    fun getPermission(context : Context, permission : String, requestCode : Int) : Boolean{
        if (checkPermission(context, permission)) return true

        //没有权限，向用户请求权限
        if (context is Fragment) {
            context.requestPermissions(arrayOf(permission), requestCode)
        }
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(permission),
                requestCode
            )
        }

        return false
    }

    fun getPermission(context : Context, permissions : MutableList<String>, requestCode : Int) : Boolean {
        return getPermission(context, permissions.toTypedArray(), requestCode)
    }

    fun getPermission(context : Context, permissions : Array<String>, requestCode : Int) : Boolean {
        if (checkPermission(context, permissions)) return true

        //没有权限，向用户请求权限
        if (context is Fragment) {
            context.requestPermissions(permissions, requestCode)
        }
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                permissions,
                requestCode
            )
        }
        return false
    }

    fun checkPermission(context : Context, permission : String) : Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(context, permission)
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            //拥有权限
            return true
        }
        return false
    }

    fun checkPermission(context : Context, permissions : MutableList<String>) : Boolean {
        return checkPermission(context, permissions.toTypedArray())
    }

    fun checkPermission(context : Context, permissions : Array<String>) : Boolean {
        var hasAllPermission = true
        permissions.forEachIndexed { _, permission ->
            if (hasAllPermission && ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                hasAllPermission = false
            }
        }
        return hasAllPermission
    }

}