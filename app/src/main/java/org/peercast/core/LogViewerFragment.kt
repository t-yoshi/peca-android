package org.peercast.core

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.peercast.core.databinding.LogviewerLineBinding
import org.peercast.pecaport.PecaPort
import kotlin.coroutines.CoroutineContext

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class LogViewerFragment : Fragment(), CoroutineScope, PeerCastActivity.NestedScrollFragment {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var job: Job
    private val adapter = LogAdapter()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        doLogParse()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        job = Job()
        val c = inflater.context
        return RecyclerView(c).also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(c)
            it.addItemDecoration(DividerItemDecoration(c, DividerItemDecoration.VERTICAL))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
        val logFile = PecaPort.getLogFile(requireContext())
        if (!logFile.isFile)
            return@launch

        val s = logFile.reader().readText()
        adapter.records = RE_LOG_LINE.findAll(s).map { r ->
            r.groupValues.let {
                LogRecord(it[1], it[3], it[4], decorateMessage(it[5]))
            }
        }.toList()
    }

    private class ViewHolder(private val binding: LogviewerLineBinding)
        : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.vMessage.movementMethod = LinkMovementMethod.getInstance()
        }

        fun bind(r: LogRecord){
            binding.r = r
            binding.executePendingBindings()
        }
    }

    private class LogAdapter : RecyclerView.Adapter<ViewHolder>() {
        var records = emptyList<LogRecord>()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount(): Int  = records.size

        fun getItem(position: Int): LogRecord {
            //return records[position]
            return records[itemCount - position - 1]
        }

        override fun getItemId(position: Int): Long = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(
                    LogviewerLineBinding.inflate(inflater, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
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
        private val RE_LOG_LINE = """^([\d:.]+)(.*?)\s+([A-Z]+)\s+(\S+)\s+- (.+?)$""".toRegex(
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )
        private val RE_URL = """https?://[^,\s]+""".toRegex()
    }
}
