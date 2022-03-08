package eu.h2020.helios_social.heliostestclient.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.DetectedActivity;
import com.google.gson.JsonParseException;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.context.ContextListener;
import eu.h2020.helios_social.core.context.ext.ActivityContext;
import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.core.info_control.InfoControl;
import eu.h2020.helios_social.core.info_control.MessageContextRepository;
import eu.h2020.helios_social.core.info_control.MessageImportance;
import eu.h2020.helios_social.core.info_control.MessageInfo;
import eu.h2020.helios_social.core.info_control.MyContexts;
import eu.h2020.helios_social.core.info_control.MyContextsDatabase;
import eu.h2020.helios_social.core.messaging.HeliosMessage;
import eu.h2020.helios_social.core.messaging.HeliosMessageListener;
import eu.h2020.helios_social.core.messaging.HeliosMessagingException;
import eu.h2020.helios_social.core.messaging.HeliosTopic;
import eu.h2020.helios_social.core.messaging.data.HeliosConversation;
import eu.h2020.helios_social.core.messaging.data.HeliosConversationList;
import eu.h2020.helios_social.core.messaging.data.HeliosMessagePart;
import eu.h2020.helios_social.core.messaging.data.HeliosTopicContext;
import eu.h2020.helios_social.core.messaging.data.JsonMessageConverter;
import eu.h2020.helios_social.core.profile.HeliosProfileManager;
import eu.h2020.helios_social.core.profile.HeliosUserData;
import eu.h2020.helios_social.core.sensor.ext.ActivitySensor;
import eu.h2020.helios_social.core.sensor.ext.LocationSensor;
import eu.h2020.helios_social.core.sensor.ext.TimeSensor;
import eu.h2020.helios_social.core.sensor.ext.WifiSensor;
import eu.h2020.helios_social.core.trustmanager.TrustManager;
import eu.h2020.helios_social.heliostestclient.service.ContactList;
import eu.h2020.helios_social.heliostestclient.service.HeliosMessagingServiceHelper;
import eu.h2020.helios_social.heliostestclient.service.MessagingService;
import eu.h2020.helios_social.heliostestclient.ui.adapters.MainAdapter;
import eu.h2020.helios_social.heliostestclient.R;

/**
 * Main activity for the TestClient.
 */
