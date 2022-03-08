package eu.h2020.helios_social.heliostestclient.ui.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.ListPreference;

import android.util.Log;

import eu.h2020.helios_social.core.profile.HeliosUserData;
import eu.h2020.helios_social.heliostestclient.R;

/**
 * Fragment to show all settings related to the application/user profile.
 */
public class NotifSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "NotifSettingsFragment";

    private void createNotifPreference(String prefix) {
        // Icon
        String iconRes = prefix + "_icon";
        ListPreference prefIcon = (ListPreference) findPreference(iconRes);
        if (prefIcon != null) {
            prefIcon.setOnPreferenceChangeListener(this);
            String entry = prefIcon.getEntry().toString();
            Log.d(TAG, iconRes + " oncreate " + entry);
            prefIcon.setSummary(entry);
        }
        String colorRes = prefix + "_color";
        ListPreference prefColor = (ListPreference) findPreference(colorRes);
        if (prefColor != null) {
            prefColor.setOnPreferenceChangeListener(this);
            String entry = prefColor.getEntry().toString();
            Log.d(TAG, colorRes + " oncreate " + entry);
            prefColor.setSummary(entry);
        }
        String intervalRes = prefix + "_interval";
        ListPreference prefInterval = (ListPreference) findPreference(intervalRes);
        if (prefInterval != null) {
            prefInterval.setOnPreferenceChangeListener(this);
            String entry = prefInterval.getEntry().toString();
            Log.d(TAG, intervalRes + " oncreate " + entry);
            prefInterval.setSummary(entry);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.notifsettings, rootKey);
        createNotifPreference("vhi");
        createNotifPreference("hi");
        createNotifPreference("mi");
        createNotifPreference("li");
        createNotifPreference("vli");
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        // Log.d(TAG, "preferencesName username is " + o.toString());
        // Accept change, should validate it.
        String prefKey = preference.getKey();
        if (!prefKey.contains("_"))
            return false;
        String[] pkParts = prefKey.split("_", 2);
        if (pkParts[1].equals("icon")) {
            Log.d(TAG, prefKey);
            ListPreference pref = (ListPreference) findPreference(prefKey);
            int index = Integer.parseInt(o.toString());
            String entry = pref.getEntries()[index].toString();
            pref.setSummary(entry);
            Log.d(TAG, prefKey + " is now " + entry);
            HeliosUserData.getInstance().setValue(pkParts[0] + "_icon", "" + index);
        }
        else if (pkParts[1].equals("color")) {
            Log.d(TAG, prefKey);
            ListPreference pref = (ListPreference) findPreference(prefKey);
            int index = Integer.parseInt(o.toString());
            String entry = pref.getEntries()[index].toString();
            pref.setSummary(entry);
            Log.d(TAG, prefKey + " is now " + entry);
            HeliosUserData.getInstance().setValue(pkParts[0] + "_icon", "" + index);
        }
        else if (pkParts[1].equals("interval")) {
            Log.d(TAG, prefKey);
            ListPreference pref = (ListPreference) findPreference(prefKey);
            int index = Integer.parseInt(o.toString());
            String entry = pref.getEntries()[index].toString();
            pref.setSummary(entry);
            Log.d(TAG, prefKey + " is now " + entry);
            HeliosUserData.getInstance().setValue(pkParts[0] + "_color", "" + index);
        }
        // TODO:...
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.hasKey()) {
            String key = preference.getKey();
            boolean ret = true;
            switch (key) {
                // TODO:
                default:
                    Log.e(TAG, "key " + key + " not recognized");
                    return false;
            }
            // return ret;
        } else {
            Log.d(TAG, "No key bound to a preference");
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO?
    }
}
