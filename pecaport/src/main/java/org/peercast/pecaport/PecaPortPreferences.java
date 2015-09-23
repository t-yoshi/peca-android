package org.peercast.pecaport;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class PecaPortPreferences {
    private static final String TAG = "PecaPortPreferences";
    private final SharedPreferences mPreferences;
    private static final String PREF_DISABLED_NETWORKS = "pref_upnp_disabled_networks";


    private PecaPortPreferences(Context c) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(c);
    }

    public static PecaPortPreferences from(Context c){
        return new PecaPortPreferences(c);
    }

    public void addDisabledNetwork(@NonNull NetworkIdentity identity){
        //Log.d(TAG, "addDisabledNetwork -> " + identity);
        Set<String> disabled = getNewStringSet(PREF_DISABLED_NETWORKS);
        disabled.add(identity.toString());
        mPreferences.edit().putStringSet(PREF_DISABLED_NETWORKS, disabled).apply();
    }

    public void removeDisabledNetwork(@NonNull NetworkIdentity identity){
        Set<String> disabled = getNewStringSet(PREF_DISABLED_NETWORKS);
        disabled.remove(identity.toString());
        mPreferences.edit().putStringSet(PREF_DISABLED_NETWORKS, disabled).apply();
    }

    private Set<String> getNewStringSet(String key){
        return new HashSet<>(
                mPreferences.getStringSet(key,
                        Collections.<String>emptySet()));
    }

    public Collection<NetworkIdentity> getAllDisabledNetworks(){
        Set<String> disabled = getNewStringSet(PREF_DISABLED_NETWORKS);
        return CollectionUtils.collect(disabled, new Transformer<String, NetworkIdentity>() {
            @Override
            public NetworkIdentity transform(String input) {
                return new NetworkIdentity(input);
            }
        });
    }


}
