package com.kostas.service;

import android.app.*;
import android.support.v4.app.NotificationCompat;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.*;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.IntervalActivity;
import com.kostas.onlineHelp.R;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RunningService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static final String INTERVAL_DISTANCE = "intervalDistance";
    public static final String INTERVAL_SPEED = "intervalSpeed";
    public static final String INTERVAL_TIME = "intervalTime";
    public static final String INTERVAL_ROUNDS = "intervalRounds";
    public static final String INTERVAL_COMPLETED = "intervalCompleted";
    public static final String LAST_LOCATION = "lastLocation";
    public static final String LATLONLIST = "latLonList";
    public static final String COMPLETED_NUM = "completed_num";
    public static final String TOTAL_DIST = "totalDist";
    public static final String TOTAL_TIME = "totalTime";
    public static final String MSTART_TIME = "mStartTime";
    public static final String MSTART_COUNTDOWN_TIME = "mStartCountdownTime";
    public static final String INTERVALS = "intervals";
    public static final String NOTIFICATION = "com.kostas.onlineHelp";
    public static final String IS_RUNNING = "is_running";

    public StringBuilder lastStringBuilder;
    public float intervalDistance, currentDistance;
    private long mStartTime, totalTime, intervalTime, interval = 5000, vibrationMillis;
    private int intervalRounds;
    public Location lastLocation;
    SharedPreferences app_preferences;
    SharedPreferences.Editor editor;
    boolean firstChange, hasSound;
    ExtApplication application;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Gson gson;

    PowerManager.WakeLock wl;

    Type listOfObjects, listOfLocations;
    //    Vibrator v;
    int altStart, altFinish, altMax, altMin;
    TTSManager ttsManager;
    List<Location> locationList;

    private Handler mHandler = new Handler();

    //i need the list here. The app might be killed and user might complete several intervals
    private List<Interval> intervals = new ArrayList<Interval>();

    @Override
    protected void onHandleIntent(Intent intent) {}

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");

        wl.acquire();

        application = (ExtApplication) getApplication();
        app_preferences = getApplication().getSharedPreferences(IntervalActivity.PREFS_NAME, Context.MODE_PRIVATE);
        listOfObjects = new TypeToken<List<Interval>>(){}.getType();
        listOfLocations = new TypeToken<List<Location>>(){}.getType();
        locationList = new ArrayList<Location>();
        lastStringBuilder = new StringBuilder();

        buildGoogleApiClient();

        hasSound = !app_preferences.getBoolean("noSound", false);
