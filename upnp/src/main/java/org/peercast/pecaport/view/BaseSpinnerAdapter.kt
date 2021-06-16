package org.peercast.pecaport.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.LayoutRes

abstract class BaseSpinnerAdapter<T>(
        @LayoutRes private val layout: Int,
        @LayoutRes private val dropDownLayout: Int = layout
) : BaseAdapter() {

    var items: List<T> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView?:kotlin.run {
            val inflater = LayoutInflater.from(parent.context)
            inflater.inflate(layout, parent, false)
        }
        bindView(v, position)
        return v
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView?:kotlin.run {
            val inflater = LayoutInflater.from(parent.context)
            inflater.inflate(dropDownLayout, parent, false)
        }
        bindView(v, position)
        return v
    }

    abstract fun bindView(view: View, position: Int)

    final override fun getItem(position: Int): T = items[position]

    override fun getItemId(position: Int): Long = 0L

    final override fun getCount(): Int = items.size

}

