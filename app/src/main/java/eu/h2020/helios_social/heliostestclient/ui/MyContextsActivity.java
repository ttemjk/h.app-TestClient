package eu.h2020.helios_social.heliostestclient.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.DetectedActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.context.ContextListener;
import eu.h2020.helios_social.core.context.ext.ActivityContext;
import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.core.context.ext.TimeContext;
import eu.h2020.helios_social.core.context.ext.WifiContext;
import eu.h2020.helios_social.core.info_control.InfoControl;
import eu.h2020.helios_social.core.info_control.MessageImportance;
import eu.h2020.helios_social.core.info_control.MessageInfo;
import eu.h2020.helios_social.core.info_control.MyContexts;
import eu.h2020.helios_social.heliostestclient.ui.adapters.ContextAdapter;
import eu.h2020.helios_social.heliostestclient.R;

/**
 *  MyContextsActivity
 *  - Shows user's contexts and their status in a view
 */
public class MyContextsActivity extends AppCompatActivity implements ContextListener, AdapterView.OnItemSelectedListener{

    private static final String TAG = MyContextsActivity.class.getSimpleName();

    // UI Widgets
    private RecyclerView mMyContextsView;

    private LinearLayoutManager layoutManager;

    private ContextAdapter mAdapter;

    private MyContexts mMyContexts;

    private List<MessageInfo> mMessageInfo;

    private HashMap<String, List<MessageImportance>> mImportances;

