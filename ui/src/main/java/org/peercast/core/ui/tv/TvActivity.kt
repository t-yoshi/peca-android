package org.peercast.core.ui.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class TvActivity : FragmentActivity() {
    private val viewModel by viewModel<TvViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, BrowseFragment())
                .commitNow()
        }

        lifecycleScope.launch {
            //5秒間隔をあけて表示
            val lines = StringBuilder(128)
            var j: Job? = null
            viewModel.toastMessageFlow.collect { msg ->
                lines.appendLine(msg)
                if (j?.isActive != true) {
                    j = launch {
                        while (lines.isNotEmpty() && isActive) {
                            Toast.makeText(this@TvActivity, lines.trimEnd(), Toast.LENGTH_LONG)
                                .show()
                            lines.clear()
                            delay(5_000)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService()
    }

}