package eu.h2020.helios_social.heliostestclient.ui;

import android.content.Context;
import android.view.KeyEvent;

import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.Nullable;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_NULL;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode;
import eu.h2020.helios_social.heliostestclient.R;


public class UiUtils {
    public static void setTheme(Context ctx, String theme) {
        if (theme.equals(ctx.getString(R.string.pref_theme_light_value))) {
            setDefaultNightMode(MODE_NIGHT_NO);
        } else if (theme
                   .equals(ctx.getString(R.string.pref_theme_dark_value))) {
            setDefaultNightMode(MODE_NIGHT_YES);
        } else if (theme
                   .equals(ctx.getString(R.string.pref_theme_system_value))) {
            setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            setDefaultNightMode(MODE_NIGHT_YES);
        }
    }

    public static boolean enterPressed(int actionId,
                                       @Nullable KeyEvent keyEvent) {
        return actionId == IME_NULL &&
            keyEvent != null &&
            keyEvent.getAction() == ACTION_DOWN &&
            keyEvent.getKeyCode() == KEYCODE_ENTER;
    }

    public static void setError(TextInputLayout til, @Nullable String error,
                                boolean set) {
        if (set) {
            if (til.getError() == null) til.setError(error);
        } else {
            til.setError(null);
        }
    }
}
