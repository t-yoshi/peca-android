package org.peercast.core.tv

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import org.peercast.core.lib.rpc.YpChannel
import kotlin.math.roundToInt

class CardPresenter : Presenter() {

    @ColorRes
    var selectedColorRes = R.color.default_selected_background

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }

            init {
                isFocusable = true
                isFocusableInTouchMode = true
            }
        }
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val ch = item as YpChannel
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = HtmlCompat.fromHtml(ch.name, 0)
        cardView.contentText = ch.contentText
        cardView.setMainImageAdjustViewBounds(true)
        if (ch.channelId != NULL_ID) {
            val d = TextDrawable(cardView.context)
            d.text = ch.name.take(3)
            cardView.mainImage = d
        }

        val res = cardView.context.resources
        cardView.setMainImageDimensions(
            res.getDimensionPixelSize(R.dimen.tv_card_width),
            res.getDimensionPixelSize(R.dimen.tv_card_height)
        )
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        //Timber.d("onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val r = when (selected) {
            true -> selectedColorRes
            else -> R.color.default_background
        }
        val color = ContextCompat.getColor(view.context, r)
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }


    companion object {
        private val RE_REPLACE = """( - |&lt;(Free|Open|Over)&gt;|\s+)""".toRegex()

        private val YpChannel.contentText: CharSequence
            get() {
                return HtmlCompat.fromHtml(
                    "$genre $description $comment".replace(RE_REPLACE, " "),
                    0
                )
            }
    }
}

internal fun convertDpToPixel(context: Context, dp: Int): Int {
    val density = context.applicationContext.resources.displayMetrics.density
    return (dp.toFloat() * density).roundToInt()
}

internal const val NULL_ID = "00000000000000000000000000000000"