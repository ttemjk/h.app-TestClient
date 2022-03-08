package eu.h2020.helios_social.heliostestclient.ui.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import eu.h2020.helios_social.heliostestclient.BuildConfig;
import eu.h2020.helios_social.heliostestclient.ui.UiUtils;
import eu.h2020.helios_social.heliostestclient.ui.preferences.PicturePreference;
import eu.h2020.helios_social.core.profile.HeliosUserData;
import eu.h2020.helios_social.heliostestclient.R;

/**
 * Fragment to show all settings related to the application/user profile.
 */
public class MySettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "MySettingsFragment";
    private static final int PICK_IMAGE = 1;
    private PicturePreference prefPicture;

    private void configTextPreference(String key) {
        EditTextPreference pref = (EditTextPreference) findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(this);
            pref.setSummary(pref.getText());
        }
    }

    private void changeTextPreference(String key, String value) {
        EditTextPreference pref = (EditTextPreference) findPreference(key);
        pref.setSummary(value);
        HeliosUserData.getInstance().setValue(key, value);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        configTextPreference(getString(R.string.setting_username));
        configTextPreference(getString(R.string.setting_user_id));
        configTextPreference(getString(R.string.setting_fullname));
        configTextPreference(getString(R.string.setting_phone_number));
        configTextPreference(getString(R.string.setting_email_address));
        configTextPreference(getString(R.string.setting_home_address));
        configTextPreference(getString(R.string.setting_work_address));
        configTextPreference("homelat");
        configTextPreference("homelong");
        configTextPreference("worklat");
        configTextPreference("worklong");

        ListPreference prefList = (ListPreference) findPreference("list");
        if (prefList != null) {
            prefList.setOnPreferenceChangeListener(this);
            String entry = prefList.getEntry().toString();
            Log.d(TAG, "sharing oncreate " + entry);
            prefList.setSummary(entry);
        }

        ListPreference prefListLoc = (ListPreference) findPreference("location_setting");
        if (prefListLoc != null) {
            prefListLoc.setOnPreferenceChangeListener(this);
            String entry = prefListLoc.getEntry().toString();
            Log.d(TAG, "location_setting oncreate " + entry);
            prefListLoc.setSummary(entry);
            /*int defaultIndex = 1;
            prefListLoc.setValueIndex(defaultIndex);
            String entry = prefListLoc.getEntries()[defaultIndex].toString();
            prefListLoc.setSummary(entry);
            Log.d(TAG, "location_setting oncreate " + entry);
            HeliosUserData.getInstance().setValue(getString(R.string.setting_location), "" + defaultIndex);*/
        }

        ListPreference prefListTag = (ListPreference) findPreference("tag_setting");
        if (prefListTag != null) {
            prefListTag.setOnPreferenceChangeListener(this);
            String entry = prefListTag.getEntry().toString();
            Log.d(TAG, "location_tag oncreate " + entry);
            prefListTag.setSummary(entry);
        }

        configTextPreference("max_context_messages");

        ListPreference prefImportance = (ListPreference) findPreference("lowest_message_importance");
        if (prefImportance != null) {
            prefImportance.setOnPreferenceChangeListener(this);
            String entry = prefImportance.getEntry().toString();
            Log.d(TAG, "lowest_message_importance oncreate " + entry);
            prefImportance.setSummary(entry);
        }

        ListPreference prefTheme = (ListPreference) findPreference("pref_key_theme");
        if (prefTheme != null) {
            prefTheme.setOnPreferenceChangeListener(this);
            String entry = prefTheme.getEntry().toString();
            Log.d(TAG, "theme oncreate " + entry);
            prefTheme.setSummary(entry);
        }

        // Using separate button to activate profile image switch
        Preference switchPicture = findPreference("switchpic");
        Log.d(TAG, "switchPicture found");
        switchPicture.setOnPreferenceClickListener(this);
        prefPicture = (PicturePreference)findPreference("picture");
        Log.d(TAG, "PicturePreference found");


        // Update version name for the HeliosTestClient
        Preference version = findPreference("version");
        String versionName = "-";
        try {
            versionName = this.getContext().getPackageManager().getPackageInfo(this.getContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get getPackageInfo");
            e.printStackTrace();
        }
        version.setSummary(versionName);

        // Update build date for the HeliosTestClient
        Preference build = findPreference("build");
        String buildName = "-";
        try {
            buildName =  new Date(BuildConfig.TIMESTAMP).toString();
        } catch (Exception e1) {
            Log.e(TAG, "Could not get BuildConfig.TIMESTAMP");
            e1.printStackTrace();
        }
        build.setSummary(buildName);

        // Update relays info
        Preference relays = findPreference("relays");
        String[] relayAddress = getResources().getStringArray(R.array.relay_addresses);
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < relayAddress.length; i++) {
            sb.append(relayAddress[i]);
            sb.append("\n");
        }
        String str = sb.toString();
        relays.setSummary(str);

        // Using separate button to view home location
        Preference viewHome = findPreference("viewhome");
        Log.d(TAG, "viewHome found");
        viewHome.setOnPreferenceClickListener(this);

        // Using separate button to view home location
        Preference viewWork = findPreference("viewwork");
        Log.d(TAG, "viewWork found");
        viewWork.setOnPreferenceClickListener(this);

        // Using separate button to view home location
        Preference viewHomeCoord = findPreference("viewhomecoord");
        Log.d(TAG, "viewHome found");
        viewHomeCoord.setOnPreferenceClickListener(this);

        // Using separate button to view home location
        Preference viewWorkCoord = findPreference("viewworkcoord");
        Log.d(TAG, "viewWork found");
        viewWorkCoord.setOnPreferenceClickListener(this);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        // Log.d(TAG, "preferencesName username is " + o.toString());
        if (preference instanceof EditTextPreference) {
            changeTextPreference(preference.getKey(), o.toString());
        } else {
            if (preference.getKey().equals("list")) {
                Log.d(TAG, "sharing changed");
                ListPreference pref = (ListPreference) findPreference("list");
                int index = Integer.parseInt(o.toString());
                String entry = pref.getEntries()[index].toString();
                pref.setSummary(entry);
                Log.d(TAG, "sharing is now " + entry);
                HeliosUserData.getInstance().setValue(getString(R.string.setting_sharing), "" + index);
            }
            if (preference.getKey().equals("picture")) {
                Log.d(TAG, "picture invoked");
            }
            if (preference.getKey().equals("location_setting")) {
                Log.d(TAG, "location_setting");
                ListPreference pref = (ListPreference) findPreference("location_setting");
                int index = Integer.parseInt(o.toString());
                String entry = pref.getEntries()[index].toString();
                pref.setSummary(entry);
                Log.d(TAG, "location_setting is now " + entry);
                HeliosUserData.getInstance().setValue(getString(R.string.setting_location), "" + index);
            }
            if (preference.getKey().equals("tag_setting")) {
                Log.d(TAG, "tag_setting");
                ListPreference pref = (ListPreference) findPreference("tag_setting");
                int index = Integer.parseInt(o.toString());
                String entry = pref.getEntries()[index].toString();
                pref.setSummary(entry);
                Log.d(TAG, "tag_setting is now " + entry);
                HeliosUserData.getInstance().setValue(getString(R.string.setting_tag), "" + index);
            }
            if (preference.getKey().equals("lowest_message_importance")) {
                Log.d(TAG, "lowest_message_importance");
                ListPreference pref = (ListPreference) findPreference("lowest_message_importance");
                int index = Integer.parseInt(o.toString());
                String entry = pref.getEntries()[index].toString();
                pref.setSummary(entry);
                Log.d(TAG, "lowest message importance is now " + entry);
                // KISAAH: Is the below bug? tag_setting -> lowest_message_importance??
                // HeliosUserData.getInstance().setValue(getString(R.string.setting_tag), "" + index);
            }
            if (preference.getKey().equals("pref_key_theme")) {
                Log.d(TAG, "pref_key_theme");
                ListPreference pref = (ListPreference) findPreference("pref_key_theme");
                String entry = o.toString();
                pref.setSummary(entry);
                Log.d(TAG, "theme is now " + entry);
                HeliosUserData.getInstance().setValue(getString(R.string.setting_theme), "" + entry);
                UiUtils.setTheme(getActivity(), entry);
            }
        }
        // Accept change, should validate it.
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

    private boolean showMap(String uri) {
        Uri gmmIntentUri = Uri.parse(uri);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Activity not found - map invovation failed");
            return false;
        }
        if (mapIntent.resolveActivity(activity.getApplicationContext().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Log.e(TAG, "Map invocation failed");
            return false;
        }
        return true;
    }

    private boolean viewAddress(String key) {
        EditTextPreference preference = findPreference(key);
        if (preference == null) {
            return false;
        }
        String address = preference.getText();
        if (address == null) {
            return false;
        }
        return showMap("geo:0,0?q=" + address);
    }

    private boolean checkCoordinate(String value, double minVal, double maxVal,
                                    String errNumberFormat, String errNullString, String errRange) {
        double coordinate;
        try {
            coordinate = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, errNumberFormat);
            Activity activity = getActivity();
            Toast.makeText(activity.getApplicationContext(), errNumberFormat, Toast.LENGTH_SHORT).show();
            return false;
        } catch (NullPointerException e) {
            Log.e(TAG, errNullString);
            Activity activity = getActivity();
            Toast.makeText(activity.getApplicationContext(), errNullString, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (coordinate < minVal || coordinate > maxVal) {
            Log.e(TAG, errRange);
            Activity activity = getActivity();
            Toast.makeText(activity.getApplicationContext(), errRange, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean viewLocation(String keylat, String keylong, String label) {
        boolean ret;
        EditTextPreference latPreference = findPreference(keylat);
        if (latPreference == null) {
            return false;
        }
        String latitudeString = latPreference.getText();
        ret = checkCoordinate(latitudeString, -90.0, 90.0,
                "Bad latitude number format - not floating point number",
                "Null string as latitude", "Latitude must be between (-90,90)");
        if (!ret) {
            return false;
        }

        EditTextPreference longPreference = findPreference(keylong);
        if (longPreference == null) {
            return false;
        }
        String longitudeString = longPreference.getText();
        ret = checkCoordinate(longitudeString, -180.0, 180.0,
                "Bad longitude number format - not floating point number",
                "Null string as longitude", "Longitude must be between (-180,180)");
        if (!ret) {
            return false;
        }

        return showMap("geo:0,0?q=" + latitudeString + "," + longitudeString + "(" + label + ")");
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.hasKey()) {
            String key = preference.getKey();
            boolean ret = true;
            switch (key) {
                case "switchpic":
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    String[] mimeTypes = {"image/jpeg", "image/png"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    Log.d(TAG, "sending pick intent");
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                    break;
                case "viewhome":
                    ret = viewAddress("home");
                    break;
                case "viewwork":
                    ret = viewAddress("work");
                    break;
                case "viewhomecoord":
                    ret = viewLocation("homelat", "homelong", "Home");
                    break;
                case "viewworkcoord":
                    ret = viewLocation("worklat", "worklong", "Work");
                    break;
                default:
                    Log.e(TAG, "key " + key + " not recognized");
                    return false;
            }
            return ret;
        } else {
            Log.d(TAG, "No key bound to a preference");
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Log.e(TAG, "Image data is not available (intent was null)");
                return;
            }
            ContentResolver resolver = getActivity().getContentResolver();
            try {
                InputStream inputStream = resolver.openInputStream(data.getData());
                Log.d(TAG, "Input stream opened");
                String pathname = Environment.getExternalStorageDirectory().getAbsolutePath()+"/avatar.img";
                FileOutputStream outputStream = new FileOutputStream(pathname);
                Log.d(TAG, "Output stream opened");
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                Log.d(TAG, "Stream copied");
                inputStream.close();
                outputStream.close();
                Drawable drawable = Drawable.createFromPath(pathname);
                Log.d(TAG, "Image data read from avatar.img");
                prefPicture.setIcon(drawable);
                Log.d(TAG, "Icon set");
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Image data is not available (file not found)");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "Image data is not available (read error)");
                e.printStackTrace();
            }
        }
    }
}
