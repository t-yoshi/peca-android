package org.peercast.core.tv

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import org.peercast.core.R
import org.peercast.core.lib.rpc.YpChannel
import kotlin.math.roundToInt
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class CardPresenter2 : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        Log.d(TAG, "onCreateViewHolder")

        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor =
            ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)

        //ImageCardView
        val cardView = object : ImageCardView(parent.context) {
            init {
                //cardType = CARD_TYPE_FLAG_TITLE or CARD_TYPE_FLAG_ICON_RIGHT
            }
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }
        }

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return Presenter.ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val ch = item as YpChannel
        val cardView = viewHolder.view as ImageCardView

        //cardView.isEnabled = ch.channelId != NULL_ID
        cardView.titleText = HtmlCompat.fromHtml(ch.name, 0)
        cardView.contentText = HtmlCompat.fromHtml(
            "${ch.genre} ${ch.description} ${ch.comment}".replace("""\s+""".toRegex(), " ")
            ,0
        )
        if (ch.channelId != NULL_ID) {
            cardView.mainImage = TextDrawable2(cardView.context).also {
                for (i in 5 downTo 3) {
                    val s = ch.name.trim().take(i)
                    if (it.measureTextWidth(s) > 15)
                        continue

                }

                it.text = ch.name.trim().take(3)
                it.typeface = Typeface.MONOSPACE
                it.textAlign = Layout.Alignment.ALIGN_NORMAL
                //it.textSize = 5f
            }
        }
//        cardView.mainImageView.setImageResource(R.drawable.ic_baseline_videogame_asset_64)
        cardView.setMainImageAdjustViewBounds(true)
        val c = cardView.context
        cardView.setMainImageDimensions(
            convertDpToPixel(c, CARD_WIDTH),
            convertDpToPixel(c, CARD_HEIGHT)
        )
//            Glide.with(viewHolder.view.context)
//                //.load(movie.cardImageUrl)
//                .centerCrop()
//                .error(mDefaultCardImage)
//                .into(cardView.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }


    companion object {


        private val TAG = "CardPresenter"

        private const val CARD_WIDTH = 192 //313
        private const val CARD_HEIGHT = 96 //176

    }
}

internal fun convertDpToPixel(context: Context, dp: Int): Int {
    val density = context.applicationContext.resources.displayMetrics.density
    return (dp.toFloat() * density).roundToInt()
}

internal const val NULL_ID = "00000000000000000000000000000000"