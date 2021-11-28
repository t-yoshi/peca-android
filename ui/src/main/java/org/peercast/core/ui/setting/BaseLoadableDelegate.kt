package org.peercast.core.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.peercast.core.common.AppPreferences
import org.peercast.core.ui.R
import timber.log.Timber
import java.io.IOException

internal abstract class BaseLoadableDelegate(protected val fragment: PreferenceFragmentCompat) {
    protected val appPrefs by fragment.inject<AppPreferences>()
    private val isBusy = MutableStateFlow(false)

    init {
        //TV設定画面で
        if (fragment is LeanbackPreferenceFragmentCompat) {
            fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    //戻るボタンのイベントを拾うため
                    (fragment.view as ViewGroup)[0].requestFocus()
                }
            })
        }
    }

    @CallSuper
    open fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        fragment.preferenceScreen =
            fragment.preferenceManager.createPreferenceScreen(fragment.requireContext())
        isBusy.onEach {
            fragment.preferenceScreen.isEnabled = !it
        }.launchIn(fragment.lifecycleScope)
    }

    fun wrapProgressView(container: View): ViewGroup {
        val inflater = LayoutInflater.from(container.context)
        val v = FrameLayout(inflater.context)
        v.addView(container)

        val vProgress = inflater.inflate(R.layout.progress, v, false)
        v.addView(vProgress)
        isBusy.onEach {
            vProgress.isVisible = it
        }.launchIn(fragment.viewLifecycleOwner.lifecycleScope)

        return v
    }

    protected fun <T> asyncExecute(
        doInBackground: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onFailure: (IOException) -> Unit = { Timber.w(it) }
    ) {
        isBusy.value = true

        fragment.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                kotlin.runCatching { doInBackground() }
            }
                .onSuccess(onSuccess)
                .onFailure { e ->
                    when (e) {
                        is IOException -> onFailure(e)
                        else -> throw e
                    }
                }
            isBusy.value = false
        }
    }
}