    private InfoControl mInfoControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mycontexts);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        // Locate the UI widgets.
        mMyContextsView = findViewById(R.id.mycontexts_view);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        mMyContextsView.setLayoutManager(layoutManager);

        mMyContexts = MainActivity.mMyContexts;
        mMessageInfo = MainActivity.mMessageInfo;
        mImportances = MainActivity.mImportances;
        mInfoControl = MainActivity.mInfoControl;

        // specify an adapter
        mAdapter = new ContextAdapter(this, mMyContexts.getContexts(), mInfoControl, mMessageInfo, mImportances);
        mAdapter.setOnItemClickListener(ctxClickHandler);
        mMyContextsView.setAdapter(mAdapter);

        for (Context c : mMyContexts.getContexts()) {
            c.registerContextListener(this);
        }
    }

    private final ContextAdapter.OnItemClickListener ctxClickHandler = new ContextAdapter.OnItemClickListener() {
        @Override
        public void onClick(Context c) {
            // Change activity in root Context classes only.
            if (c.getClass().equals(Context.class)) {
                // c.setActive(!c.isActive());
                mMyContexts.setActive(c, !c.isActive());
                runOnUiThread(() -> mAdapter.notifyDataSetChanged());
            } else {
                Toast.makeText(MyContextsActivity.this, "Context " + c.getName() + " is automatically activated!", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onLongClick(Context c) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MyContextsActivity.this);
            builder.setPositiveButton("Yes", (dialog, id) -> {
                mMyContexts.remove(c);
                mAdapter.updateDataset(mMyContexts.getContexts());
                runOnUiThread(() -> mAdapter.notifyDataSetChanged());
            });
            builder.setNegativeButton("No", (dialog, id) -> {
            });
            builder.setMessage("The context will be deleted")
                    .setTitle("Delete context: " + c.getName());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };
    
    @Override
    public void onResume() {
        super.onResume();
        runOnUiThread(() -> mAdapter.notifyDataSetChanged());
        // TODO:
        // mAdapter.setOnItemClickListener(mOnClickListener);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("message_info"));
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            if (intent != null) {
                runOnUiThread(() -> mAdapter.notifyDataSetChanged());
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mMyContexts != null) {
            for (Context c : mMyContexts.getContexts()) {
                c.unregisterContextListener(this);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO:
        // mAdapter.setOnItemClickListener(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mycontexts, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                this.finish();
                return true;

            case R.id.create_context:
                AlertDialog.Builder builder = new AlertDialog.Builder(MyContextsActivity.this);
                builder.setTitle("New context name");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("OK", (dialog, which) -> {
                    String context_name = input.getText().toString();
                    Context c = new Context(null, context_name, false);
                    c.registerContextListener(MyContextsActivity.this);
                    c.registerContextListener(MainActivity.getActivity());
                    mMyContexts.add(c);
                    mAdapter.updateDataset(mMyContexts.getContexts());
                    runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();
                return true;
            case R.id.create_activity_context:
                builder = new AlertDialog.Builder(MyContextsActivity.this);
                builder.setTitle("Create activity-based context");
                View view = builder.create().getLayoutInflater().inflate(R.layout.dialog_activity_context, null);
                builder.setView(view);
                final EditText contextName = view.findViewById(R.id.context_name);
                final RadioGroup contextGroup = view.findViewById(R.id.activity_context);
                contextName.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    String context_name = contextName.getText().toString();
                    int selected_context = contextGroup.getCheckedRadioButtonId();
                    Context c = null;
                    switch(selected_context) {
                        case R.id.context_walking:
                            c = new ActivityContext(context_name, DetectedActivity.WALKING);
                            break;
                        case R.id.context_invehicle:
                            c = new ActivityContext(context_name, DetectedActivity.IN_VEHICLE);
                            break;
                        case R.id.context_onbicycle:
                            c = new ActivityContext(context_name, DetectedActivity.ON_BICYCLE);
                            break;
                        case R.id.context_running:
                            c = new ActivityContext(context_name, DetectedActivity.RUNNING);
                            break;
                        case R.id.context_onfoot:
                            c = new ActivityContext(context_name, DetectedActivity.ON_FOOT);
                            break;
                        case R.id.context_still:
                            c = new ActivityContext(context_name, DetectedActivity.STILL);
                            break;
                        default:
                            break;
                    }
                    if(c != null) {
                        c.addSensor(MainActivity.getActivity().mActivitySensor);
                        c.registerContextListener(MyContextsActivity.this);
                        c.registerContextListener(MainActivity.getActivity());
                        mMyContexts.add(c);
                        mAdapter.updateDataset(mMyContexts.getContexts());
                        runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                builder.show();
                return true;
            case R.id.create_location_context:
                builder = new AlertDialog.Builder(MyContextsActivity.this);
                builder.setTitle("Create location-based context");
                final View location_view = builder.create().getLayoutInflater().inflate(R.layout.dialog_location_context, null);
                builder.setView(location_view);
                final EditText contextName1 = location_view.findViewById(R.id.context_name);
                contextName1.setInputType(InputType.TYPE_CLASS_TEXT);
                final EditText latText = location_view.findViewById(R.id.context_latitude);
                final EditText lonText = location_view.findViewById(R.id.context_longitude);
                final EditText radText = location_view.findViewById(R.id.context_radius);
                Button currentLocationButton = location_view.findViewById(R.id.context_current_location);
                currentLocationButton.setOnClickListener(v -> {
                    Location currentLocation = MainActivity.getActivity().mLocationSensor.getLocation();
                    if(currentLocation != null) {
                        latText.setText(Double.toString(currentLocation.getLatitude()), TextView.BufferType.EDITABLE);
                        lonText.setText(Double.toString(currentLocation.getLongitude()), TextView.BufferType.EDITABLE);
                    }
                });
                Button viewLocationButton = location_view.findViewById(R.id.context_view_location);
                viewLocationButton.setOnClickListener(v -> {
                    try {
                        double lat = Double.parseDouble(latText.getText().toString());
                        double lon = Double.parseDouble(lonText.getText().toString());
                        showMap("geo:0,0?q=" + lat + "," + lon + "(View location)");
                    } catch (Exception e) {
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                final AlertDialog alertDialog = builder.create();
                alertDialog.show();

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
                    try {
                        String context_name = contextName1.getText().toString();
                        if(context_name.isEmpty()) {
                            throw new Exception("Context name is null!");
                        }
                        double lat = Double.parseDouble(latText.getText().toString());
                        double lon = Double.parseDouble(lonText.getText().toString());
                        double rad = Double.parseDouble(radText.getText().toString());
                        //double lat = Double.parseDouble()
                        Context c = new LocationContext(context_name, lat, lon, rad);
                        c.addSensor(MainActivity.getActivity().mLocationSensor);
                        c.registerContextListener(MyContextsActivity.this);
                        c.registerContextListener(MainActivity.getActivity());
                        mMyContexts.add(c);
                        mAdapter.updateDataset(mMyContexts.getContexts());
                        runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                        alertDialog.dismiss();
                    } catch (Exception e) {
                        AlertDialog alert = new AlertDialog.Builder(MyContextsActivity.this).create();
                        alert.setTitle("Context cannot be created");
                        alert.setMessage("Check input values");
                        alert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                (dialog, which) -> dialog.dismiss());
                        alert.show();
                    }
                });
                return true;
            case R.id.create_time_context:
                builder = new AlertDialog.Builder(MyContextsActivity.this);
                builder.setTitle("Create time-based context");
                View timeView = builder.create().getLayoutInflater().inflate(R.layout.dialog_time_context, null);
                builder.setView(timeView);
                final EditText timeContextName = timeView.findViewById(R.id.context_name);
                timeContextName.setInputType(InputType.TYPE_CLASS_TEXT);

                final Button startTimeButton = timeView.findViewById(R.id.context_time_start);
                final TextView startTimeView = timeView.findViewById(R.id.context_time_start_view);
                startTimeButton.setOnClickListener(v -> {
                    try {
                        final DialogFragment newFragment = new TimePickerFragment(startTimeView);
                        newFragment.show(getSupportFragmentManager(), "startTimePicker");
                    } catch (Exception e) {
                    }
                });
                final Button endTimeButton = timeView.findViewById(R.id.context_time_end);
                final TextView endTimeView = timeView.findViewById(R.id.context_time_end_view);
                endTimeButton.setOnClickListener(v -> {
                    try {
                        final DialogFragment newFragment = new TimePickerFragment(endTimeView);
                        newFragment.show(getSupportFragmentManager(), "endTimePicker");
                    } catch (Exception e) {
                    }
                });
                final Button dateButton = timeView.findViewById(R.id.context_time_date);
                final TextView dateView = timeView.findViewById(R.id.context_time_date_view);
                dateView.setText(getCurrentDate());
                dateButton.setOnClickListener(v -> {
                    try {
                        final DialogFragment newFragment = new DatePickerFragment(dateView);
                        newFragment.show(getSupportFragmentManager(), "datePicker");
                    } catch (Exception e) {
                    }
                });
                final CheckBox repeatDaily = timeView.findViewById(R.id.context_repeat_daily);
                final CheckBox repeatWeekly = timeView.findViewById(R.id.context_repeat_weekly);
                final CheckBox repeatWeekdays = timeView.findViewById(R.id.context_repeat_weekdays);
                final CheckBox repeatWeekends = timeView.findViewById(R.id.context_repeat_weekends);

                repeatDaily.setOnClickListener(v -> {
                    if(repeatDaily.isChecked()) {
                        repeatWeekly.setChecked(false);
                        repeatWeekends.setChecked(false);
                        repeatWeekdays.setChecked(false);
                    }
                });
                repeatWeekly.setOnClickListener(v -> {
                    if(repeatWeekly.isChecked()) {
                        repeatDaily.setChecked(false);
                        repeatWeekends.setChecked(false);
                        repeatWeekdays.setChecked(false);
                    }
                });
                repeatWeekends.setOnClickListener(v -> {
                    if(repeatWeekends.isChecked()) {
                        repeatDaily.setChecked(false);
                        repeatWeekly.setChecked(false);
                        repeatWeekdays.setChecked(false);
                    }
                });
                repeatWeekdays.setOnClickListener(v -> {
                    if(repeatWeekdays.isChecked()) {
                        repeatDaily.setChecked(false);
                        repeatWeekly.setChecked(false);
                        repeatWeekends.setChecked(false);
                    }
                });
                builder.setPositiveButton("OK", (dialog, which) -> {});
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                final AlertDialog timeDialog = builder.create();
                timeDialog.show();

                timeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
                    try {
                        String name = timeContextName.getText().toString();
                        if(name.isEmpty()) {
                            throw new Exception("Context name cannot be empty!");
                        }


                        int repeat = TimeContext.REPEAT_NONE;
                        if(repeatDaily.isChecked()) {
                            repeat = TimeContext.REPEAT_DAILY;
                        }
                        else if(repeatWeekly.isChecked()) {
                            repeat = TimeContext.REPEAT_WEEKLY;
                        }
                        else if(repeatWeekdays.isChecked()) {
                            repeat = TimeContext.REPEAT_WEEKDAYS;
                        }
                        else if(repeatWeekends.isChecked()) {
                            repeat = TimeContext.REPEAT_WEEKENDS;
                        }

                        String date = dateView.getText().toString();
                        if(date.isEmpty() && (repeat == TimeContext.REPEAT_NONE || repeat == TimeContext.REPEAT_WEEKLY)) {
                            throw new Exception("Date is empty!");
                        } else if(date.isEmpty()) {
                            date = "01.01.2021";
                        }
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm,dd.MM.yyyy");
                        Date startDate = format.parse(startTimeView.getText().toString() + ',' + date);
                        Date endDate = format.parse(endTimeView.getText().toString() + ',' + date);
                        Calendar.getInstance().setTime(startDate);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(startDate);
                        long startTime = calendar.getTimeInMillis();
                        calendar.setTime(endDate);
                        long endTime = calendar.getTimeInMillis();
                        Context c = new TimeContext(null, name, startTime, endTime, repeat);
                        c.addSensor(MainActivity.getActivity().mTimeSensor);
                        c.registerContextListener(MyContextsActivity.this);
                        c.registerContextListener(MainActivity.getActivity());
                        mMyContexts.add(c);
                        mAdapter.updateDataset(mMyContexts.getContexts());
                        runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                        timeDialog.dismiss();
                    } catch (Exception e) {
                        AlertDialog alert = new AlertDialog.Builder(MyContextsActivity.this).create();
                        alert.setTitle("Context cannot be created");
                        alert.setMessage(e.getMessage());
                        alert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                (dialog, which) -> dialog.dismiss());
                        alert.show();
                    }
                });
                return true;
            case R.id.create_wifi_context:
                builder = new AlertDialog.Builder(MyContextsActivity.this);
                builder.setTitle("Create WiFi-based context");
                final View wifiView = builder.create().getLayoutInflater().inflate(R.layout.dialog_wifi_context, null);
                builder.setView(wifiView);
                final EditText wifiContextName = wifiView.findViewById(R.id.context_name);
                wifiContextName.setInputType(InputType.TYPE_CLASS_TEXT);
                final Button wifiButton = wifiView.findViewById(R.id.context_wifi_button);
                final TextView wifiText = wifiView.findViewById(R.id.context_wifi);
                wifiButton.setOnClickListener(v -> {
                    String ssid = MainActivity.getActivity().mWifiSensor.getWifiSSID();
                    wifiText.setText(ssid.equals("<unknown ssid>") ? "No WiFi connection!" : ssid);
                });
                builder.setPositiveButton("OK", (dialog, which) -> {});
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                final AlertDialog wifiDialog = builder.create();
                wifiDialog.show();

                wifiDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
                    try {
                        String context_name = wifiContextName.getText().toString();
                        if(context_name.isEmpty()) {
                            throw new Exception("Context name is empty!");
                        }
                        String ssid = wifiText.getText().toString();
                        if(ssid.isEmpty() || ssid.equals("No WiFi connection!")) {
                            throw new Exception("No active Wifi connections!");
                        }
                        WifiContext c = new WifiContext(context_name, ssid);
                        c.receiveValue(ssid);
                        c.addSensor(MainActivity.getActivity().mWifiSensor);
                        c.registerContextListener(MyContextsActivity.this);
                        c.registerContextListener(MainActivity.getActivity());
                        mMyContexts.add(c);
                        mAdapter.updateDataset(mMyContexts.getContexts());
                        runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                        wifiDialog.dismiss();
                    } catch (Exception e) {
                        AlertDialog alert = new AlertDialog.Builder(MyContextsActivity.this).create();
                        alert.setTitle("Context cannot be created");
                        alert.setMessage(e.getMessage());
                        alert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                (dialog, which) -> dialog.dismiss());
                        alert.show();
                    }
                });
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Implements the ContextLister interface contextChanged method.
     * This is called when context is changed.
     *
     * @param active
     * @see eu.h2020.helios_social.core.context.ContextListener
     */
    @Override
    public void contextChanged(boolean active) {
        runOnUiThread(() -> mAdapter.notifyDataSetChanged());
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private boolean showMap(String uri) {
        Uri gmmIntentUri = Uri.parse(uri);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Log.e(TAG, "Map invocation failed");
            return false;
        }
        return true;
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        private final TextView textView;

        public TimePickerFragment(TextView textView) {
            this.textView = textView;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,true);
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            textView.setText(String.format("%02d:%02d", hourOfDay,minute));
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private final TextView textView;

        public DatePickerFragment(TextView textView) {
            this.textView = textView;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            textView.setText(String.format("%02d.%02d.%04d", day,(month + 1), year));
        }
    }

    private String getCurrentDate() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        return String.format("%02d.%02d.%04d", day,(month + 1), year);
    }

}
