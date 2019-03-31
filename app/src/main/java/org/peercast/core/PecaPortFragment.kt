package org.peercast.core

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Switch
import org.koin.android.ext.android.inject
import org.peercast.pecaport.PecaPortFragmentBase


/**
 * * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class PecaPortFragment : PecaPortFragmentBase()  {
    private val appPrefs by inject<AppPreferences> ()
    private val activity: PeerCastActivity
        get() = super.getActivity() as PeerCastActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity.actionBar?.let { ab->
            ab.setDisplayHomeAsUpEnabled(true)
            ab.setTitle(R.string.t_pecaport)
        }

        if (appPrefs.isUPnPEnabled)
            startDiscoverer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isEnabled = appPrefs.isUPnPEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_close_on_exit -> {
                appPrefs.isUPnPCloseOnExit = !item.isChecked
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val enabled = appPrefs.isUPnPEnabled

        with(menu.findItem(R.id.menu_enabled).actionView as Switch){
            setOnCheckedChangeListener(null)
            isChecked = enabled
            setOnCheckedChangeListener { _, isChecked ->
                view!!.isEnabled = isChecked
                appPrefs.isUPnPEnabled = isChecked
                if (isChecked)
                    startDiscoverer()
            }
        }

        //menu.findItem(R.id.menu_debug).isChecked = isDebugMode
        menu.findItem(R.id.menu_close_on_exit).isChecked = appPrefs.isUPnPCloseOnExit

        0.until(menu.size()).map { menu.getItem(it) }.forEach {
            if (it.itemId != R.id.menu_enabled) {
                it.isEnabled = enabled
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.pecaport_menu, menu)
    }

}
