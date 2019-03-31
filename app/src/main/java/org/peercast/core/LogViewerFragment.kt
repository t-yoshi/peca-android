package org.peercast.core

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.*
import android.widget.BaseAdapter
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.ListFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.peercast.core.databinding.LogviewerLineBinding
import org.peercast.pecaport.PecaPort
import kotlin.coroutines.CoroutineContext

/**
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class LogViewerFragment : ListFragment(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var job: Job
    private val adapter = LogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        job = Job()
        listAdapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.let { ab ->
            ab.setDisplayHomeAsUpEnabled(true)
            ab.setTitle(R.string.t_view_log)
        }

        doLogParse()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    data class LogRecord(
            val time: CharSequence,
            val level: CharSequence,
            val source: CharSequence,
            val message: CharSequence
    ) {
        @ColorInt
        fun getMessageColor(c: Context): Int {
            return when {
                level == "ERROR" -> ContextCompat.getColor(c, R.color.md_red_800)
                level == "WARN" -> ContextCompat.getColor(c, R.color.md_orange_800)
                source.startsWith("org.peercast") -> ContextCompat.getColor(c, R.color.md_green_800)
                else -> ContextCompat.getColor(c, R.color.md_grey_800)
            }
        }
    }

    private fun doLogParse() = launch {
        if (!PecaPort.logFile.isFile)
            return@launch

        val s = PecaPort.logFile.reader().readText()
        adapter.records = RE_LOG_LINE.findAll(s).map { r ->
            r.groupValues.let {
                LogRecord(it[1], it[3], it[4], decorateMessage(it[5]))
            }
        }.toList()
    }

    private class LogAdapter : BaseAdapter() {
        var records = emptyList<LogRecord>()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getCount(): Int = records.size

        override fun getItem(position: Int): LogRecord {
            //return records[position]
            return records[count - position - 1]
        }

        override fun getItemId(position: Int): Long = 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val b = convertView?.let {
                DataBindingUtil.getBinding<LogviewerLineBinding>(it)
            } ?: kotlin.run {
                val inflater = LayoutInflater.from(parent.context)
                LogviewerLineBinding.inflate(inflater, parent, false).also {
                    it.vMessage.movementMethod = LinkMovementMethod.getInstance()
                }
            }
            b.r = getItem(position)
            b.executePendingBindings()
            return b.root
        }
    }

    private fun decorateMessage(s: String): CharSequence {
        return SpannableStringBuilder(s).also { ssb ->
            RE_URL.findAll(s).forEach { r ->
                val start = r.range.start
                val end = r.range.endInclusive + 1
                ssb.setSpan(URLSpan(s.substring(start, end)), start, end, 0)
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.logviewer_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reload -> {
                doLogParse()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val TAG = "LogViewerFragment"

        private val RE_LOG_LINE = """^([\d:.]+)(.*?)\s+([A-Z]+)\s+(\S+)\s+- (.+?)$""".toRegex(
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )
        private val RE_URL = """https?://[^,\s]+""".toRegex()
    }
}
