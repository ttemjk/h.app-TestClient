package eu.h2020.helios_social.heliostestclient.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.logging.Logger;

import javax.annotation.Nullable;

import eu.h2020.helios_social.core.profile.HeliosProfileManager;
import eu.h2020.helios_social.heliostestclient.ui.fragments.MainOrSettingsFragment;
import eu.h2020.helios_social.heliostestclient.ui.fragments.SetUsernameFragment;
import eu.h2020.helios_social.heliostestclient.R;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.os.Build.VERSION.SDK_INT;
import static java.util.logging.Logger.getLogger;

public class SplashScreenActivity extends AppCompatActivity {

    private static final Logger LOG =
        getLogger(SplashScreenActivity.class.getName());

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        if (SDK_INT >= 21) {
            getWindow().setExitTransition(new Fade());
        }

        setContentView(R.layout.splash);

        // Init/check user settings
        if (checkUserAccount()) {
            // Create username fragment, if username not set
            getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SetUsernameFragment()).
                addToBackStack(null).commit();
        } else {
            new Handler().postDelayed(() -> {
                    startNextActivity(MainActivity.class);
                    supportFinishAfterTransition();
            }, 1000);
        }
    }

    private void startNextActivity(Class<? extends Activity> activityClass) {
        Intent i = new Intent(this, activityClass);
        i.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private boolean checkUserAccount() {
        HeliosProfileManager profileMgr = HeliosProfileManager.getInstance();
        android.content.Context appCtx = getApplicationContext();

        // Load default values, in case settings are not launched before
        PreferenceManager.setDefaultValues(appCtx, R.xml.settings, false);
        PreferenceManager.setDefaultValues(appCtx, R.xml.notifsettings, true);

        // Check ProfileManager key generation
        profileMgr.keyInit(appCtx);
        // Get default preferences userId
        profileMgr.identityInit(this, getString(R.string.setting_user_id));

        String userName = profileMgr.load(appCtx, getString(R.string.setting_username));
        profileMgr.load(appCtx, getString(R.string.setting_fullname));
        profileMgr.load(appCtx, getString(R.string.setting_phone_number));
        profileMgr.load(appCtx, getString(R.string.setting_email_address));
        profileMgr.load(appCtx, getString(R.string.setting_home_address));
        profileMgr.load(appCtx, getString(R.string.setting_work_address));
        profileMgr.load(appCtx, "homelat");
        profileMgr.load(appCtx, "homelong");
        profileMgr.load(appCtx, "worklat");
        profileMgr.load(appCtx, "worklong");
        profileMgr.load(appCtx, getString(R.string.setting_tag));

        // Check that sharing preference value is numerical and if not set it to zero
        String sharing = profileMgr.load(appCtx, getString(R.string.setting_sharing));
        try {
            int val = Integer.parseInt(sharing);
        } catch (NumberFormatException e) {
            profileMgr.store(appCtx, getString(R.string.setting_sharing), "0");
        }

        // Check that sharing preference value is numerical and if not set it to zero
        String location = profileMgr.load(appCtx, getString(R.string.setting_location));
        try {
            int val = Integer.parseInt(location);
        } catch (NumberFormatException e) {
            profileMgr.store(appCtx, getString(R.string.setting_location), "0");
        }
        
        return (userName.isEmpty());
    }

    public void startNextFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new MainOrSettingsFragment()).
                addToBackStack(null).commit();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            startNextActivity(MainActivity.class);
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
