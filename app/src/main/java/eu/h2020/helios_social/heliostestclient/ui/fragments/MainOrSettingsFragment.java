package eu.h2020.helios_social.heliostestclient.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.fragment.app.Fragment;

import eu.h2020.helios_social.core.profile.HeliosUserData;
import eu.h2020.helios_social.heliostestclient.R;

import javax.annotation.Nullable;

public class MainOrSettingsFragment extends Fragment {

    private final static String TAG = MainOrSettingsFragment.class.getName();

    private Button gotoMainButton;
    private Button gotoSettingsButton;

    public static MainOrSettingsFragment newInstance() {
        return new MainOrSettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main_or_settings,
                                  container, false);
        gotoMainButton = v.findViewById(R.id.goto_main);
        gotoSettingsButton = v.findViewById(R.id.goto_settings);;

        gotoMainButton.setOnClickListener(view -> {
            // Tell parent to continue to main activity
            SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences
                (getActivity().getApplicationContext());
            prefs.edit().putString
                (getString(R.string.open_settings), "no").apply();
            HeliosUserData.getInstance().setValue(getString(R.string.open_settings), "no");
            getActivity().onBackPressed();
        });

        gotoSettingsButton.setOnClickListener(view -> {
            // Tell parent to continue to main activity
            // where settings dialog is opened.
            SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences
                (getActivity().getApplicationContext());
            prefs.edit().putString
                (getString(R.string.open_settings), "yes").apply();
            HeliosUserData.getInstance().setValue(getString(R.string.open_settings), "yes");
            getActivity().onBackPressed();
        });

        return v;
    }
}