//        hasVibration = !app_preferences.getBoolean("noVibration", false);
//        v = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);

        ttsManager = application.getTtsManager();

    }

    protected synchronized void buildGoogleApiClient() {
        //i will connect when needed
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {

        //if i request a single update i want only one within 20secs
        //else i want update every 8 secs with min 4 secs
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(4000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setSmallestDisplacement(10);
//        }
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public RunningService() {
        super("RunningService");
    }


    @Override
    public void onStart(Intent intent, int startId) {


        createForegroundNotification();

        if (intent!=null){//first

            intervalTime = intent.getLongExtra(INTERVAL_TIME, 0);
            startCountDownForNextInterval();

                intervals = new ArrayList<Interval>();

                intervalDistance = intent.getFloatExtra(INTERVAL_DISTANCE, 0);

                intervalRounds = intent.getIntExtra(INTERVAL_ROUNDS, 0 );

            new PerformAsyncTask(0).execute();

        }
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public boolean stopService(Intent name) {
        stopLocationUpdates();
        return super.stopService(name);
    }


    @Override
    public void onLocationChanged(Location location) {


            if (!firstChange || lastLocation == null) {
                firstChange = true;

                lastLocation = location;
                locationList.add(location);
                return;
            }

            int altCurrent = (int) location.getAltitude();


            //the first update can be a max of 20m accurate
            // if ( latLonList==null && location.getAccuracy() < 20){

            if (locationList.size() == 1 && location.getAccuracy() < 20) {

                altStart = altCurrent;
                altMax = altCurrent;
                altMin = altCurrent;

                currentDistance = 0;
                locationList.add(location);
                //latLonList= String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
                lastLocation = location;
                return;
            }

            //every following update must be of at least 15m accurate or over 20 m distance to avoid a big straight line of many meters
            if (location.getAccuracy() < 15) {

                mLocationRequest.setFastestInterval(10000);
                mLocationRequest.setInterval(10000);

                //if i have a last & new loc and a right accuracy
                if (lastLocation != null && (location.getTime() > lastLocation.getTime())) {

                    float newDistance = lastLocation.distanceTo(location);
                    currentDistance += newDistance;

                    locationList.add(location);
                    //latLonList+="," + String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
                    totalTime = SystemClock.uptimeMillis() - mStartTime;

                    if (altCurrent > altMax) altMax = altCurrent;
                    else if (altCurrent < altMin) altMin = altCurrent;

                    if (currentDistance >= intervalDistance) {
//                play( intervalRounds>0 && intervals.size()+1>=intervalRounds, false);
                        altFinish = (int) location.getAltitude();
                        finishInterval();
                    } else if (currentDistance > 0) {
                        lastStringBuilder.setLength(0);
                        lastStringBuilder.append(lastLocation.getLatitude() + "," + lastLocation.getLongitude() + "," + location.getLatitude() + "," + location.getLongitude());
                        refreshInterval((location.getTime() - lastLocation.getTime()) / newDistance);
                    }
                }

                if (lastLocation == null || (location.getTime() > lastLocation.getTime())) lastLocation = location;

                mLocationRequest.setFastestInterval(interval - 1000);
                mLocationRequest.setInterval(interval);

            }


    }


    private void createForegroundNotification() {


        final int myID = 1234;

        if (Build.VERSION.SDK_INT > 15) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(application);
            mBuilder.setSmallIcon(R.drawable.interval_flag);
            mBuilder.setContentTitle("Interval in progress");
            mBuilder.setContentText("Click to get into");
//            mBuilder.setOngoing(true);

            Intent resultIntent = new Intent(application, IntervalActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(application);
            stackBuilder.addParentStack(IntervalActivity.class);

// Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);


            Notification notification = mBuilder.build();

            notification.ledARGB = getResources().getColor(R.color.interval_green);
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.ledOffMS = 1000;
            notification.ledOnMS = 1500;


//            NotificationManager notificationManager =
//                    (NotificationManager) application.getSystemService(android.content.Context.NOTIFICATION_SERVICE);


            // notificationID allows you to update the notification later on.
            startForeground(myID, notification);
//            notificationManager.notify(123, notification);
        } else {


//The intent to launch when the user clicks the expanded notification
            Intent intent2 = new Intent(this, IntervalActivity.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent2, 0);

//This constructor is deprecated. Use Notification.Builder instead
            Notification notice = new Notification(R.drawable.interval_flag, "Interval in progress", System.currentTimeMillis());

//This method is deprecated. Use Notification.Builder instead.
            notice.setLatestEventInfo(this, "Click to get into", "Running", pendIntent);

            notice.flags |= Notification.FLAG_NO_CLEAR;
            startForeground(myID, notice);
        }


    }


    private void finishInterval() {

        stopLocationUpdates();


//        intervals.add(new Interval(-1,latLonList, totalTime, intervalDistance, altStart, altFinish, altMax, altMin));

        intervals.add(new Interval(-1, locationList, totalTime, intervalDistance, altStart, altFinish, altMax, altMin));


        totalTime = 0;
        currentDistance = 0;
//        latLonList = null;

        locationList.clear();
        firstChange = false;


//        new PerformAsyncTask(3).execute();

        boolean completed = intervalRounds > 0 && intervals.size() >= intervalRounds;

        editor.putBoolean(INTERVAL_COMPLETED, completed);
        editor.putInt(COMPLETED_NUM, intervals.size());


        editor.putBoolean(IS_RUNNING, false);
        editor.putFloat(TOTAL_DIST, 0);
        editor.putFloat(TOTAL_TIME, 0);
//        editor.putString(LATLONLIST,null);

        String json = (new Gson()).toJson(intervals, listOfObjects);
        editor.putString(INTERVALS, json);
        editor.apply();

        if (((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn()) {

            Intent intent = new Intent(NOTIFICATION);
            intent.putExtra(INTERVAL_COMPLETED, completed);

            sendBroadcast(intent);
        }


        if (!completed) {

            startCountDownForNextInterval();
            speak("STOPPED");
//            ttsManager.initQueue("STOPPED");
//            textToSpeech.speak("STOPPED", TextToSpeech.QUEUE_FLUSH, null);

//            while (textToSpeech.isSpeaking()){
//
//            }


        } else {

//            textToSpeech.speak("COMPLETED", TextToSpeech.QUEUE_FLUSH, null);
            speak("COMPLETED");
//            ttsManager.initQueue("COMPLETED");
//            while (textToSpeech.isSpeaking()){
//
//            }
        }


    }

    private void speak(String textToSpeak) {

        application.getTtsManager().initQueue(textToSpeak);

    }


    private void refreshInterval(float pace) {

        interval = (100 * pace / 6) > 3000 ? ((100 * pace / 6) < 6000 ? (long) (100 * pace / 6) : 6000) : 3000;

//        storeSharedPrefsValues(2);
        new PerformAsyncTask(2).execute();

    }


    private void startCountDownForNextInterval() {

        mHandler.postDelayed(mStartRunnable, intervalTime);

        editor = app_preferences.edit();
        editor.putLong(MSTART_COUNTDOWN_TIME, SystemClock.uptimeMillis());
        editor.apply();

    }


    @Override
    public void onDestroy() {

        try {
            stopLocationUpdates();
           if (wl.isHeld()) wl.release();

            if (ttsManager != null) {
                ttsManager.shutDown();
            }

            mHandler.removeCallbacks(mStartRunnable);


        } catch (Exception e) {
            Log.v("LATLNG", "Crash");

        }finally{
            super.onDestroy();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private Runnable mStartRunnable = new Runnable() {

        public void run() {

            mStartTime = SystemClock.uptimeMillis();

            editor.putBoolean(IS_RUNNING, true);
            editor.putLong(MSTART_TIME, mStartTime);
            editor.apply();

            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            } else {
                startReceiving();
            }

            speak("STARTED");

        }
    };

    @Override
    public void onConnected(Bundle bundle) {

        createLocationRequest();
        startReceiving();

    }

    private void startReceiving() {

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else
            Toast.makeText(application, "google client not connected", Toast.LENGTH_SHORT).show();
    }

    protected void stopLocationUpdates() {

        if (mGoogleApiClient.isConnected()) LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Google services api failed to connect", Toast.LENGTH_SHORT).show();
    }

    private class PerformAsyncTask extends AsyncTask<Void, Void, Void> {

        int type;

        public PerformAsyncTask(int type) {
            this.type = type;

        }

        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... unused) {


            editor = app_preferences.edit();

            if (type == 0) {

                editor.putFloat(INTERVAL_DISTANCE, intervalDistance);
                editor.putLong(INTERVAL_TIME, intervalTime);
                editor.putInt(INTERVAL_ROUNDS, intervalRounds);

                editor.apply();

            } else if (type == 2) {


                if (((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn()) {
                    Intent intent = new Intent(NOTIFICATION);
                    intent.putExtra(INTERVAL_DISTANCE, currentDistance);
                    intent.putExtra(INTERVAL_TIME, totalTime);
//                    intent.putExtra(LAST_LOCATION, lastString);
                    intent.putExtra(LAST_LOCATION, lastStringBuilder.toString());

                    sendBroadcast(intent);
                }

                editor.putFloat(TOTAL_DIST, currentDistance);
                editor.putLong(TOTAL_TIME, totalTime);

                String json = (new Gson()).toJson(locationList, listOfLocations);
                editor.putString(LATLONLIST, json);

//                editor.putString(LATLONLIST, latLonList);

                editor.apply();

            } else if (type == 3) {


                boolean completed = intervalRounds > 0 && intervals.size() >= intervalRounds;

                editor.putBoolean(INTERVAL_COMPLETED, completed);
                editor.putInt(COMPLETED_NUM, intervals.size());


                editor.putBoolean(IS_RUNNING, false);
                editor.putFloat(TOTAL_DIST, 0);
                editor.putFloat(TOTAL_TIME, 0);
                editor.putString(LATLONLIST, null);

                String json = (new Gson()).toJson(intervals, listOfObjects);
                editor.putString(INTERVALS, json);
                editor.apply();

                if (((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn()) {

                    Intent intent = new Intent(NOTIFICATION);
                    intent.putExtra(INTERVAL_COMPLETED, completed);

                    sendBroadcast(intent);
                }

            }

//            editor.commit();

            return null;

        }

        @Override
        protected void onPostExecute(Void result) {

        }

    }


}