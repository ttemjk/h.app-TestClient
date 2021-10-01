package eu.h2020.helios_social.heliostestclient.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.annotation.Nullable;

import eu.h2020.helios_social.core.profile.HeliosUserData;
import eu.h2020.helios_social.heliostestclient.ui.SplashScreenActivity;
import eu.h2020.helios_social.heliostestclient.ui.UiUtils;
import eu.h2020.helios_social.heliostestclient.R;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_NEXT;

public class SetUsernameFragment extends Fragment
    implements TextWatcher, OnEditorActionListener, OnClickListener {

    private final static String TAG = SetUsernameFragment.class.getName();
    private static final int MAX_USER_NAME_LENGTH = 50;

    private Button nextButton;
    // private ProgressBar progressBar;
    private TextInputLayout userNameWrapper;
    private TextInputEditText userNameInput;

    public static SetUsernameFragment newInstance() {
        return new SetUsernameFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setup_username,
                                  container, false);

        nextButton = v.findViewById(R.id.set_and_continue);
        userNameWrapper = v.findViewById(R.id.username_entry_wrapper);
        userNameInput = v.findViewById(R.id.username_entry);
        userNameInput.addTextChangedListener(this);

        nextButton.setOnClickListener(this);

        return v;
    }

    public String getUniqueTag() {
        return TAG;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
        // noop
    }

    @Override
    public void onTextChanged(CharSequence userName, int i, int i1, int i2) {
        int nlen = userNameInput.getText().toString().length();
        boolean nameError = nlen > MAX_USER_NAME_LENGTH;
        UiUtils.setError(userNameWrapper, getString(R.string.name_too_long),
                         nameError);

        boolean enabled = !nameError && nlen > 0;
        nextButton.setEnabled(enabled);
        userNameInput.setOnEditorActionListener(enabled ? this : null);
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId,
                                  @Nullable KeyEvent keyEvent) {
        if (actionId == IME_ACTION_NEXT || actionId == IME_ACTION_DONE ||
            UiUtils.enterPressed(actionId, keyEvent)) {
            onClick(textView);
            return true;
        }
        return false;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        // noop
    }

    @Override
    public void onClick(View view) {
        String key = getString(R.string.setting_username);
        String value = userNameInput.getText().toString().trim();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences
                        (getActivity().getApplicationContext());
        prefs.edit().putString(key, value).apply();
        HeliosUserData.getInstance().setValue(key, value);
        // Change to next dialog
        ((SplashScreenActivity)getActivity()).startNextFragment();
    }
}
