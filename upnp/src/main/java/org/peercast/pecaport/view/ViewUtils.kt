package org.peercast.pecaport.view

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

typealias SimpleSpinnerItemSelectedListener = Spinner.(position: Int, id: Long) -> Unit

fun Spinner.onItemSelectedListener(listener: SimpleSpinnerItemSelectedListener) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            listener(position, id)
        }
    }
}