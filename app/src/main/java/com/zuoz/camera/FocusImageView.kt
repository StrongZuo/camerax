package com.zuoz.camera

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout

/**

 * @Description:
 * @Author:         zuoz
 * @CreateDate:     2020/11/5 20:10
 */
class FocusImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet) :
    AppCompatImageView(context, attrs) {

    companion object {
        private val NO_ID = -1
    }

    private lateinit var mHandler: Handler

    private var mFocusImg = NO_ID
    private var mFocusSucceedImg = NO_ID
    private var mFocusFailedImg = NO_ID

    init {

        visibility = View.GONE
        mHandler = Handler()
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.FocusImageView)

        mFocusImg = typedArray.getResourceId(
            R.styleable.FocusImageView_focus_focusing_id,
            NO_ID
        )
        mFocusSucceedImg = typedArray.getResourceId(
            R.styleable.FocusImageView_focus_success_id,
            NO_ID
        )
        mFocusFailedImg =
            typedArray.getResourceId(R.styleable.FocusImageView_focus_fail_id, NO_ID)

    }

    /**
     * 显示对焦图案
     */
    fun startFocus(point: Point) {

        //根据触摸的坐标设置聚焦图案的位置
        val params = layoutParams as ConstraintLayout.LayoutParams
        params.topMargin = point.y - measuredHeight / 2
        params.leftMargin = point.x - measuredWidth / 2
        layoutParams = params
        //设置控件可见，并开始动画
        visibility = View.VISIBLE
        setImageResource(mFocusImg)

    }

    /**
     * 聚焦成功回调
     */
    fun onFocusSuccess() {
        setImageResource(mFocusSucceedImg)
        //移除在startFocus中设置的callback，1秒后隐藏该控件
//        mHandler.removeCallbacks(null, null)
        mHandler.postDelayed(Runnable { visibility = View.GONE }, 1000)
    }

    /**
     * 聚焦失败回调
     */
    fun onFocusFailed() {
        setImageResource(mFocusFailedImg)
        //移除在startFocus中设置的callback，1秒后隐藏该控件
//        mHandler.removeCallbacks(null, null)
        mHandler.postDelayed(Runnable { visibility = View.GONE }, 1000)
    }

}