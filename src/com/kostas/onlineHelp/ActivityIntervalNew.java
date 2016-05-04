package com.kostas.onlineHelp;

import android.app.*;
import android.content.*;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Plan;
import com.kostas.dbObjects.Running;
import com.kostas.model.Database;
import com.kostas.service.RunningService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Activity for performing a new interval session
 */
public class ActivityIntervalNew extends BaseFrgActivityWithBottomButtons {

    /**
     * The starting timer text
     */
    private final String ZERO_TIME = "00 : 00 : 00";

    /**
     * Timer for counting down for the next interval round
     */
    CountDownTimer countDownTimer;

    /**
     * The time spent on the interval / start rest seconds / the start time of the interval
     * I do not use the picker values everywhere because the app may get killed and i will retrieve from shared prefs
     */
    private long intervalTime, intervalStartRest, startTimeMillis;
    SharedPreferences app_preferences;
    RelativeLayout textsInfoRun;
    Button buttonSetIntervalValues, buttonDismiss, buttonSave;
    LinearLayout layoutBottomButtons;
    TextView roundsText, myAddress, timeText;
    float intervalDistance, coveredDist;
    ViewFlipper flipper;
    CheckBox sound, vibration;
    private Handler mHandler = new Handler();

    /**
     * Pickers for selecting distance, time between intervals, rest start and rounds of interval
     */
    NumberPickerKostas intervalTimePicker, intervalDistancePicker, intervalRoundsPicker, intervalStartRestPicker;
    ProgressWheel timerProgressWheel, distanceProgressWheel;
    ListView completedIntervalsListView;
    List<Interval> intervalsList;
    List<Plan> plans = new ArrayList<Plan>();
    IntervalAdapterItem adapterInterval;
    Spinner plansSpinner;
    SpinnerAdapter plansAdapter;
    AdRequest adRequest, adRequest2;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        ((ExtApplication) getApplication()).setInRunningAct(true);
        setContentView(R.layout.activity_interval);

        flipper = (ViewFlipper) findViewById(R.id.flipper);
        flipper.setDisplayedChild(2);
        setPlansSpinner();
        setViewsAndButtons();
        getPlansFromDb();
        new LoadAsyncAds().execute();
        setListeners();

        flipper.setDisplayedChild(0);
    }

    /**
     * A receiver that listens for broadcast messages
     * If the message contains an interval_distance entry I update the current interval info
     * Else it means the interval is over and i prepare for the next one
     */
    private void setBroadcastReceiver() {
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();
                if (bundle != null) {

                    if (bundle.getFloat(RunningService.INTERVAL_DISTANCE) != 0) {
                        coveredDist = bundle.getFloat(RunningService.INTERVAL_DISTANCE);
                        setDistanceProgress(coveredDist);
                    } else {//todo add another message to identify ending
                        prepareForNextInterval(bundle.getBoolean(RunningService.INTERVAL_COMPLETED, false));
                    }
                }
            }
        };
    }

    /**
     * For testing purposes returns an md5 hash of the device to add testing ads
     * @param s
     * @return
     */
    public static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            //Log.v("SERVICE",e.getMessage());
        }
        return "";
    }

    /**
     * Self explanatory
     */
    private void placeAds() {
        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = app_preferences.getString("deviceId", null);
        if (deviceId == null) {
            deviceId = md5(android_id).toUpperCase();
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putString("deviceId", deviceId);
            editor.apply();
        }
        adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(deviceId)
                .build();
        adRequest2 = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(deviceId)
                .build();
    }

    /**
     * Initializes the drop down with the user saved plans
     */
    private void setPlansSpinner() {
        plansSpinner = (Spinner) findViewById(R.id.plansSpinner);
        plansAdapter = new SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, plans);
        plansSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListener());
        plansSpinner.setAdapter(plansAdapter);

        if (Build.VERSION.SDK_INT > 15) {
            Display display = this.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int _width = size.x;
            plansSpinner.setDropDownWidth(_width - 10);
        }

    }

    private void setViewsAndButtons() {

        app_preferences = this.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);

        sound = (CheckBox) findViewById(R.id.checkbox_sound);
        vibration = (CheckBox) findViewById(R.id.checkbox_vibration);

        sound.setChecked(!app_preferences.getBoolean("noSound", false));
        vibration.setChecked(!app_preferences.getBoolean("noVibration", false));

        //adView = (AdView) findViewById(R.id.adViewInterval);
        //adView2 = (AdView) findViewById(R.id.adViewInterval2);
        intervalsList = new ArrayList<Interval>();
        myAddress = (TextView) findViewById(R.id.myAddressText);
        roundsText = (TextView) findViewById(R.id.roundsText);
        timeText = (TextView) findViewById(R.id.timeText);
        buttonSetIntervalValues = (Button) findViewById(R.id.buttonSetIntervalValues);