public class
MainActivity extends AppCompatActivity implements HeliosMessageListener, ContextListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_ALL = 1;
    private static final boolean runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    private static final String activityPermission = runningQOrLater ? Manifest.permission.ACTIVITY_RECOGNITION :
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private MainAdapter mTopicAdapter = null;
    private String mPreviousTag = null;

    private static final ContactList mContactList = ContactList.getInstance();
    private final HeliosMessagingServiceHelper mMessageMgr = HeliosMessagingServiceHelper.getInstance();

    private static MainActivity activity;

    private ContextualEgoNetwork mEgoNetwork;
    private TrustManager mTrustManager;

    // Contexts for MyContexts view
    public static MyContexts mMyContexts = null;

    // Sensors used by contexts
    public LocationSensor mLocationSensor;
    public ActivitySensor mActivitySensor;
    public TimeSensor mTimeSensor;
    public WifiSensor mWifiSensor;

    // Information overload control
    public static InfoControl mInfoControl;

    public static List<MessageInfo> mMessageInfo;
    public static HashMap<String, List<MessageImportance>> mImportances;

    // Tracks the status of the location updates request
    private Boolean mRequestingLocationUpdates = false;
    // Tracks the status of the activity updates request
    private Boolean mRequestingActivityUpdates = false;

    private volatile boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.HeliosTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        activity = this;
        started = false;

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        showWelcomeView();

        // Check if settings should be opened
        checkSettings();
    }

    private void init() {

        // Init contextual ego network
        // initCen();
        initCenTest(); // temporarily

        // Init/check context module. Create example contexts for MyContexts view
        initContext();

        // Load stored topics and contexts
        mContactList.loadTopics(this.getApplicationContext(), mEgoNetwork);

        setupTopicListView();

        // Handle permissions
        String[] PERMISSIONS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                activityPermission
        };
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            Log.d(TAG, "Permissions OK");
            start();
        }

        // An example how to receive an intent with data
        handleIncomingIntent(getIntent());
    }

    // while settings are checked (activity) continue initialization (init method)
    public void settingsChecked(boolean checked) {
        if(checked && !started) {
            new Thread(() -> init()).start();
        }
    }

    // start service and sensors
    private void start() {
        // Bind to service via helper class
        mMessageMgr.setContext(this.getApplicationContext());
        mMessageMgr.bindService(this);

        // start sensor updates
        startOrStopSensorUpdates();

        // Start messaging service, could be in helper class also.
        Intent startIntent = new Intent(this.getApplicationContext(), MessagingService.class);
        startIntent.setAction(MessagingService.START_ACTION);
        ContextCompat.startForegroundService(this.getApplicationContext(), startIntent);
        started = true;
    }

    private void setupTopicListView() {

        mTopicAdapter = new MainAdapter(this, mContactList.getTopics(), mInfoControl, mMessageInfo, mImportances);

        mTopicAdapter.setOnItemClickListener(topicClickHandler);
        // If topics loaded already, hide progress bar
        if(mTopicAdapter.getItemCount() > mTopicAdapter.getActiveContextsCount()) {
            hideWelcomeView();
        }
        mHandler.post(() -> {
            RecyclerView listView = findViewById(R.id.messageListView);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            listView.setLayoutManager(layoutManager);
            listView.setAdapter(mTopicAdapter);
        });
    }

    public static MainActivity getActivity() {
        return activity;
    }

    private final MainAdapter.OnItemClickListener topicClickHandler = new MainAdapter.OnItemClickListener() {
        @Override
        public void onClick(HeliosTopicContext htc) {
            Log.d(TAG, "ctx.uuid:" + htc.uuid);

            // If the topic has uuid set, it is a 1-1 chat for now
            if (!TextUtils.isEmpty(htc.uuid)) {
                Intent i = new Intent(MainActivity.this, DirectChatActivity.class);
                i.putExtra(DirectChatActivity.CHAT_UUID, htc.uuid);
                i.putExtra(DirectChatActivity.CHAT_ID, htc.topic);
                startActivity(i);
            } else {
                String test = htc.topic;
                Intent i = new Intent(MainActivity.this, ChatActivity.class);
                i.putExtra(ChatActivity.CHAT_ID, test);
                startActivity(i);
            }

            Iterator<MessageInfo> itr = mMessageInfo.iterator();
            while (itr.hasNext()) {
                MessageInfo m = itr.next();
                // Get all messages in the same topic as the one clicked.
                if (m.getMessageTopic().equals(htc.topic)) {
                    // Set reaction time and MessageContext
                    mInfoControl.readMessage(m);
                    // Remove from MessageInfo list
                    itr.remove();
                }
            }
        }

        @Override
        public void onClick(Context c) {
            // Change activity in root Context classes only.
            if (c.getClass().equals(Context.class)) {
                // c.setActive(!c.isActive());
                mMyContexts.setActive(c, !c.isActive());
                notifyDataSetChanged();
            } else {
                Toast.makeText(MainActivity.this, "Context " + c.getName() + " is automatically activated!", Toast.LENGTH_SHORT).show();
            }
        }




        @Override
        public void onLongClick(HeliosTopicContext htc) {
            Log.d(TAG, "ctx.uuid:" + htc.uuid);
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            // Allow deleting chats with uuid (1-1) for now
            if (!TextUtils.isEmpty(htc.uuid)) { // the long clicked item is a direct message
                builder.setPositiveButton("Yes", (dialog, id) -> {
                    mContactList.delTopic(htc.topic);
                    HeliosConversationList.getInstance().deleteConversationByTopicUUID(htc.uuid);
                    notifyDataSetChanged();
                    MessagingService service = mMessageMgr.getService();
                    /*
                    if(service != null) {
                        service.doSaveJson();
                    }
                    */
                });
                // TODO strings to res
                builder.setMessage("Chat messages will be deleted (excluding externally saved files).")
                        .setTitle("Delete chat: " + htc.topic);
                builder.setNegativeButton("No", (dialog, id) -> {});
            } else if(htc.topic.equals(getString(R.string.chat_default_topic))) {
                builder.setMessage("Default chat topic " + htc.topic + " cannot be deleted");
                builder.setPositiveButton("Ok", (dialog, id) -> {});
            } else { // the long clicked item is topic
                builder.setPositiveButton("Yes", (dialog, id) -> {
                    try {
                        mContactList.delTopic(htc.topic);
                        HeliosConversationList.getInstance().getTopics().remove(htc);
                        HeliosConversation c = HeliosConversationList.getInstance().getConversation(htc.topic);
                        if(c != null) {
                            // TODO. Not syncronized. Needed remove method to HeliosConverssationList
                            HeliosConversationList.getInstance().getConversations().remove(c);
                            MessagingService service = mMessageMgr.getService();
                        }
                        mMessageMgr.unsubscribe(new HeliosTopic(htc.topic, htc.topic));
                        notifyDataSetChanged();
                    } catch (HeliosMessagingException e) {
                        e.printStackTrace();
                    }});
                // TODO strings to res
                builder.setMessage("Chat Topic will be deleted")
                        .setTitle("Delete chat topic: " + htc.topic);
                builder.setNegativeButton("No", (dialog, id) -> {});
            }
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        @Override
        public void onLongClick(Context c) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setPositiveButton("Yes", (dialog, id) -> {
                mMyContexts.remove(c);
                mTopicAdapter.updateDataset();
            });
            builder.setNegativeButton("No", (dialog, id) -> {
            });
            builder.setMessage("The context will be deleted")
                    .setTitle("Delete context: " + c.getName());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    private void checkSettings() {
        HeliosProfileManager profileMgr = HeliosProfileManager.getInstance();
        android.content.Context appCtx = getApplicationContext();

        String userName = profileMgr.load(appCtx, getString(R.string.setting_username));
        String openSettings = profileMgr.load(appCtx, getString(R.string.open_settings));

        // Open settings, if username not set or settings set to be opened on splash screen
        if (userName.isEmpty() || openSettings.equals("yes")) {
            // Toast.makeText(this.getApplicationContext(), "Write a name for your profile.", Toast.LENGTH_LONG).show();
            profileMgr.store(appCtx, getString(R.string.open_settings), "no");
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            settingsChecked(false);
        } else {
            settingsChecked(true);
        }
    }

    private void startOrStopSensorUpdates() {
        // Turn on/off location and Activity sensor updates
        Log.d(TAG, "startOrStopSensorUpdates()");

        // TODO: There should be controls to enable/disable updates more easily?
        try {
            int val = Integer.parseInt(HeliosUserData.getInstance().getValue(getString(R.string.setting_location)));
            if (val == 0) {
                Log.d(TAG, ">startLocationUpdates");
                mLocationSensor.startUpdates();
                mRequestingLocationUpdates = true;
            } else {
                Log.d(TAG, ">stopLocationUpdates");
                mLocationSensor.stopUpdates();
                mRequestingLocationUpdates = false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error getting setting location.");
            Log.d(TAG, ">stopLocationUpdates");
            mLocationSensor.stopUpdates();
            mRequestingLocationUpdates = false;
        }
        // Activity sensor updates
        // TODO. Value from settings? Now allowed if location allowed
        if (mRequestingLocationUpdates) {
            if (!mRequestingActivityUpdates) {
                mActivitySensor.startUpdates();
                mRequestingActivityUpdates = true;
            }
        } else {
            if (mRequestingActivityUpdates) {
                mActivitySensor.stopUpdates();
                mRequestingActivityUpdates = false;
            }
        }
        // time sensor updates
        mTimeSensor.startUpdates();
        // WiFi sensor updates
        mWifiSensor.startUpdates();
    }

    private void notifyDataSetChanged() {
        if(mTopicAdapter != null) {
            mTopicAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult");
        /*
        if (requestCode == PERMISSION_ALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "granted");
            } else {
                Log.d(TAG, "not granted");
            }
        }*/
        // Continue starting service even if user has not granted all permissions, then some parts
        // will be disabled.
        // TODO: Notify user why permissions are needed if not granted.
        start();
    }

    public static boolean hasPermissions(android.content.Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     *  An example of creation of CEN using the CENlibrary.
     *  userId is given as the id for the ego node.
     *  Also, TrustManager is initialized and started
     */
    private void initCen() {
        HeliosUserData profileData = HeliosUserData.getInstance();
        String userId = profileData.getValue(getString(R.string.setting_user_id));

        //TODO: CEN tries to save file to system, check proper path
        mEgoNetwork = ContextualEgoNetwork.createOrLoad("", userId, null);
        Node egoNode = mEgoNetwork.getEgo();
        Object nodeData = egoNode.getData();
        if (nodeData != null) {
            Log.i(TAG, "User data related to egoNode modified?");
        }
        String nodeId = egoNode.getId();
        if (nodeId != userId) {
            Log.i(TAG, "User id related to egoNode not found?");
        }

        // init TrustManager
        // mTrustManager = new TrustManager(mEgoNetwork, 100, new HashMap<>());
        // start TrustManager
        // mTrustManager.startModule();
    }

    //  only for testing ...
    private void initCenTest() {
        mEgoNetwork = null;
        mTrustManager = null;
    }

    /**
     * Initialize user contexts.
     * Also, information overload control is initialized.
     *
     * This method creates two location-based contexts named "At home" and "At work".
     * The location contexts (instances of the class LocationContext) take the coordinates (lat, lon) and
     * radius (in meters) as input, which values define the area (circle) where the context is active.
     * Further, the example shows how to receive updates to the contexts active value.
     * Also, it shows how to associate a context with CEN.
     * For LocationContext and LocationSensor class implementations (in context repository):
     *      @see eu.h2020.helios_social.core.context.ext.LocationContext
     *      @see eu.h2020.helios_social.core.sensor.ext.LocationSensor
     */
    private void initContext() {
        Log.d(TAG, "initContext start");

        // Init LocationSensor
        mLocationSensor = new LocationSensor("location_sensor", this);
        // init activitySensor
        mActivitySensor = new ActivitySensor("activity_sensor", this);
        // init timeSensor
        mTimeSensor = new TimeSensor("time_sensor", 10000);
        // init wifiSensor
        mWifiSensor = new WifiSensor("wifi_sensor", this);

        // Create MyContexts container.  In testClient, MyContextsActivity shows the states of the contexts.
        // The added contexts are associated with CEN.
        // Info overload control is also based on the myContexts
        mMyContexts = new MyContexts(mEgoNetwork, MyContextsDatabase.getDatabase(getApplicationContext()));
        Log.d(TAG, "MyContexts read from database: number of contexts:" + mMyContexts.getContexts().size());

        // Creating new location-based contexts named "At work" and "At home"
        // First, get the location coordinates from profile data
        HeliosUserData profileData = HeliosUserData.getInstance();
        double workLat, workLong, homeLat, homeLong;
        double radius = 1000.0;
        try {
            workLat = Double.parseDouble(profileData.getValue("worklat"));
            workLong = Double.parseDouble(profileData.getValue("worklong"));
        } catch(Exception e) {
            Log.d(TAG, "checkContext Bad number format (workLat, workLong)");
            // add some values for testing
            workLat = 60.1803;
            workLong = 24.8255;
        }
        try {
            homeLat = Double.parseDouble(profileData.getValue("homelat"));
            homeLong = Double.parseDouble(profileData.getValue("homelong"));
        } catch(Exception e) {
            Log.d(TAG, "checkContext Bad number format (homeLat, homeLong)");
            // add some values for testing
            homeLat = 60.2803;
            homeLong = 24.8255;
        }

        LocationContext locationContext1;
        LocationContext locationContext2;

        // If MyContextsDatabase is empty,  create example contexts ...
        if(mMyContexts.getContexts().size() == 0) {
            // create the location contexts
            locationContext1 = new LocationContext("location_context1","Work", workLat, workLong, radius);
            locationContext2 = new LocationContext("location_context2", "Home", homeLat, homeLong, radius);

            // add sensors
            locationContext1.addSensor(mLocationSensor);
            locationContext2.addSensor(mLocationSensor);

            // Still create some example contexts ...
            // New activity contexts
            ActivityContext inVehicleContext = new ActivityContext("Driving", DetectedActivity.IN_VEHICLE);
            ActivityContext onBicycleContext = new ActivityContext("Biking", DetectedActivity.ON_BICYCLE);
            ActivityContext onFootContext = new ActivityContext("Walking", DetectedActivity.WALKING);
            // ActivityContext stillContext = new ActivityContext("Still", DetectedActivity.STILL);

            inVehicleContext.addSensor(mActivitySensor);
            onBicycleContext.addSensor(mActivitySensor);
            onFootContext.addSensor(mActivitySensor);
            // stillContext.addSensor(mActivitySensor);

            // Add the contexts to MyContexts container
            mMyContexts.add(locationContext1);
            mMyContexts.add(locationContext2);
            mMyContexts.add(inVehicleContext);
            mMyContexts.add(onBicycleContext);
            mMyContexts.add(onFootContext);
            // mMyContexts.add(stillContext);
        } else {
            // the location contexts were read from the database and, so, already in the MyContexts
            locationContext1 = (LocationContext)mMyContexts.getContextById("location_context1");
            locationContext2 = (LocationContext)mMyContexts.getContextById("location_context2");
            if(locationContext1 != null && locationContext2 != null) {
                locationContext1.setLat(workLat);
                locationContext1.setLon(workLong);
                locationContext2.setLat(homeLat);
                locationContext2.setLon(homeLong);
            }
        }

        // For info overload control ...
        // initialize local database of received messages
        MessageContextRepository messageRepository = new MessageContextRepository(getApplicationContext());
        // Information overflow control is done in the scope of MyContexts.  Trustmanager is also related to the infocontrol..
        mInfoControl = new InfoControl(mMyContexts, mTrustManager, messageRepository);
        mMessageInfo = new ArrayList<>();
        mImportances = new HashMap<>();
        // register listeners for context changes
        for (Context c : mMyContexts.getContexts()) {
            c.registerContextListener(this);
        }
        Log.d(TAG, "initContext end");
    }

    // Create MessageInfo from received topic and message
    private MessageInfo createMessageInfo(HeliosTopic topic, HeliosMessage message) {
        if (message == null)
            return null;

        MessageInfo msgInfo = null;
        try {
            HeliosMessagePart msgPart = JsonMessageConverter.getInstance().readHeliosMessagePart(message.getMessage());
            // Ignore our own messages
            if (msgPart.messageType == HeliosMessagePart.MessagePartType.MESSAGE) {
                long timestamp = 0;
                try {
                    timestamp = ZonedDateTime.parse(msgPart.ts, DateTimeFormatter.ISO_ZONED_DATE_TIME).
                            toInstant().toEpochMilli();
                } catch (DateTimeParseException e) {
                    Log.e(TAG, "DateTimeParseException from received message: " + e.toString());
                    // TODO:? timestamp = now()?
                }
                if(msgPart.senderUUID != null && msgPart.senderUUID.equals(HeliosUserData.getInstance().getValue(getString(R.string.setting_user_id)))) {
                    // sent message
                    String messageTopic = (topic == null) ? msgPart.to : topic.getTopicName();
                    String to = messageTopic == null ? msgPart.to : null;
                    mInfoControl.sendMessage(to, messageTopic, msgPart.msg);
                } else { // received message
                    // DirectChat topics have sender's name
                    String messageTopic = (topic == null) ? msgPart.senderName : topic.getTopicName();
                    String networkId = (msgPart.senderNetworkId != null) ? msgPart.senderNetworkId
                        : HeliosMessagingServiceHelper.getInstance().getUserNetworkIdByUUID(msgPart.senderUUID);
                    List<String> contextIds;
                    if (networkId != null)
                        contextIds = mContactList.getTopicAndContactContexts(messageTopic, msgPart.senderName + ":" + networkId);
                    else
                        contextIds = mContactList.getTopicContexts(messageTopic);
                    msgInfo = new MessageInfo(msgPart.uuid, msgPart.senderName, timestamp, 0, contextIds, messageTopic, msgPart.msg);
                }
            }
        } catch (JsonParseException e) {
            // Corrupt msg?
            Log.e(TAG, "JsonParseException from received message: " + e.toString());
            return null;
        }

        return msgInfo;
    }

    // Send update to listening activities about a new message.
    private void sendUpdate(String type, HeliosTopic topic, HeliosMessage message) {
        Intent intent = new Intent(type);
        if (message != null) {
            intent.putExtra("json", message.getMessage());
        }
        if (topic != null) {
            intent.putExtra("topic", topic.getTopicName());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()");
        // Note: MainActivity is 'singleTask', thus handle received share x intents here,
        // onCreate will forward if the app is not started.
        handleIncomingIntent(intent);
    }

    /**
     * Parses the intent, logs and presents the type in a Toast for the user, if of mimeType:
     * 1) text/plain
     * 2) image/*
     * 3) video/*
     * // TODO: Initial minimal test for Intents receive, have to refactor, test, verify by WP6.
     * // TODO: Missing actual usage/sharing to other contents, viewing/storing received media.
     *
     * @param intent Received Intent (expects Intent.ACTION_SEND)
     */
    private void handleIncomingIntent(Intent intent) {
        Log.d(TAG, "handleIncomingIntent()");

        String action = intent.getAction();
        String type = intent.getType();
        Bundle extras = intent.getExtras();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.d(TAG, "handleIncomingIntent: Received Intent type.");

            String intentDescription = "";
            String receivedText = null;

            if ("text/plain".equals(type)) {
                receivedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (receivedText != null) {
                    intentDescription = "text/plain: " + receivedText;
                }
            } else if (type.startsWith("image/")) {
                receivedText = intent.getStringExtra(Intent.EXTRA_TEXT);

                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    Log.d(TAG, "imageUri: available");
                    intentDescription = "IMG Uri: " + imageUri.toString();
                }

                if (receivedText != null) {
                    intentDescription += ", EXTRA_TEXT: " + receivedText;
                }
            } else if (type.startsWith("video/")) {
                receivedText = intent.getStringExtra(Intent.EXTRA_TEXT);

                Uri videoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (videoUri != null) {
                    Log.d(TAG, "videoUri: available");
                    intentDescription = "VIDEO Uri: " + videoUri.toString();
                }

                if (receivedText != null) {
                    intentDescription += ", EXTRA_TEXT: " + receivedText;
                }
            }

            String topic = "Citizen Journalist";  // temporarily, only for testing with CJ app
            if(extras != null) {
                String topicExt = extras.getString("HELIOS_TOPIC");
                if(topicExt != null) {
                    topic = topicExt;
                }
            }
            if(topic != null && receivedText != null) {
                sendMessageToTopic(topic, receivedText);
            }

            // As debug, show to the user what was received
            if (!TextUtils.isEmpty(intentDescription)){
                Log.d(TAG,  "Received " + intentDescription);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() start");

        if(!started) {
            return;
        }
        startOrStopSensorUpdates();

        // Start: Check tag updates
        String tagValue = HeliosUserData.getInstance().getValue(getString(R.string.setting_tag));
        Log.d(TAG, "onResume tagValue:" + tagValue);
        Log.d(TAG, "onResume mPreviousTag:" + mPreviousTag);
        mTopicAdapter.setOnItemClickListener(topicClickHandler);
        mMessageMgr.updateHeliosIdentityInfo();

        if (mPreviousTag != tagValue) {
            if (mPreviousTag != null) {
                HeliosMessagingServiceHelper.getInstance().updateTag(tagValue, mPreviousTag);
            }
            Log.d(TAG, "tagValue:" + tagValue);

            mPreviousTag = tagValue;
        }
        // END: check tag updates
        Log.d(TAG, "onResume() end");

        notifyDataSetUpdate(1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        // mTopicAdapter.setOnItemClickListener(null);

        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");

        // Unbind from service, which continues running in the foreground.
        // User can stop the service and connection from the service notification.
        mMessageMgr.unBindService();
        // stop sensor updates
        if (mRequestingLocationUpdates) {
            mLocationSensor.stopUpdates();
        }
        if (mRequestingActivityUpdates) {
            mActivitySensor.stopUpdates();
        }
        // time sensor updates
        mTimeSensor.stopUpdates();
        // WiFi sensor updates
        mWifiSensor.stopUpdates();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.join_a_topic:
                showJoinNewTopicDialog();
                return true;

            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                //Toast.makeText(this.getApplicationContext(), "Settings..", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;

            case R.id.action_peer_tag_list:
                Intent j = new Intent(this, PeerTagActivity.class);
                startActivity(j);
                return true;

            case R.id.my_contexts:
                startActivity(new Intent(this, MyContextsActivity.class));
                return true;

            case R.id.contact_list:
                startActivity(new Intent(this, ContactListActivity.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void joinNewTopic(String topic) {
        Log.d(TAG, "joinNewTopic :" + topic);

        if (!TextUtils.isEmpty(topic)) {
            ArrayList<HeliosTopicContext> arrTopics = mContactList.getTopics();
            for (int i = 0; i < arrTopics.size(); i++) {
                HeliosTopicContext tpc = arrTopics.get(i);
                if (tpc.topic.equals(topic)) {
                    Log.d(TAG, "Topic already exists, not joining:" + topic);
                    return;
                }
            }
            createConversation(topic);
            MessagingService service = mMessageMgr.getService();
        }
    }

    private void createConversation(String topicName) {
        Log.d(TAG, "createConversation with topic :" + topicName);

        HeliosConversation defaultConversation = new HeliosConversation();
        defaultConversation.topic = new HeliosTopicContext(topicName, "-", "-", "-");

        HeliosConversationList.getInstance().addConversation(defaultConversation);

        mContactList.addTopic(topicName);

        // Update topic adapter
        notifyDataSetUpdate();
        try {
            mMessageMgr.subscribe(new HeliosTopic(topicName, topicName));
        } catch (HeliosMessagingException e) {
            e.printStackTrace();
        }
    }

    private void showJoinNewTopicDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        EditText text = new EditText(this);
        builder.setView(text);
        builder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                joinNewTopic(text.getText().toString());
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        // TODO: Strings to res
        builder.setTitle("Join a topic");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void showMessage(HeliosTopic topic, HeliosMessage message) {
        Log.d(TAG, "showMessage(), topic: " + topic);

        // Initialized message. Could be a broadcast or other.
        if (null == topic && null == message) {
            Log.d(TAG, "init showMessage");
            notifyDataSetUpdate();
            sendUpdate("helios_message", null, null);
            return;
        }
        if (null == topic) {
            // DirectMessage
            Log.d(TAG, "topic: null message:" + message.getMessage());
        } else {
            Log.d(TAG, "topic: " + topic.getTopicName() + " message:" + message.getMessage());
        }

        MessageInfo msgInfo = createMessageInfo(topic, message);
        // Check that message is not a duplicate
        if (msgInfo != null && !mImportances.containsKey(msgInfo.getId())) {
            List<MessageImportance> importances = mInfoControl.getMessageImportance(msgInfo);
            mMessageInfo.add(msgInfo);
            mImportances.put(msgInfo.getId(), importances);
            sendUpdate("message_info", topic, message);
        }

        // Update internal data
        sendUpdate("helios_message", topic, message);

        notifyDataSetUpdate(10);
    }

    // Welcome view
    public void showWelcomeView() {
        mHandler.post(() -> {
            findViewById(R.id.heliosWelcome).setVisibility(View.VISIBLE);
            findViewById(R.id.progressBarUPDATE).setVisibility(View.VISIBLE);
            findViewById(R.id.heliosSocialNetwork).setVisibility(View.VISIBLE);
        });
    }

    public void hideWelcomeView() {
        mHandler.post(() -> {
            findViewById(R.id.heliosWelcome).setVisibility(View.INVISIBLE);
            findViewById(R.id.progressBarUPDATE).setVisibility(View.INVISIBLE);
            findViewById(R.id.heliosSocialNetwork).setVisibility(View.INVISIBLE);
            if(mTopicAdapter != null) {
                mTopicAdapter.updateDataset();
            }
        });
    }

    private final Runnable runnable = new Runnable() {
        public void run() {
            if(mTopicAdapter != null) {
                mTopicAdapter.updateDataset();
            }
        }
    };

    private void notifyDataSetUpdate(int delay) {
        mHandler.postDelayed(runnable, delay);
    }

    public void notifyDataSetUpdate() {
        mHandler.post(runnable);
    }

    /**
     * Implements the ContextLister interface contextChanged method.
     * This is called when context is changed. Sending notification to chat
     * channel about context change.
     *
     * @param active
     * @see eu.h2020.helios_social.core.context.ContextListener
     */
    @Override
    public void contextChanged(boolean active) {
        Log.i(TAG, "Context changed " + active);
        mHandler.post(() -> mTopicAdapter.updateContexts());
    }

    /**
     * A helper method to send a text message to topic chat
     * @param topic
     * @param text
     */
    private void sendMessageToTopic(String topic, String text) {
        HeliosProfileManager profileMgr = HeliosProfileManager.getInstance();
        String userName = profileMgr.load(this, "username");
        String userId = profileMgr.load(this, getString(R.string.setting_user_id), android.content.Context.MODE_PRIVATE);
        String ts = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now());
        HeliosMessagePart msg = new HeliosMessagePart(text, userName, userId, topic, ts);
        try {
            HeliosMessagingServiceHelper.getInstance().publish(new HeliosTopic(topic, topic), msg);
        } catch (HeliosMessagingException e) {
            e.printStackTrace();
        }
    }

    public void showToast(final String text) {
        mHandler.post(() -> {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        });
    }

}
