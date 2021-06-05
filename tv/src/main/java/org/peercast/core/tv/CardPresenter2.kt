package org.peercast.core.tv

import android.content.Context
import android.graphics.Typeface
import android.text.Layout
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class CardPresenter2 : Presenter() {
    //private var mDefaultCardImage: Drawable? = null
    //private var sSelectedBackgroundColor: Int by Delegates.notNull()
    //private var sDefaultBackgroundColor: Int by Delegates.notNull()

    @ColorRes
    var selectedColorRes = R.color.default_selected_background


    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        Timber.d("onCreateViewHolder")

//        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        //sSelectedBackgroundColor =
        //    ContextCompat.getColor(parent.context, R.color.selected_background)
//        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)

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
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val ch = item as YpChannel
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = HtmlCompat.fromHtml(ch.name, 0)
        cardView.contentText = ch.contentText
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
        Timber.d("onUnbindViewHolder")
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
        private const val CARD_WIDTH = 192 //313
        private const val CARD_HEIGHT = 96 //176

        private val RE_REPLACE = """( - |&lt;(Free|Open)&gt;|\s+)""".toRegex()

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