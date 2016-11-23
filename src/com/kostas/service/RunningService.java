package com.kostas.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.onlineHelp.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RunningService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    /**
     * The DURATION for updates and vibration
     */
    private final long DURATION = 2000;

    /**
     * The distance to count for in this interval session.
     */
    private float intervalDistance;
    /**
     * The distance covered so far in this specific interval
     */
    private float currentDistance;
    private long mStartTime, totalTime, intervalTime, intervalStartRest;
    private int intervalRounds;
    /**
     * The last location object obtained by the location updates
     * Used to compute distance from the next location and add to total.
     */
    public Location lastLocation;
    SharedPreferences app_preferences;
    boolean hasSound, hasVibration;
    ExtApplication application;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Boolean intervalInProgress;
    PowerManager.WakeLock wl;
    Vibrator v;
    TTSManager ttsManager;
    List<Location> locationList;
    private Handler mHandler = new Handler();
    private List<Interval> intervals = new ArrayList<Interval>();

    /**
     * The pos of the fastest interval
     **/
    private int fastest_position;
    /**
     * The millisecs of the fastest interval
     */
    private long fastest_millis;

    public static final double MILE_TO_METERS_CONST = 1609.344;

    public static final double METERS_TO_MILES_CONST = 0.000621371192;

    /**
     * Has the user selected miles as unit?
     */
    private boolean isMetricMiles;


    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wl.acquire();
        application = (ExtApplication) getApplication();
        v = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
        app_preferences = getApplication().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        locationList = new ArrayList<Location>();
        buildGoogleApiClient();
        ttsManager = application.getTtsManager();

        isMetricMiles = app_preferences.getBoolean(AppConstants.METRIC_MILES, false);

    }

    protected synchronized void buildGoogleApiClient() {
        //i will connect when needed
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Create a request for location updates
     * setting the appropriate parameters
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        if (intervalInProgress){
            mLocationRequest.setInterval(DURATION);
            mLocationRequest.setFastestInterval(DURATION - 1000);
            mLocationRequest.setSmallestDisplacement(10);
        }else {
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setSmallestDisplacement(1);
        }
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public RunningService() {
        super("RunningService");
    }

    /**
     * Can be called multiple times. Basically called every time startService()
     * is called from an activity
     *
     * @param intent
     * @param startId
     */
    @Override
    public void onStart(Intent intent, int startId) {

        SharedPreferences.Editor editor = app_preferences.edit();
        if (startId == 1) {//first
            editor.putBoolean(AppConstants.INTERVAL_IN_PROGRESS, false).apply();
            intervalInProgress = false;
            connectAndReceive();
            return;
        }

        if (intervalInProgress){
            return;
        }

        fastest_position = 0;
        fastest_millis = Long.MAX_VALUE;

        editor.putBoolean(AppConstants.INTERVAL_IN_PROGRESS, true).apply();
        hasSound = !app_preferences.getBoolean(AppConstants.NO_SOUND, false);
        hasVibration = !app_preferences.getBoolean(AppConstants.NO_VIBRATION, false);
        createForegroundNotification();
        intervalTime = intent.getLongExtra(AppConstants.INTERVAL_TIME, 0);
        intervalStartRest = intent.getLongExtra(AppConstants.INTERVAL_START_REST, 0);
        startCountDownForNextInterval(intervalStartRest);
        intervals = new ArrayList<Interval>();
        intervalDistance = intent.getFloatExtra(AppConstants.INTERVAL_DISTANCE, 0);

        if (isMetricMiles){
            intervalDistance *= MILE_TO_METERS_CONST;//meters to miles
        }

        intervalRounds = intent.getIntExtra(AppConstants.INTERVAL_ROUNDS, 0);
        new PerformAsyncTask(0).execute();
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

    /**
     * While interval is not in progress i send he last location all the time in order to be precise.
     *
     * As soon as i start the interval i check if the last location is null and if so,
     * I set it equals to the incoming location and I put it in the list
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {

        if (!intervalInProgress || mLocationRequest.getSmallestDisplacement() < 10) {
            if (lastLocation == null){
                sendFirstLocation(location);
            }
            lastLocation = location;
            return;
        }

        if (lastLocation == null) {
            lastLocation = location;
            locationList.add(location);
            return;
        }

        //every following update must be of at least 15m accurate to avoid a big straight line of many meters
        if (location.getAccuracy() < 15 && lastLocation.distanceTo(location) >= 10) {

            mLocationRequest.setFastestInterval(10000);
            mLocationRequest.setInterval(10000);
            //if i have a last & new loc and a right accuracy
            if (location.getTime() > lastLocation.getTime()) {
                float newDistance = lastLocation.distanceTo(location);
                currentDistance += newDistance;
                locationList.add(location);
                totalTime = SystemClock.uptimeMillis() - mStartTime;
                if (currentDistance >= intervalDistance) {
                    finishInterval();
                } else if (currentDistance > 0) {
                    refreshInterval();
                }

                lastLocation = location;
            }

            mLocationRequest.setFastestInterval(DURATION - 1000);
            mLocationRequest.setInterval(DURATION);
        }
    }

    void connectAndReceive() {
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        } else {
            createLocationRequest();
            startReceiving();
        }
    }

    /**
     * Create a notification with a title and a message
     * that leads to my activity_main activity when pressed
     * <p/>
     * If the sdk is >15 then I set flags like the
     * color of the light to blink
     */
    private void createForegroundNotification() {
        final int myID = 1234;
        if (Build.VERSION.SDK_INT > 15) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(application);
            mBuilder.setSmallIcon(R.drawable.ic_notification_icon);
            int color = getResources().getColor(R.color.interval_green);
            mBuilder.setColor(color);
            mBuilder.setContentTitle("Interval in progress");
            mBuilder.setContentText("Click to get into");
//            mBuilder.setOngoing(true);
            Intent resultIntent = new Intent(application, ActIntervalNew.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(application);
            stackBuilder.addParentStack(ActMain.class);
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
            Intent intent2 = new Intent(this, ActIntervalNew.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent2, 0);
//This constructor is deprecated. Use Notification.Builder instead
            Notification notice = new Notification(R.drawable.ic_notification_icon, "Interval in progress", System.currentTimeMillis());
//This method is deprecated. Use Notification.Builder instead.
            notice.setLatestEventInfo(this, "Click to get into", "Running", pendIntent);
            notice.flags |= Notification.FLAG_NO_CLEAR;
            startForeground(myID, notice);
        }
    }

    private void finishInterval() {
        intervalInProgress = false;
        Interval interval = new Interval(-1, locationList, totalTime, intervalDistance);
        intervals.add(interval);
        if (totalTime < fastest_millis){
            fastest_millis = totalTime;
            intervals.get(fastest_position).setFastest(false);
            interval.setFastest(true);
            fastest_position = intervals.size()-1;
        }

        currentDistance = 0;
        locationList.clear();
        boolean completed = intervalRounds > 0 && intervals.size() >= intervalRounds;

        vibrate(3000);

        String toSpeak;
        if (completed) {
            toSpeak = "COMPLETED " + intervalRounds + " intervals. ";
        } else {
            toSpeak = "STOPPED. ";
            startCountDownForNextInterval(intervalTime);
        }

        toSpeak += computeRemainingSpeakText();
        speak(toSpeak);
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putBoolean(AppConstants.INTERVAL_COMPLETED, completed);
        editor.putInt(AppConstants.COMPLETED_NUM, intervals.size());
        editor.putBoolean(AppConstants.IS_RUNNING, false);
        editor.putFloat(AppConstants.TOTAL_DIST, 0);
        editor.putLong(AppConstants.TOTAL_TIME, 0);
        Type listOfIntervals = new TypeToken<List<Interval>>() {
        }.getType();
        String json = (new Gson()).toJson(intervals, listOfIntervals);
        editor.putString(AppConstants.INTERVALS, json);
        if (!app_preferences.getBoolean(AppConstants.HAS_RUN_METERS, false)){
            editor.putBoolean(AppConstants.HAS_RUN_METERS, true);
        }
        editor.apply();
        Intent intent = new Intent(AppConstants.NOTIFICATION);
        intent.putExtra(AppConstants.INTERVAL_COMPLETED, completed);
        sendBroadcast(intent);
        totalTime = 0;
    }

    /**
     * Compute what has to be spoken based on the total time
     * @return
     */
    private String computeRemainingSpeakText(){
        int hours = (int) (totalTime / 3600000);
        int mins = (int) ((totalTime - (hours * 3600000)) / 60000);
        int secs = (int) ((totalTime - (hours * 3600000) - (mins * 60000)) / 1000);
        return mins + " minutes and " + secs + " seconds";
    }

    private void speak(final String textToSpeak) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (hasSound) {
                    application.getTtsManager().initQueue(textToSpeak);
                }
            }
        }).start();
    }

    private void vibrate(final long millis){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (hasVibration) {
                    v.vibrate(millis);
                }
            }}).start();
    }

    private void sendFirstLocation(Location loc){
        SharedPreferences.Editor editor = app_preferences.edit();
        Type locType = new TypeToken<Location>() {
        }.getType();
        String json = (new Gson()).toJson(loc, locType);
        editor.putString(AppConstants.FIRST_LOCATION, json);
        editor.apply();
        Intent intent = new Intent(AppConstants.NOTIFICATION);
        intent.putExtra(AppConstants.FIRST_LOCATION, "loc");
        sendBroadcast(intent);
    }

    private void refreshInterval() {
        new PerformAsyncTask(2).execute();
    }

    /**
     * Every time an interval s finished i restart the handler runnable
     * and reset the countdown start time.
     * @param millis
     */
    private void startCountDownForNextInterval(long millis) {
        mHandler.postDelayed(mStartRunnable, millis);
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putLong(AppConstants.MSTART_COUNTDOWN_TIME, SystemClock.uptimeMillis());
        editor.apply();
    }

    @Override
    public void onDestroy() {
        try {
            stopLocationUpdates();
            if (wl.isHeld()) {
                wl.release();
            }
            mHandler.removeCallbacks(mStartRunnable);
        } catch (Exception e) {
            //Log.v("LATLNG", "Crash");
        } finally {
            super.onDestroy();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private Runnable mStartRunnable = new Runnable() {

        public void run() {
            vibrate(2000);
            speak("STARTED");
            mStartTime = SystemClock.uptimeMillis();
            if (mLocationRequest.getSmallestDisplacement() < 10) {
                mLocationRequest.setInterval(DURATION);
                mLocationRequest.setFastestInterval(DURATION - 1000);
                mLocationRequest.setSmallestDisplacement(10);
                if ( lastLocation != null){
                    locationList.add(lastLocation);
                }
            }
            startReceiving();
            intervalInProgress = true;
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putBoolean(AppConstants.IS_RUNNING, true);
            editor.putLong(AppConstants.MSTART_TIME, mStartTime);
            editor.apply();
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
        Intent intent = new Intent(AppConstants.NOTIFICATION);
        intent.putExtra(AppConstants.CONNECTION_FAILED, true);
        sendBroadcast(intent);
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
            SharedPreferences.Editor editor = app_preferences.edit();
            if (type == 0) {
                editor.putFloat(AppConstants.INTERVAL_DISTANCE, intervalDistance);
                editor.putLong(AppConstants.INTERVAL_START_REST, intervalStartRest);
                editor.putLong(AppConstants.INTERVAL_TIME, intervalTime);
                editor.putInt(AppConstants.INTERVAL_ROUNDS, intervalRounds);
            } else if (type == 2) {
               // if (((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn()) {
                    Intent intent = new Intent(AppConstants.NOTIFICATION);
                    intent.putExtra(AppConstants.INTERVAL_DISTANCE, currentDistance);

                    intent.putExtra(AppConstants.INTERVAL_TIME, totalTime);
                    sendBroadcast(intent);
               // }

                if (!app_preferences.getBoolean(AppConstants.HAS_RUN_METERS, false)){
                    editor.putBoolean(AppConstants.HAS_RUN_METERS, true);
                }

                Type listOfLocations = new TypeToken<List<Location>>() {
                }.getType();

                List <Location> locationListSafeCopy = new ArrayList<Location>(locationList);
                String json = (new Gson()).toJson(locationListSafeCopy, listOfLocations);
                editor.putString(AppConstants.LATLONLIST, json);
                editor.putFloat(AppConstants.TOTAL_DIST, currentDistance);
                editor.putLong(AppConstants.TOTAL_TIME, totalTime);
            }
            editor.apply();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

}