package org.peercast.pecaport.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**Disable時に半透明になり、イベントを子に渡さないFrameLayout*/
class ContainerFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = when (enabled) {
            true -> 1f
            else -> 0.5f
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return !isEnabled
    }
}