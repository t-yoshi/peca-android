package org.peercast.pecaport.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.children
import org.fourthline.cling.support.model.PortMapping
import org.peercast.pecaport.R
import org.peercast.pecaport.databinding.PortmappingRowBinding

/**
 *  列のインデックス
    ICON = 0
    CLIENT = 1
    PORT = 2
    PROTO = 3
    DESC = 4
    DURATION = 5
    ENABLED = 6
    REMOVE = 7
 *
 * */
class PortMappingTable : TableLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private val inflater = LayoutInflater.from(context)

    init {
        val header = inflater.inflate(R.layout.portmapping_header_row, this, false) as TableRow
        header.children.asIterable().zip(
                context.resources.getStringArray(R.array.wan_table_headers)
        ).forEach { (v, s) ->
            (v as TextView).text = s
        }
        addView(header)
    }

    fun setPortMappings(mappings: List<PortMapping>, onBind: (PortmappingRowBinding, PortMapping) -> Unit) {
        if (childCount > 1)
            removeViews(1, childCount - 1)

        mappings.forEach { m ->
            val b = PortmappingRowBinding.inflate(inflater, this, false).also {
                it.vm = PortMappingViewModel()
            }
            onBind(b, m)
            b.executePendingBindings()
            addView(b.root)
        }
    }
}