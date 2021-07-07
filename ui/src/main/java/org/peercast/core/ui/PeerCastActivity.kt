package org.peercast.core.ui


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.common.isTvMode
import org.peercast.core.tv.TvActivity

/**
 * @author (c) 2014-2020, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class PeerCastActivity : AppCompatActivity() {
    private val viewModel by viewModel<UiViewModel>()

    /**BackPressイベントを受け取るFragment*/
    interface BackPressSupportFragment {
        fun onBackPressed(): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //FIRE TV
        if (isTvMode && intent.action == Intent.ACTION_MAIN) {
            val i = Intent(this, TvActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            finish()
            return
        }

        setContentView(R.layout.peercast_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.vFragContainer, WebViewFragment())
                .commit()
        }
        initActionBar()
        showAppBarIfEnoughHeight()

        lifecycleScope.launchWhenResumed {
            viewModel.notificationMessage.filter { it.isNotBlank() }.collect { msg ->
                Snackbar.make(findViewById(R.id.vContent), msg, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun initActionBar() {
        findViewById<Toolbar>(R.id.vToolbar).let { bar ->
            setSupportActionBar(bar)
            bar.setOnMenuItemClickListener {
                val f = supportFragmentManager.findFragmentById(R.id.vFragContainer)
                f?.onOptionsItemSelected(it) == true || onOptionsItemSelected(it)
            }
        }

        supportActionBar?.run {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    /**ディスプレイの高さに余裕があるとき、AppBarをデフォルトで表示する*/
    fun showAppBarIfEnoughHeight() {
        val vAppBar = findViewById<AppBarLayout>(R.id.vAppBar)
        val expanded = vAppBar.height - vAppBar.bottom == 0
        if (expanded)
            vAppBar.setExpanded(
                resources.getBoolean(R.bool.is_portrait_enough_height)
            )
    }

    override fun onBackPressed() {
        val f = supportFragmentManager.findFragmentById(R.id.vFragContainer)
        if (f is BackPressSupportFragment && f.onBackPressed())
            return
        super.onBackPressed()
    }

}
