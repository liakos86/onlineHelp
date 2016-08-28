package com.kostas.onlineHelp;

import static com.kostas.service.RunningService.*;
import android.app.*;
import android.content.*;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.custom.NumberPickerKostas;
import com.kostas.custom.ProgressWheel;
import com.kostas.dbObjects.Plan;
import com.kostas.model.Database;
import com.kostas.service.RunningService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Activity for performing a new interval session
 */
public class ActIntervalNew extends BaseFrgActivityWithBottomButtons {

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
    Button buttonSetIntervalValues;
    TextView roundsText, myAddress, timeText, distanceText;
    /**
     * The distance needed to be covered for every interval
     */
    float intervalDistance;
    /**
     *The covered distance in this specific interval (< intervalDistance)
     */
    float coveredDist;
    ViewFlipper flipper;
    CheckBox sound, vibration;
    private Handler mHandler = new Handler();

    /**
     * Pickers for selecting distance, time between intervals, rest start and rounds of interval
     */
    NumberPickerKostas intervalTimePicker, intervalDistancePicker, intervalRoundsPicker, intervalStartRestPicker;
    ProgressWheel progressWheel;
    List<Plan> plans = new ArrayList<Plan>();
    Spinner plansSpinner;
    SpinnerAdapter plansAdapter;
    AdRequest adRequest;
    private BroadcastReceiver receiver;

    /**
     * the metric the user has selected
     */
    private boolean isMiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        ((ExtApplication) getApplication()).setInRunningAct(true);
        setContentView(R.layout.activity_interval);

        flipper = (ViewFlipper) findViewById(R.id.flipper);
        setPlansSpinner();
        setViewsAndButtons();
        getPlansFromDb();
        if (((ExtApplication) getApplication()).isOnline()) {
            new LoadAsync().execute();
        }
        setListeners();

