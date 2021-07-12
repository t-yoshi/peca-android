package org.peercast.core.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.tv.util.TextDrawable
import org.peercast.core.tv.yp.Bookmark
import timber.log.Timber

class CardPresenter(
    @ColorRes private val selectedColorRes: Int,
) : Presenter(), KoinComponent {
    private val bookmark by inject<Bookmark>()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
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

        cardView.titleText = ch.name.unescapeHtml()
        cardView.contentText = ch.contentText
        cardView.setMainImageAdjustViewBounds(true)
        val c = cardView.context

        if (ch.isNotNilId) {
            val titleImage = TextDrawable(c)
            titleImage.text = ch.name.take(3)
            cardView.mainImage = titleImage

            if (ch in bookmark) {
                cardView.badgeImage =
                    ContextCompat.getDrawable(c, R.drawable.ic_baseline_bookmark_border_36)
            }

            cardView.setOnKeyListener { _, keyCode, event ->
                //Timber.d("keyCode=$keyCode, event=$event")
                if (keyCode == KeyEvent.KEYCODE_BOOKMARK && event.action == KeyEvent.ACTION_UP) {
                    Timber.d("bookmark: $ch")
                    bookmark.toggle(ch)
                    true
                } else {
                    false
                }
            }
        }

        cardView.setMainImageDimensions(
            c.resources.getDimensionPixelSize(R.dimen.tv_card_width),
            c.resources.getDimensionPixelSize(R.dimen.tv_card_height)
        )
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        with(viewHolder.view as ImageCardView) {
            badgeImage = null
            mainImage = null
        }
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
                return "$genre $description $comment".replace(RE_REPLACE, " ").unescapeHtml()
            }
    }
}

