package com.example.recognitionproject.common.utils

import android.content.Context
import android.util.TypedValue


/**
 * Created by fangzicheng
 */
object UnitUtils {
    fun dp2px(context : Context, dpValue : Float) : Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dp(context: Context, pxValue : Float) : Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun pt2px(context: Context, ptValue : Float) : Int {
        // 获取当前手机的像素密度（1个pt对应多少个px）
        val scale = context.resources.displayMetrics.xdpi
        return (ptValue * scale / 72 + 0.5f).toInt() // 四舍五入取整
    }

    fun  px2pt(context: Context, pxValue : Float) : Int {
        // 获取当前手机的像素密度（1个pt对应多少个px）
        val scale = context.resources.displayMetrics.xdpi
        return (pxValue * 72 / scale + 0.5f).toInt() // 四舍五入取整
    }

    fun sp2px(context: Context, spValue : Float) : Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.resources.displayMetrics).toInt()
    }


}

// dp 转换为 px, 返回类型为 Float
fun Number.dp(context : Context) : Int {
    return UnitUtils.dp2px(context, this.toFloat())
}

// dp 转换为 px，返回类型为 Float
fun Number.dpFloat(context : Context) : Float{
    return this.dp(context).toFloat()
}

// pt 转换为 px, 返回类型为 Float
fun Number.pt(context : Context) : Int {
    return UnitUtils.pt2px(context, this.toFloat())
}

// pt 转换为 px, 返回类型为 Float
fun Number.ptFloat(context : Context) : Float {
    return this.pt(context).toFloat()
}

// sp 转换为 px, 返回类型为 Float
fun Number.sp(context : Context) : Int {
    return UnitUtils.sp2px(context, this.toFloat())
}

// sp 转换为 px, 返回类型为 Float
fun Number.spFloat(context : Context) : Float {
    return this.sp(context).toFloat()
}