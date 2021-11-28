package org.peercast.core.ui.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.*
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
            viewModel.toastMessageFlow.collect { msg ->
                Toast.makeText(this@TvActivity, msg, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.bindService()
    }

}