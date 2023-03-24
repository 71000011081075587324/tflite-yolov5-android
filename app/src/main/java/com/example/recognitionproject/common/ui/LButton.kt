package com.example.recognitionproject.common.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.example.recognitionproject.common.utils.dp

/**
 * Created by fangzicheng on 3/22/23
 * @author fangzicheng@bytedance.com
 */
class LButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) :
    AppCompatButton(context, attrs) {

    init {
        initCheckViewParam()
    }


    private fun initCheckViewParam() {
        if (paddingLeft + paddingRight + minWidth < AccessibleValue.MIN_WIDTH_DP.dp(context)) {
            this.minWidth = AccessibleValue.MIN_WIDTH_DP.dp(context) - paddingLeft - paddingRight
        }
        if (paddingTop + paddingBottom + minHeight < AccessibleValue.MIN_HEIGHT_DP.dp(context)) {
            this.minHeight = AccessibleValue.MIN_HEIGHT_DP.dp(context) - paddingTop - paddingBottom
        }

        if (paddingLeft + paddingRight + minimumWidth < AccessibleValue.MIN_WIDTH_DP.dp(context)) {
            this.minimumWidth = AccessibleValue.MIN_WIDTH_DP.dp(context) - paddingLeft - paddingRight
        }
        if (paddingTop + paddingBottom + minimumHeight < AccessibleValue.MIN_HEIGHT_DP.dp(context)) {
            this.minimumHeight = AccessibleValue.MIN_HEIGHT_DP.dp(context) - paddingTop - paddingBottom
        }
    }

}