//        buttonStart = (ImageButton) findViewById(R.id.buttonStart);
//        buttonStop = (ImageButton) findViewById(R.id.buttonStop);
//        buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        intervalTimePicker = (NumberPickerKostas) findViewById(R.id.intervalTimePicker);
        intervalTimePicker.setValue(10);
        intervalDistancePicker = (NumberPickerKostas) findViewById(R.id.intervalDistancePicker);
        intervalDistancePicker.setValue(50);
        intervalRoundsPicker = (NumberPickerKostas) findViewById(R.id.intervalRoundsPicker);
        intervalStartRestPicker = (NumberPickerKostas) findViewById(R.id.intervalStartRestPicker);
        intervalStartRestPicker.setValue(10);
        timerProgressWheel = (ProgressWheel) findViewById(R.id.timerProgressWheel);
        distanceProgressWheel = (ProgressWheel) findViewById(R.id.distanceProgressWheel);
        completedIntervalsListView = (ListView) findViewById(R.id.completedIntervals);
        layoutBottomButtons = (LinearLayout) findViewById(R.id.layoutBottomButtons);
        buttonDismiss = (Button) findViewById(R.id.buttonDismissInterval);
        buttonSave = (Button) findViewById(R.id.buttonSaveRunWithIntervals);
        textsInfoRun = (RelativeLayout) findViewById(R.id.textsInfoRun);

        adapterInterval = new IntervalAdapterItem(this, this.getApplicationContext(),
                R.layout.list_interval_row, intervalsList);
        completedIntervalsListView.setAdapter(adapterInterval);
    }

    /**
     * Checks if RunningService is between the amount of the services running in the device
     *
     * @return
     */
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RunningService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**the first time and every time we come back from resume
     *
     * @param isRunning true if the app is in running/interval mode
     * @param isCompleted true if user has achieved the number of intervals selected
     * @param startOfCountdown the start time milliseconds of the interval
     * @param distanceCovered the distance covered so far in the interval
     */
    private void getInRunningMode(boolean isRunning, boolean isCompleted, boolean isFirst, long startOfCountdown, float distanceCovered) {
        ((ExtApplication) getApplication()).setInRunningMode(true);
        flipper.setDisplayedChild(1);
        setButtonVisibilities(true);

        long millisecondsToCount = isFirst ? intervalStartRest : intervalTime;

        if (isRunning && !isCompleted) {//from resume
            setDistanceProgress(distanceCovered);
            timerProgressWheel.setVisibility(View.INVISIBLE);
            distanceProgressWheel.setVisibility(View.VISIBLE);
            mHandler.post(mUpdateTimeTask);
        }
        else if (!isRunning && !isCompleted) {//from resume OR first time
            final int step = (int) (360000 / millisecondsToCount);
            distanceProgressWheel.setVisibility(View.INVISIBLE);
            timerProgressWheel.setVisibility(View.VISIBLE);
            timerProgressWheel.setText((int) ((millisecondsToCount - (SystemClock.uptimeMillis() - startOfCountdown)) / 1000) + " secs");

            if (!(isMyServiceRunning())) {
                startRunningService();
            }

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            countDownTimer = new CountDownTimer(millisecondsToCount - (SystemClock.uptimeMillis() - startOfCountdown), 1000) {
                public void onTick(long millisUntilFinished) {
                    onTickUpdate(millisUntilFinished, step);
                }
                public void onFinish() {
                    onFinishUpdate();
                }
            }.start();
        }
        else {//from resume(isCompleted=true)
            doStop();
        }
    }

    /**
     * Sets the timer textView text based on the diff of the startTime and current time
     */
    private void setTimerText() {
        long total = SystemClock.uptimeMillis() - startTimeMillis;
        int hours = (int) (total / 3600000);
        int mins = (int) ((total - (hours * 3600000)) / 60000);
        int secs = (int) ((total - (hours * 3600000) - (mins * 60000)) / 1000);
        timeText.setText(String.format("%02d", hours) + " : " + String.format("%02d", mins) + " : " + String.format("%02d", secs));
    }

    /**
     * Clears textfields and timers and starts countdown if there are more intervals
     *
     * @param completed indication if user has completed all interval sessions
     */
    private void prepareForNextInterval(boolean completed) {
        coveredDist = 0;
        mHandler.removeCallbacks(mUpdateTimeTask);
        timeText.setText(ZERO_TIME);

        if (countDownTimer !=null) {
            countDownTimer.cancel();
        }

        if (completed) {
            doStop();
            return;
        }

        setRoundsText(intervalRoundsPicker.getValue());
        distanceProgressWheel.setVisibility(View.INVISIBLE);
        timerProgressWheel.setVisibility(View.VISIBLE);
        final int step = (int) (360000 / intervalTime);
        countDownTimer = new CountDownTimer(intervalTime, 1000) {
            public void onTick(long millisUntilFinished) {
                onTickUpdate(millisUntilFinished, step);
            }
            public void onFinish() {
                onFinishUpdate();
            }
        }.start();
    }

    /**
     * Actions performed when the countdown finishes and the interval will start.
     */
    private void onFinishUpdate() {
        distanceProgressWheel.setText(0 + " / " + (int) intervalDistance);
        startTimeMillis = SystemClock.uptimeMillis();
        mHandler.post(mUpdateTimeTask);
        setProgressAndVisibilityTimerAndDistance(0, View.INVISIBLE, 0, View.VISIBLE);
    }

    /**
     * The ui updates after every tick of the countdown timer
     *
     * @param millis the milliseconds left for the interval to start
     * @param step the step to decrease in degrees from the timer progress wheel
     */
    private void onTickUpdate(long millis, int step) {
        timerProgressWheel.setText(millis / 1000 + " secs");
        timerProgressWheel.setProgress((int) (timerProgressWheel.getProgress() - step));
    }

    /**
     * The ui updates after every distance update received from service
     *
     * @param progress the meters covered so far in the interval
     */
    private void setDistanceProgress(float progress) {
        distanceProgressWheel.setProgress(((int) ((progress / intervalDistance) * 360)));
        distanceProgressWheel.setText((int) progress + " / " + (int) intervalDistance);
    }

    /**
     * Clears the app prefs that where changed during the finished interval session
     */
    public void resetAppPrefs() {
        SharedPreferences.Editor editor = app_preferences.edit();

        boolean hasNoSound = app_preferences.getBoolean(RunningService.NO_SOUND, false);
        boolean hasNoVibration = app_preferences.getBoolean(RunningService.NO_VIBRATION, false);

        editor.clear().apply();

        editor.putBoolean(RunningService.NO_SOUND, hasNoSound);
        editor.putBoolean(RunningService.NO_VIBRATION, hasNoVibration);
        editor.apply();
    }

    /**
     * Clears views and values after finishing an interval session
     */
    private void clear() {
        ((ExtApplication)getApplication()).setInRunningMode(false);
        myAddress.setVisibility(View.INVISIBLE);
        completedIntervalsListView.setVisibility(View.GONE);
        intervalDistance = 0;
        intervalTime = 0;
        intervalStartRest = 0 ;
        intervalsList.clear();
        findViewById(R.id.buttonBack).setVisibility(View.VISIBLE);
        clearViews();
        hideFrame();
    }

    /**
     * Default values for texts
     */
    private void setInitialTextInfo() {
        timeText.setText(ZERO_TIME);
        roundsText.setText("0 / 0");
    }

    /**
     * Assigns listeners for every button in the layout
     */
    public void setListeners() {

        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app_preferences.edit().putBoolean(RunningService.NO_SOUND, !sound.isChecked()).apply();
            }
        });

        vibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app_preferences.edit().putBoolean(RunningService.NO_VIBRATION, !vibration.isChecked()).apply();
            }
        });

        buttonSetIntervalValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && obtainUserInput()) {
                    if (isMyServiceRunning()) {
                        stopRunningService();
                    }
                    showFrame();
                } else if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)))
                    Toast.makeText(getApplication(), "Please enable GPS", Toast.LENGTH_SHORT).show();
            }
        });

        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmStopOrDelete(false);
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveRunWithIntervalsDB();
            }
        });

        ( findViewById(R.id.buttonStart)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getInRunningMode(false, false, true, SystemClock.uptimeMillis(), 0);
            }
        });

        ( findViewById(R.id.buttonStop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmStopOrDelete(true);
            }
        });

        ( findViewById(R.id.buttonBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideFrame();
            }
        });
    }

    /**
     * Fetches the user plans from the db
     *
     */
    public void getPlansFromDb() {
        Database db = new Database(this);
        plans.clear();
        List<Plan> newPlans = db.fetchPlansFromDb();
        plans.add(new Plan("Select a plan"));
        for (Plan plan : newPlans) {
            plans.add(plan);
        }
        plansAdapter.notifyDataSetChanged();
        plansSpinner.setClickable(true);
    }

    /**
     * Obtains input values from user selections
     *
     * @return true if all values are retrieved successfully
     */
    private boolean obtainUserInput() {
        try {
            intervalDistance = ((float) intervalDistancePicker.getValue());
            intervalTime = intervalTimePicker.getValue() * 1000;
            intervalStartRest = intervalStartRestPicker.getValue() * 1000;
            CheckBox sound = (CheckBox) findViewById(R.id.checkbox_sound);
            CheckBox vibration = (CheckBox) findViewById(R.id.checkbox_vibration);
            Boolean soundOn = sound.isChecked();
            Boolean vibrationOn = vibration.isChecked();
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putBoolean("noSound", !soundOn);
            editor.putBoolean("noVibration", !vibrationOn);
            editor.apply();
        } catch (Exception e) {
            Toast.makeText(getApplication(), "wrong data", Toast.LENGTH_LONG).show();
            return false;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(buttonSetIntervalValues.getWindowToken(), 0);
        return true;
    }

    /**
     * Reenter every interval in the list and notify adapter
     *
     * @param intervals
     */
    private void fixListAndAdapter(List<Interval> intervals) {
        if (intervals != null && intervals.size() > 0) {
            intervalsList.clear();
            for (Interval interval : intervals) {
                intervalsList.add(interval);
            }
            adapterInterval.notifyDataSetChanged();
        }
    }

    private void setRoundsText(int rounds) {
        int size = app_preferences.getInt(RunningService.COMPLETED_NUM, 0);
        if (rounds > 0) {
            roundsText.setText(size + " / " + rounds + " comp");
        } else {
            roundsText.setText(size + " comp");
        }
    }

    /**
     *  If the service is running and also the mode is INTERVAL_IN_PROGRESS i need to start the receiver,
     *  get the values from app_prefs and fix every ui element based on these.
     **/
    @Override
    public void onResume() {
        super.onResume();
        //if (isMyServiceRunning() && (app_preferences.getBoolean(RunningService.INTERVAL_IN_PROGRESS, false))) {//service is on and i am running
        if (isMyServiceRunning()){
            if (receiver == null) {
                setBroadcastReceiver();
            }
            intervalDistance = app_preferences.getFloat(RunningService.INTERVAL_DISTANCE, 0);
            intervalTime = app_preferences.getLong(RunningService.INTERVAL_TIME, 0);
            intervalStartRest = app_preferences.getLong(RunningService.INTERVAL_START_REST, 0);
            startTimeMillis = app_preferences.getLong(RunningService.MSTART_TIME, SystemClock.uptimeMillis());
            //mHandler.post(mUpdateTimeTask);
            coveredDist = app_preferences.getFloat(RunningService.TOTAL_DIST, 0);
            setRoundsText(app_preferences.getInt(RunningService.INTERVAL_ROUNDS, 0));
            registerReceiver(receiver, new IntentFilter(RunningService.NOTIFICATION));
            getInRunningMode(app_preferences.getBoolean(RunningService.IS_RUNNING, false),
                    app_preferences.getBoolean(RunningService.INTERVAL_COMPLETED, false),
                    app_preferences.getInt(RunningService.INTERVAL_ROUNDS, 0) == 0,
                    app_preferences.getLong(RunningService.MSTART_COUNTDOWN_TIME, SystemClock.uptimeMillis()),
                    coveredDist);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) countDownTimer.cancel();
        try {
            if (isMyServiceRunning()) {
                mHandler.removeCallbacks(mUpdateTimeTask);
                unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {

//        try {
//            if (isMyServiceRunning())
//                getActivity().unregisterReceiver(receiver);
//                receiver=null;
//        }catch(Exception e){
//            //Log.v("LATLNG", "Exception: Receiver not registered");
//            e.printStackTrace();
//        }

        super.onDestroy();
    }

    @Override
    public void onStart() {
        //Log.v("LIFECYCLE", "START");
        super.onStart();
    }

    private void setAddressText(Location loc) {
        if (myAddress.getVisibility() == View.INVISIBLE) {
            myAddress.setText("Currently near " + getMyLocationAddress(loc.getLatitude(), loc.getLongitude()));
            myAddress.setVisibility(View.VISIBLE);
        }
    }

    private void clearViews() {


        setButtonVisibilities(false);


        layoutBottomButtons.setVisibility(View.INVISIBLE);
        findViewById(R.id.adViewInterval2).setVisibility(View.INVISIBLE);
        timerProgressWheel.setVisibility(View.INVISIBLE);
        distanceProgressWheel.setVisibility(View.INVISIBLE);

    }


    private void setButtonVisibilities(boolean startsNow) {

        if (startsNow) {

            findViewById(R.id.buttonStart).setVisibility(View.INVISIBLE);
            findViewById(R.id.buttonStop).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonBack).setVisibility(View.INVISIBLE);
        } else {

            findViewById(R.id.buttonStart).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonStop).setVisibility(View.INVISIBLE);
            findViewById(R.id.buttonBack).setVisibility(View.VISIBLE);
        }
    }

    private void showFrame() {

        flipper.setDisplayedChild(1);

        buttonSave.setClickable(true);
        buttonDismiss.setClickable(true);

        setRoundsText(intervalRoundsPicker.getValue());
        textsInfoRun.setVisibility(View.VISIBLE);

    }

    private void hideFrame() {
        flipper.setDisplayedChild(0);
        textsInfoRun.setVisibility(View.INVISIBLE);
    }

    private void confirmStopOrDelete(final boolean isStop) {

        String title = isStop ? "Quit Interval" : "Delete Interval";
        String message = isStop ? "Stop running now?" : "Delete current progress?";

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if (isStop)
                            doStop();
                        else
                            clear();

                    }

                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Stops the interval after user selection or after user has completed the selected
     * amount of intervals.
     *
     */
    private void doStop() {
        Gson gson = new Gson();
        Type listOfObjects = new TypeToken<List<Interval>>() {}.getType();
        String intervalsGson = app_preferences.getString(RunningService.INTERVALS, "");
        List<Interval> intervals = (List<Interval>) gson.fromJson(intervalsGson, listOfObjects);
        fixListAndAdapter(intervals);
        setInitialTextInfo();
        setProgressAndVisibilityTimerAndDistance(0, View.GONE, 0, View.GONE);

        if (intervalsList.size() == 0 && coveredDist == 0) {
            clear();
        }
        else if (coveredDist > 0) {

            if (app_preferences.getString(RunningService.LATLONLIST, "").length() < 2)
                Toast.makeText(getApplication(), "WILL CRUSH EMPTY LIST", Toast.LENGTH_LONG).show();

            Type listOfLocation = new TypeToken<List<Location>>() {
            }.getType();
            String locsGson = app_preferences.getString(RunningService.LATLONLIST, "");
            List<Location> locationList = (List<Location>) gson.fromJson(locsGson, listOfLocation);
            StringBuilder sb = new StringBuilder(locationList.get(0).getLatitude() + "," + locationList.get(0).getLongitude());
            locationList.remove(0);
            for (Location l : locationList) {
                sb.append("," + l.getLatitude() + "," + l.getLongitude() + "," + l.getLatitude() + "," + l.getLongitude());
            }
            intervalsList.add(new Interval(-1, sb.toString(), SystemClock.uptimeMillis() - startTimeMillis, coveredDist));
        }

        mHandler.removeCallbacks(mUpdateTimeTask);
        stopRunningService();
        if (countDownTimer != null)
            countDownTimer.cancel();
        resetAppPrefs();

        if (intervalsList.size() > 0) {
            findViewById(R.id.buttonStop).setVisibility(View.GONE);
            layoutBottomButtons.setVisibility(View.VISIBLE);
            findViewById(R.id.adViewInterval2).setVisibility(View.VISIBLE);
            textsInfoRun.setVisibility(View.GONE);
            completedIntervalsListView.setVisibility(View.VISIBLE);
        }
        coveredDist = 0;
    }

    /**
     *
     * @param timeProg
     * @param timeVis
     * @param distProg
     * @param distVis
     */
    private void setProgressAndVisibilityTimerAndDistance(int timeProg, int timeVis, int distProg, int distVis){
        timerProgressWheel.setProgress(timeProg);
        timerProgressWheel.setVisibility(timeVis);
        distanceProgressWheel.setProgress(distProg);
        distanceProgressWheel.setVisibility(distVis);
    }

    /**
     * Save the run and every interval in the list in the db
     */
    private void saveRunWithIntervalsDB() {
        buttonDismiss.setClickable(false);
        buttonSave.setClickable(false);
        Running running = new Running(-1, "", intervalTime, new SimpleDateFormat("dd/MM/yyyy, hh:mm a").format(new Date()), intervalDistance, intervalsList);
        Database db = new Database(getApplicationContext());
        db.addRunning(running);
        ((ExtApplication) getApplication()).setNewIntervalInDb(true);
        Toast.makeText(getApplication(), "Saved in Diary", Toast.LENGTH_SHORT).show();
        clear();
    }

    private void stopRunningService() {
        try {
            stopService(new Intent(getBaseContext(), RunningService.class));
            unregisterReceiver(receiver);
        } catch (Exception e) {
            //Log.v("LATLNG", "Exception: Receiver was not registered");
        }
    }


    private void startRunningService() {

        setBroadcastReceiver();


        //Log.v("LATLNG", "START SERVICE");
        Intent intent = new Intent(getBaseContext(), RunningService.class);
        intent.putExtra(RunningService.INTERVAL_DISTANCE, intervalDistance);
        intent.putExtra(RunningService.INTERVAL_TIME, intervalTime);
        intent.putExtra(RunningService.INTERVAL_START_REST, intervalStartRest);
        intent.putExtra(RunningService.INTERVAL_ROUNDS, intervalRoundsPicker.getValue());
        startService(intent);

//        if (firstTime)
        registerReceiver(receiver, new IntentFilter(RunningService.NOTIFICATION));


    }


    public String getMyLocationAddress(double lat, double lon) {

        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);

        try {

            //Place your latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

            if ((addresses != null) && (addresses.size() > 0)) {

                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder();

                for (int i = 0; i < fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
                }
                return strAddress.toString();

            } else return "No address for this location";

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "No address for this location";
        }
    }

    private void loadPlan(int position) {
        Plan plan = plans.get(position);
        intervalTimePicker.setValue(plan.getSeconds());
        intervalDistancePicker.setValue(plan.getMeters());
        intervalRoundsPicker.setValue(plan.getRounds());
        intervalStartRestPicker.setValue(plan.getStartRest());
        intervalRoundsPicker.disableButtonColor(plan.getRounds() == 0);

        Toast.makeText(getApplication(), plan.getDescription() + " loaded", Toast.LENGTH_SHORT).show();
    }

    class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

            if (pos > 0) loadPlan(pos);

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            Toast.makeText(getApplication(), "nothing", Toast.LENGTH_SHORT).show();
            // Nothing
        }

    }


    class SpinnerAdapter extends ArrayAdapter<Plan> {

        int layoutResourceId;
        List<Plan> data;

        public SpinnerAdapter(Context ctx, int layoutResourceId, List<Plan> data) {
            super(ctx, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.data = data;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomDropView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }


        //this is the spinner drop
        public View getCustomDropView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =getLayoutInflater();
            planViewHolder holder;
            if (position > 0 && (convertView == null || !(convertView.getTag() instanceof planViewHolder))) {

                holder = new planViewHolder();
                convertView = inflater.inflate(R.layout.custom_plans_spinner_item, null);
                holder.description = (TextView) convertView.findViewById(R.id.planDescriptionSpinner);

                convertView.setTag(holder);
            } else if (position == 0) {
                holder = new planViewHolder();
                convertView = inflater.inflate(R.layout.zero_height_spinner_item, null);
                holder.description = (TextView) convertView.findViewById(R.id.planDescriptionSpinner);
            } else {
                holder = (planViewHolder) convertView.getTag();
            }

            holder.description.setText(data.get(position).getDescription());

            return convertView;
        }

        //this is the spinner header
        public View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            planViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof planViewHolder)) {
                convertView = inflater.inflate(R.layout.custom_spinner_header, null);
                holder = new planViewHolder();
                holder.description = (TextView) convertView.findViewById(R.id.planDescriptionSpinner2);
                holder.arrow = (ImageView) convertView.findViewById(R.id.planArrow);

                convertView.setTag(holder);
            } else {
                holder = (planViewHolder) convertView.getTag();
            }
            holder.description.setText(data.get(position).getDescription());
            return convertView;
        }
    }

    private class planViewHolder {
        TextView description;
        ImageView arrow;
    }


    private class LoadAsyncAds extends AsyncTask<Void, Void, Void> {

        protected void onPreExecute() {
            plansSpinner.setClickable(false);
        }

        @Override
        protected Void doInBackground(Void... unused) {
            placeAds();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ((AdView)findViewById(R.id.adViewInterval2)).loadAd(adRequest2);
            ((AdView)findViewById(R.id.adViewInterval)).loadAd(adRequest);
        }

    }

    private Runnable mUpdateTimeTask = new Runnable() {

        public void run() {
            setTimerText();
            mHandler.postDelayed(this, 1000);
        }
    };


    @Override
    public void onBackPressed() {

        if (((ExtApplication) getApplication()).isInRunningMode()){
            Toast.makeText(getApplication(), "You are running", Toast.LENGTH_SHORT).show();
            return;
        }

        ((ExtApplication) getApplication()).setInRunningAct(false);
        super.onBackPressed();

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);



    }
}