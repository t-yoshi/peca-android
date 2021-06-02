package org.peercast.core.tv

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.leanback.widget.BaseCardView

open class TextCardView : BaseCardView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val vTitle : TextView

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.text_card_view, this, true)
        vTitle = findViewById(android.R.id.title)
    }


    var titleText: CharSequence?
        set(value) {
            vTitle.text = value
        }
        get() = vTitle.text
}