        if (!isMyServiceRunning()) {
            resetAppPrefs();
            startRunningService(false);
            registerReceiver(receiver, new IntentFilter(NOTIFICATION));
        }
    }

    /**
     * A receiver that listens for broadcast messages
     * If the message contains an interval_distance entry I update the current interval info
     * Else it means the interval is over and i prepare for the next one
     */
    private void setBroadcastReceiver() {
        if (receiver != null) {
            return;
        }
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    if (bundle.get(FIRST_LOCATION) !=null ){
                        Gson gson = new Gson();
                        Type locType = new TypeToken<Location>() {}.getType();
                        String locGson = app_preferences.getString(FIRST_LOCATION, "");
                        Location loc =  gson.fromJson(locGson, locType);
                        setAddressText(loc);
                    }
                    else if (bundle.getBoolean(CONNECTION_FAILED)) {
                        Toast.makeText(getApplication(), "Google services api is not correctly configured!", Toast.LENGTH_LONG).show();
                        stopRunningService();
                        clearAndReturnToMain();
                    }
                    else if (bundle.getFloat(INTERVAL_DISTANCE) != 0) {
                        coveredDist = bundle.getFloat(INTERVAL_DISTANCE);

                        if (isMiles){
                            coveredDist /= RunningService.METERS_TO_MILES_CONST;
                        }

                        setDistanceProgress(coveredDist);
                    } else {//todo add another message to identify ending
                        prepareForNextInterval(bundle.getBoolean(INTERVAL_COMPLETED, false));
                    }
                }
            }
        };
    }



    /**
     * Self explanatory
     */
    private void placeAds() {
        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = app_preferences.getString("deviceId", null);
        if (deviceId == null) {
            deviceId = ((ExtApplication) getApplication()).md5(android_id).toUpperCase();
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putString("deviceId", deviceId);
            editor.apply();
        }
        adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(deviceId)
                .build();

    }

    /**
     * Initializes the drop down with the user saved plans
     */
    private void setPlansSpinner() {
        plansSpinner = (Spinner) findViewById(R.id.plansSpinner);
        plansAdapter = new SpinnerAdapter(this, R.layout.custom_plans_spinner_item, plans);
        plansSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListener());
        plansSpinner.setAdapter(plansAdapter);
    }

    private void setViewsAndButtons() {
        app_preferences = this.getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        sound = (CheckBox) findViewById(R.id.checkbox_sound);
        vibration = (CheckBox) findViewById(R.id.checkbox_vibration);
        sound.setChecked(!app_preferences.getBoolean("noSound", false));
        vibration.setChecked(!app_preferences.getBoolean("noVibration", false));
        myAddress = (TextView) findViewById(R.id.myAddressText);
        roundsText = (TextView) findViewById(R.id.roundsText);
        distanceText = ((TextView) findViewById(R.id.distanceText));
        timeText = (TextView) findViewById(R.id.timeText);
        buttonSetIntervalValues = (Button) findViewById(R.id.buttonSetIntervalValues);
        intervalTimePicker = (NumberPickerKostas) findViewById(R.id.intervalTimePicker);
        intervalTimePicker.setValue(10);
        intervalDistancePicker = (NumberPickerKostas) findViewById(R.id.intervalDistancePicker);
        intervalDistancePicker.setValue(50);
        intervalRoundsPicker = (NumberPickerKostas) findViewById(R.id.intervalRoundsPicker);
        intervalStartRestPicker = (NumberPickerKostas) findViewById(R.id.intervalStartRestPicker);
        intervalStartRestPicker.setValue(10);
        progressWheel = (ProgressWheel) findViewById(R.id.timerProgressWheel);


        isMiles = app_preferences.getBoolean(METRIC_MILES, false);
        String distString = isMiles ? getResources().getString(R.string.distance_miles): getResources().getString(R.string.distance_meters);
        intervalDistancePicker.setDescriptionText(distString+" to run");


        TextView distanceTextTitle = ((TextView) findViewById(R.id.distance));

        if (isMiles){
            intervalDistancePicker.setMinValue(0.1f);
            intervalDistancePicker.setValue(0.1f);
            intervalDistancePicker.setMaxValue(3);
            intervalDistancePicker.setStep(0.1f);
            distanceTextTitle.setText("Miles covered");
        }else{

            distanceTextTitle.setText("Meters covered");


        }

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
            progressWheel.setVisibility(View.VISIBLE);
            mHandler.post(mUpdateTimeTask);
        }
        else if (!isRunning && !isCompleted) {//from resume OR first time

            distanceText.setText("0 / " +  intervalDistance);
            timeText.setText(getResources().getString(R.string.zero_time));

            final int step = (int) (360000 / millisecondsToCount);
            progressWheel.setVisibility(View.VISIBLE);
            progressWheel.setText((int) ((millisecondsToCount - (SystemClock.uptimeMillis() - startOfCountdown)) / 1000) + " secs");

            if ((isMyServiceRunning()) && !(app_preferences.getBoolean(INTERVAL_IN_PROGRESS, false))) {
                startRunningService(true);
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
        timeText.setText(getResources().getString(R.string.zero_time));
        distanceText.setText(0 + " / " +  intervalDistance);

        if (countDownTimer !=null) {
            countDownTimer.cancel();
        }

        if (completed) {
            doStop();
            return;
        }

        setRoundsText((int)intervalRoundsPicker.getValue());
        progressWheel.setVisibility(View.VISIBLE);
        progressWheel.setProgress(360);
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
        progressWheel.setText(0.0 + " / " + String.format("%.2f", intervalDistance));
        distanceText.setText(0.0 + " / " +  intervalDistance);
        startTimeMillis = SystemClock.uptimeMillis();
        mHandler.post(mUpdateTimeTask);
        setProgressAndVisibilityTimerAndDistance(View.VISIBLE);
    }

    /**
     * The ui updates after every tick of the countdown timer
     *
     * @param millis the milliseconds left for the interval to start
     * @param step the step to decrease in degrees from the timer progress wheel
     */
    private void onTickUpdate(long millis, int step) {
        progressWheel.setText(millis / 1000 + " secs");
        progressWheel.setProgress((int) (progressWheel.getProgress() - step));
    }

    /**
     * The ui updates after every distance update received from service
     *
     * @param progress the meters covered so far in the interval
     */
    private void setDistanceProgress(float progress) {
        progressWheel.setProgress(((int) ((progress / intervalDistance) * 360)));
        progressWheel.setText( String.format("%.2f", progress) + " / " +  intervalDistance);
        distanceText.setText( String.format("%.2f", progress) + " / " +  intervalDistance);

    }

    /**
     * Clears the app prefs that where changed during the finished interval session
     */
    public void resetAppPrefs() {
        SharedPreferences.Editor editor = app_preferences.edit();
        boolean hasNoSound = app_preferences.getBoolean(NO_SOUND, false);
        boolean hasNoVibration = app_preferences.getBoolean(NO_VIBRATION, false);
        boolean metricMiles = app_preferences.getBoolean(METRIC_MILES, false);
        editor.clear().apply();
        editor.putBoolean(NO_SOUND, hasNoSound);
        editor.putBoolean(NO_VIBRATION, hasNoVibration);
        editor.putBoolean(METRIC_MILES, metricMiles);
        editor.apply();
    }

    /**
     * Clears views and values after finishing an interval session
     */
    private void clearAndReturnToMain() {
        resetAppPrefs();
        ((ExtApplication) getApplication()).setInRunningMode(false);
        ((ExtApplication) getApplication()).setInRunningAct(false);

        intervalDistance = 0;
        intervalTime = 0;
        intervalStartRest = 0 ;
        hideFrame();
        Intent intent = new Intent(this, ActMain.class);
        startActivity(intent);
    }

    /**
     * Assigns listeners for every button in the layout
     */
    public void setListeners() {

        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app_preferences.edit().putBoolean(NO_SOUND, !sound.isChecked()).apply();
            }
        });

        vibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app_preferences.edit().putBoolean(NO_VIBRATION, !vibration.isChecked()).apply();
            }
        });

        buttonSetIntervalValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    obtainUserInput();
                    showFrame();
                } else {
                    Toast.makeText(getApplication(), "Please enable GPS", Toast.LENGTH_SHORT).show();
                }
            }
        });



        ( findViewById(R.id.buttonStart)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ( findViewById(R.id.buttonStart)).setClickable(false);
                getInRunningMode(false, false, true, SystemClock.uptimeMillis(), 0);
            }
        });

        ( findViewById(R.id.buttonStop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmStop();
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
     **/
    private void obtainUserInput() {
            intervalDistance = ((float) intervalDistancePicker.getValue());
            intervalTime = (int)intervalTimePicker.getValue() * 1000;
            intervalStartRest = (int)intervalStartRestPicker.getValue() * 1000;
    }


    private void setRoundsText(int rounds) {

        int size = app_preferences.getInt(COMPLETED_NUM, 0);
        if (rounds > 0) {
            roundsText.setText(size + " / " + rounds);
        } else {
            roundsText.setText(""+size);
        }
    }

    private void setAddressText(Location loc) {

        new LoadAsync(loc).execute();

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

    /**
     * shows the view with the progressWheel
     * and the run info textviews
     * sets textviews to visible because the activity might not have been killed before
     * and those might be hidden from the previous run ending
     */
    private void showFrame() {
        flipper.setDisplayedChild(1);
        setRoundsText((int)intervalRoundsPicker.getValue());
        distanceText.setText("0 / "+intervalDistance);
    }

    private void hideFrame() {
        flipper.setDisplayedChild(0);
    }

    private void confirmStop() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder
                .setMessage("Stop running now?")
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                            doStop();
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
        stopRunningService();

        mHandler.removeCallbacks(mUpdateTimeTask);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        coveredDist = 0;
        if (app_preferences.getBoolean(HAS_RUN_METERS, false) ){
            ((ExtApplication) getApplication()).setInRunningMode(false);
            ((ExtApplication) getApplication()).setInRunningAct(false);
            Intent intentForResultAct = new Intent(this, ActIntervalResults.class);
            startActivity(intentForResultAct);
            finish();
            return;
        }
        resetAppPrefs();

        clearAndReturnToMain();

        finish();
        return;

    }

    /**
     *@param visibility
     */
    private void setProgressAndVisibilityTimerAndDistance(int visibility){
        progressWheel.setProgress(0);
        progressWheel.setVisibility(visibility);
    }

    private void stopRunningService() {
        try {
            stopService(new Intent(getBaseContext(), RunningService.class));
            unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the service but this time with an intent, meaning that now
     * I will start an interval session.
     */
    private void startRunningService(boolean forInterval) {
        setBroadcastReceiver();
        Intent intent = new Intent(getBaseContext(), RunningService.class);
        if (forInterval) {
            intent.putExtra(INTERVAL_DISTANCE, intervalDistance);
            intent.putExtra(INTERVAL_TIME, intervalTime);
            intent.putExtra(INTERVAL_START_REST, intervalStartRest);
            intent.putExtra(INTERVAL_ROUNDS, intervalRoundsPicker.getValue());
        }
        startService(intent);
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
        intervalDistancePicker.setValue(plan.getDistanceUnits());
        intervalRoundsPicker.setValue(plan.getRounds());
        intervalStartRestPicker.setValue(plan.getStartRest());
        intervalRoundsPicker.disableButtonColor(plan.getRounds() == 0);
    }

    class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

            if (pos > 0) loadPlan(pos);

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            Toast.makeText(getApplication(), "nothing", Toast.LENGTH_SHORT).show();
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

    private class LoadAsync extends AsyncTask<Void, Void, Void> {

        Location loc;
        String addressText = "";

        LoadAsync(){};

        LoadAsync(Location loc){
            this.loc = loc;
        }

        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... unused) {

            if (loc != null){
                addressText = getMyLocationAddress(loc.getLatitude(), loc.getLongitude());
                return null;
            }

            placeAds();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            if (loc!=null) {
                myAddress.setText("Currently near " + addressText);
                return;
            }

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
            confirmStop();
            return;
        }
        ((ExtApplication) getApplication()).setInRunningAct(false);
        stopRunningService();
        super.onBackPressed();
    }

    /**
     *  If the service is running and also the mode is INTERVAL_IN_PROGRESS i need to start the receiver,
     *  get the values from app_prefs and fix every ui element based on these.
     **/
    @Override
    public void onResume() {
        super.onResume();
        ( findViewById(R.id.buttonStart)).setClickable(true);
        if (isMyServiceRunning() && (app_preferences.getBoolean(INTERVAL_IN_PROGRESS, false))) {//service is on and i am running
            setBroadcastReceiver();
            intervalDistance = app_preferences.getFloat(INTERVAL_DISTANCE, 0);
            intervalTime = app_preferences.getLong(INTERVAL_TIME, 0);
            intervalStartRest = app_preferences.getLong(INTERVAL_START_REST, 0);
            startTimeMillis = app_preferences.getLong(MSTART_TIME, SystemClock.uptimeMillis());
            //mHandler.post(mUpdateTimeTask);
            coveredDist = app_preferences.getFloat(TOTAL_DIST, 0);
            setRoundsText(app_preferences.getInt(INTERVAL_ROUNDS, 0));
            registerReceiver(receiver, new IntentFilter(NOTIFICATION));
            getInRunningMode(app_preferences.getBoolean(IS_RUNNING, false),
                    app_preferences.getBoolean(INTERVAL_COMPLETED, false),
                    app_preferences.getInt(INTERVAL_ROUNDS, 0) == 0,
                    app_preferences.getLong(MSTART_COUNTDOWN_TIME, SystemClock.uptimeMillis()),
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

}