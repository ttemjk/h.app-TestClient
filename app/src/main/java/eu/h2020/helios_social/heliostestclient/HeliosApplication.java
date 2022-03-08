package eu.h2020.helios_social.heliostestclient;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.Arrays;
import java.util.HashSet;

import eu.h2020.helios_social.core.storage.HeliosStorageManager;
import eu.h2020.helios_social.heliostestclient.service.MessagingService;
import eu.h2020.helios_social.heliostestclient.ui.UiUtils;

/**
 * Base application. Currently used to initialize {@link AndroidThreeTen} library that is used
 * to have support of ZonedDateTime for older devices.
 * <p>
 * Can also be used to initialize application wide singletons and/or enable DEBUG BuildConfig.
 */
public class HeliosApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "HeliosApplication";
    private boolean mIsInBackground = false;
    private LocalBroadcastManager mLocalBroadcastManager;
    private int mCount = 0;
    private Handler mHandler = new Handler();
    private volatile SharedPreferences prefs;

    @Override
    protected void attachBaseContext(Context base) {
        if (prefs == null)
             prefs = PreferenceManager.getDefaultSharedPreferences(base);
        super.attachBaseContext(base);
        setTheme(base, prefs);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        Log.d(TAG, "onCreate");

        // Enable if testing.
        /*
        if (BuildConfig.DEBUG) {
            Log.d(TAG,"BuildConfig.DEBUG");
            System.gc();

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectAll()
                    .build());
        }*/
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        registerActivityLifecycleCallbacks(this);

        initRelayAddresses();
        initPnetSwarmKey();
    }

    @Override
    public void onTrimMemory(int i) {
        super.onTrimMemory(i);
        Log.d(TAG, "onTrimMemory > " + i);
        // ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN not called when Foreground notification
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mCount++;
        Log.d(TAG, "onActivityResumed > foreground :" + mCount);

        if (mIsInBackground) {
            Log.d(TAG, "onActivityResumed > background notify");
            mIsInBackground = false;
            Intent intent = new Intent(MessagingService.FOREGROUND_ACTION);
            intent.putExtra(MessagingService.FOREGROUND_ACTION, false);

            mLocalBroadcastManager.sendBroadcastSync(intent);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "onActivityPaused:" + mCount);
        mCount--;
        mHandler.postDelayed(() -> {
            Log.d(TAG, "onActivityPaused check resumed count:" + mCount);
            if(mCount == 0){
                if(!mIsInBackground) {
                    mIsInBackground = true;
                    Intent intent = new Intent(MessagingService.FOREGROUND_ACTION);
                    intent.putExtra(MessagingService.FOREGROUND_ACTION, true);
                    mLocalBroadcastManager.sendBroadcastSync(intent);
                }
            }
        }, 200);
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "onActivityDestroyed ");
    }

    private void setTheme(Context ctx, SharedPreferences prefs) {
        String theme = prefs.getString("pref_key_theme", null);
        if (theme == null) {
            // set default value
            theme = getString(R.string.pref_theme_dark_value);
            prefs.edit().putString("pref_key_theme", theme).apply();
        }
        UiUtils.setTheme(ctx, theme);
    }

    private void initRelayAddresses() {
        String[] relayAddresses = getResources().getStringArray(R.array.relay_addresses);
        SharedPreferences nodePrefs = getSharedPreferences("helios-node-libp2p-prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = nodePrefs.edit();
        if(relayAddresses != null) {
            Log.d(TAG, "set relay addresses: " + relayAddresses);
            HashSet<String> addresses = new HashSet<String>(Arrays.asList(relayAddresses));
            editor.putStringSet("relayAddresses", addresses);
            editor.commit();
        } else {
            Log.d(TAG, "No relay addresses");
        }
    }

    private void initPnetSwarmKey() {
        String swarmKeyProtocol = getResources().getString(R.string.swarm_key_protocol);
        String swarmKeyEncoding = getResources().getString(R.string.swarm_key_encoding);
        String swarmKeyData = getResources().getString(R.string.swarm_key_data);
        if (swarmKeyProtocol == null) {
            Log.d(TAG, "Swarm key protocol not set - ignore pnet key");
            return;
        }
        if (swarmKeyEncoding == null) {
            Log.d(TAG, "Swarm key encoding not set - ignore pnet key");
            return;
        }
        if (swarmKeyData == null) {
            Log.d(TAG, "Swarm key data not set - ignore pnet key");
            return;
        }
        SharedPreferences pnetPrefs = getSharedPreferences("helios-node-libp2p-prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pnetPrefs.edit();
        editor.putString("swarmKeyProtocol", swarmKeyProtocol);
        editor.putString("swarmKeyEncoding", swarmKeyEncoding);
        editor.putString("swarmKeyData", swarmKeyData);
        boolean ok = editor.commit();
        if (ok) {
            Log.d(TAG, "Swarm key set");
        } else {
            Log.w(TAG, "Swarm key setting failed");
        }
    }
}
