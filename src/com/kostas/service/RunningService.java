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
import com.kostas.onlineHelp.ActivityIntervalNew;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.MainActivity;
import com.kostas.onlineHelp.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RunningService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    /**
     * Strings for shared preferences use.
     */
    public static final String INTERVAL_IN_PROGRESS = "intervalInProgress";
    public static final String NO_SOUND = "noSound";
    public static final String NO_VIBRATION = "noVibration";
    public static final String INTERVAL_DISTANCE = "intervalDistance";
    public static final String INTERVAL_TIME = "intervalTime";
    public static final String INTERVAL_START_REST = "intervalStartRest";
    public static final String INTERVAL_ROUNDS = "intervalRounds";
    public static final String INTERVAL_COMPLETED = "intervalCompleted";
    public static final String LATLONLIST = "latLonList";
    public static final String COMPLETED_NUM = "completed_num";
    public static final String TOTAL_DIST = "totalDist";
    public static final String TOTAL_TIME = "totalTime";
    public static final String MSTART_TIME = "mStartTime";
    public static final String MSTART_COUNTDOWN_TIME = "mStartCountdownTime";
    public static final String INTERVALS = "intervals";
    public static final String IS_RUNNING = "is_running";
    public static final String CONNECTION_FAILED = "connectionFailed";
    public static final String FIRST_LOCATION = "firstLocation";

    /**
     * Intent filter
     */
    public static final String NOTIFICATION = "com.kostas.onlineHelp";

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
        app_preferences = getApplication().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        locationList = new ArrayList<Location>();
        buildGoogleApiClient();
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
            editor.putBoolean(INTERVAL_IN_PROGRESS, false).apply();
            intervalInProgress = false;
            connectAndReceive();
            return;
        }

        fastest_position = 0;
        fastest_millis = Long.MAX_VALUE;

        intervalInProgress = true;
        editor.putBoolean(INTERVAL_IN_PROGRESS, true).apply();
        hasSound = !app_preferences.getBoolean(NO_SOUND, false);
        hasVibration = !app_preferences.getBoolean(NO_VIBRATION, false);
        createForegroundNotification();
        intervalTime = intent.getLongExtra(INTERVAL_TIME, 0);
        intervalStartRest = intent.getLongExtra(INTERVAL_START_REST, 0);
        startCountDownForNextInterval(intervalStartRest);
        intervals = new ArrayList<Interval>();
        intervalDistance = intent.getFloatExtra(INTERVAL_DISTANCE, 0);
        intervalRounds = intent.getIntExtra(INTERVAL_ROUNDS, 0);
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


    @Override
    public void onLocationChanged(Location location) {

        if (!intervalInProgress) {
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
        if (location.getAccuracy() < 15) {
            mLocationRequest.setFastestInterval(10000);
            mLocationRequest.setInterval(10000);
            //if i have a last & new loc and a right accuracy
            if (lastLocation != null && (location.getTime() > lastLocation.getTime())) {
                float newDistance = lastLocation.distanceTo(location);
                currentDistance += newDistance;
                locationList.add(location);
                totalTime = SystemClock.uptimeMillis() - mStartTime;
                if (currentDistance >= intervalDistance) {
                    finishInterval();
                } else if (currentDistance > 0) {
                  // refreshInterval((location.getTime() - lastLocation.getTime()) / newDistance);
                    refreshInterval();
                }
            }

            if (lastLocation == null || (location.getTime() > lastLocation.getTime())) {
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
     * that leads to my main activity when pressed
     * <p/>
     * If the sdk is >15 then I set flags like the
     * color of the light to blink
     */
    private void createForegroundNotification() {
        final int myID = 1234;
        if (Build.VERSION.SDK_INT > 15) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(application);
            mBuilder.setSmallIcon(R.drawable.ic_notification_icon);
            mBuilder.setContentTitle("Interval in progress");
            mBuilder.setContentText("Click to get into");
//            mBuilder.setOngoing(true);
            Intent resultIntent = new Intent(application, ActivityIntervalNew.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(application);
            stackBuilder.addParentStack(MainActivity.class);
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
            Intent intent2 = new Intent(this, MainActivity.class);
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

        vibrate(2000);
        if (!completed) {
            speak("STOPPED");
        } else {
            speak("COMPLETED " + intervalRounds + " intervals");
        }
        int hours = (int) (totalTime / 3600000);
        int mins = (int) ((totalTime - (hours * 3600000)) / 60000);
        int secs = (int) ((totalTime - (hours * 3600000) - (mins * 60000)) / 1000);
        speak(mins + " minutes and " + secs + " seconds");
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putBoolean(INTERVAL_COMPLETED, completed);
        editor.putInt(COMPLETED_NUM, intervals.size());
        editor.putBoolean(IS_RUNNING, false);
        editor.putFloat(TOTAL_DIST, 0);
        editor.putFloat(TOTAL_TIME, 0);
        Type listOfIntervals = new TypeToken<List<Interval>>() {
        }.getType();
        String json = (new Gson()).toJson(intervals, listOfIntervals);
        editor.putString(INTERVALS, json);
        editor.apply();


        startCountDownForNextInterval(intervalTime);

        //todo Do i really need to care about my broadcast when no receivers?
        if (((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn()) {
            Intent intent = new Intent(NOTIFICATION);
            intent.putExtra(INTERVAL_COMPLETED, completed);
            sendBroadcast(intent);
        }



        totalTime = 0;
    }

    private void speak(String textToSpeak) {
        if (hasSound) {
            application.getTtsManager().initQueue(textToSpeak);
        }
    }

    private void vibrate(long millis){
        if (hasVibration){
            v.vibrate(millis);
        }
    }

    private void sendFirstLocation(Location loc){
        SharedPreferences.Editor editor = app_preferences.edit();
        Type locType = new TypeToken<Location>() {
        }.getType();
        String json = (new Gson()).toJson(loc, locType);
        editor.putString(FIRST_LOCATION, json);
        editor.apply();
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(FIRST_LOCATION, "loc");
        sendBroadcast(intent);
    }

    private void refreshInterval() {
        //DURATION = (100 * pace / 6) > 2000 ? ((100 * pace / 6) < 4000 ? (long) (100 * pace / 6) : 4000) : 2000;
        new PerformAsyncTask(2).execute();
    }

    private void startCountDownForNextInterval(long millis) {
        mHandler.postDelayed(mStartRunnable, millis);
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putLong(MSTART_COUNTDOWN_TIME, SystemClock.uptimeMillis());
        editor.apply();
    }

    @Override
    public void onDestroy() {
        try {
            stopLocationUpdates();
            if (wl.isHeld()) wl.release();

//            if (ttsManager != null) {
//                ttsManager.shutDown();
//            }

            mHandler.removeCallbacks(mStartRunnable);


        } catch (Exception e) {
            Log.v("LATLNG", "Crash");

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
            mStartTime = SystemClock.uptimeMillis();
            intervalInProgress = true;
            connectAndReceive();
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putBoolean(IS_RUNNING, true);
            editor.putLong(MSTART_TIME, mStartTime);
            editor.apply();
            vibrate(1000);
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
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(CONNECTION_FAILED, true);
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
                editor.putFloat(INTERVAL_DISTANCE, intervalDistance);
                editor.putLong(INTERVAL_START_REST, intervalStartRest);
                editor.putLong(INTERVAL_TIME, intervalTime);
                editor.putInt(INTERVAL_ROUNDS, intervalRounds);
            } else if (type == 2) {
                if (((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn()) {
                    Intent intent = new Intent(NOTIFICATION);
                    intent.putExtra(INTERVAL_DISTANCE, currentDistance);
                    intent.putExtra(INTERVAL_TIME, totalTime);
                    sendBroadcast(intent);
                }
                editor.putFloat(TOTAL_DIST, currentDistance);
                editor.putLong(TOTAL_TIME, totalTime);
                Type listOfLocations = new TypeToken<List<Location>>() {
                }.getType();
                String json = (new Gson()).toJson(locationList, listOfLocations);
                editor.putString(LATLONLIST, json);
            }
            editor.apply();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }
}