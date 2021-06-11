package org.peercast.core.tv

import androidx.fragment.app.FragmentManager
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import org.peercast.core.lib.rpc.YpChannel

internal class CardEventHandler(private val manager: FragmentManager) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder,
        row: Row,
    ) {
        when (item) {
            is YpChannel -> {
                DetailsFragment.start(manager, item)
            }
            R.drawable.ic_baseline_refresh_64 -> {
                LoadingFragment.start(manager, true, isForceReload = true)
            }
            R.drawable.ic_baseline_open_in_browser_64 -> {

            }
            R.drawable.ic_baseline_settings_64 -> {
                SettingFragment.start(manager)
            }
        }
    }
}