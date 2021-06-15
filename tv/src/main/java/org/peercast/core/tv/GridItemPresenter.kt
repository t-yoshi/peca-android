package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter

class GridItemPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val frame = object : FrameLayout(inflater.context) {
            init {
                isFocusable = true
                isFocusableInTouchMode = true
                updateGridItemBackgroundColor(this, false)
            }
            override fun setSelected(selected: Boolean) {
                super.setSelected(selected)
                updateGridItemBackgroundColor(this, selected)
            }
        }
        val view = inflater.inflate(R.layout.grid_item, parent, false) as ImageView
        frame.tag = view
        frame.addView(view)
        return ViewHolder(frame)
    }

    private fun updateGridItemBackgroundColor(view: FrameLayout, selected: Boolean){
        val r = when (selected) {
            true -> R.color.default_selected_background
            else -> R.color.default_background
        }
        val color = ContextCompat.getColor(view.context, r)
        view.setBackgroundColor(color)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        (viewHolder.view.tag as ImageView)
            .setImageResource(item as Int)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        (viewHolder.view.tag as ImageView).setImageDrawable(null)
    }
}