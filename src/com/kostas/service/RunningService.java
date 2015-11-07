package com.kostas.service;

import android.app.*;
import android.support.v4.app.NotificationCompat;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.*;
import android.speech.tts.TextToSpeech;
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
import com.kostas.onlineHelp.MainActivity;
import com.kostas.onlineHelp.R;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunningService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static final String INTERVAL_DISTANCE = "intervalDistance";
    public static final String INTERVAL_SPEED = "intervalSpeed";
    public static final String INTERVAL_TIME = "intervalTime";
    public static final String INTERVAL_ROUNDS = "intervalRounds";
    public static final String INTERVAL_COMPLETED = "intervalCompleted";
    public static final String LATLONLIST = "latLonList";
    public static final String TOTAL_DIST = "totalDist";
    public static final String TOTAL_TIME = "totalTime";
    public static final String MSTART_TIME = "mStartTime";
    public static final String INTERVALS = "intervals";
    public static final String NOTIFICATION = "com.kostas.onlineHelp";
    public static final String IS_RUNNING = "is_running";
    public static final String COUNTDOWN_REMAINING = "countdown_remaining";


    private TextToSpeech textToSpeech;
    private String speechTextStart, speechTextFinish, speechFinal;
    private Handler mHandler = new Handler();
    public String latLonList;
    public float intervalDistance, currentDistance;
    private long mStartTime, totalTime,  intervalTime;
    private int intervalRounds;
    public Location lastLocation;
    SharedPreferences app_preferences;
    SharedPreferences.Editor editor;
    boolean firstChange, hasSound, hasVibration;
    CountDownTimer countDownTimer;
    ExtApplication application;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Gson gson;
    Type listOfObjects;
    Vibrator v;

    //i need the list here. The app might be killed and user might complete several intervals
    private List<Interval> intervals = new ArrayList<Interval>();

    @Override
    protected void onHandleIntent(Intent intent) {
       Log.v("LATLNG", "HANLDE");
    }


    @Override
    public void onCreate() {
        super.onCreate();

        application = (ExtApplication)getApplication();
        app_preferences = getApplication().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        editor = app_preferences.edit();
        gson = new Gson();
        listOfObjects = new TypeToken<List<Interval>>(){}.getType();
        buildGoogleApiClient();

        hasSound = !app_preferences.getBoolean("noSound", false);
        hasVibration = !app_preferences.getBoolean("noVibration", false);
        v = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);

        textToSpeech = new TextToSpeech(application, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                    textToSpeech.setSpeechRate(1.2f);
                    String textStart = app_preferences.getString("speechStart", "Started");
                    String textFinish = app_preferences.getString("speechFinish", "Stopped");
                    speechTextStart = textStart.trim().equals("") ? "Started" : textStart ;
                    speechTextFinish = textFinish.trim().equals("") ? "Stopped" : textFinish ;
                }
            }
        });





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


        //it will start two times:
        //the first is with intent values
        //the second is with null intent and values from shared prefs

        if (intent!=null){//first

            intervalTime = intent.getLongExtra(INTERVAL_TIME, 0);
            startCountDownForNextInterval(intervalTime);

                intervals = new ArrayList<Interval>();

                intervalDistance = intent.getFloatExtra(INTERVAL_DISTANCE, 0);

                intervalRounds = intent.getIntExtra(INTERVAL_ROUNDS, 0 );

            new PerformAsyncTask(0).execute();

//            }

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


        if (!firstChange || lastLocation==null){
            firstChange = true;

            lastLocation = location;
            return;
        }


        //the first update can be a max of 20m accurate
        if ( latLonList==null && location.getAccuracy() < 20){

            currentDistance = 0;
            latLonList= String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
            lastLocation = location;
            return;
        }

        //every following update must be of at least 15m accurate
        if (location.getAccuracy() < 15){

        //if i have a last & new loc and a right accuracy
        if (lastLocation!=null && (location.getTime()>lastLocation.getTime())) {

            float newDistance = lastLocation.distanceTo(location);
            currentDistance += newDistance;

            latLonList+="," + String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());

            if (currentDistance >= intervalDistance){
                play( intervalRounds>0 && intervals.size()+1>=intervalRounds, false);
                finishInterval();
            }else if (currentDistance > 0){
                refreshInterval((location.getTime()-lastLocation.getTime())/ newDistance);
            }
        }

          if (lastLocation==null || (location.getTime()>lastLocation.getTime()))  lastLocation = location;

        }
    }


    private void createForegroundNotification(){


        final  int myID = 1234;

        if (Build.VERSION.SDK_INT > 15) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(application);
            mBuilder.setSmallIcon(R.drawable.interval_flag);
            mBuilder.setContentTitle("Interval in progress");
            mBuilder.setContentText("Click to get into");
//            mBuilder.setOngoing(true);

            Intent resultIntent = new Intent(application, MainActivity.class);
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
        }else {


//The intent to launch when the user clicks the expanded notification
            Intent intent2 = new Intent(this, MainActivity.class);
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



        if (!(intervalRounds>0 && intervals.size()+1>=intervalRounds)){
            startCountDownForNextInterval(intervalTime);
        }


//        locationManager.removeUpdates(this);

        //user might complete several intervals when app killed
        intervals.add(new Interval(latLonList, totalTime, intervalDistance));
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(INTERVAL_TIME, totalTime);
        intent.putExtra(LATLONLIST, latLonList);

        mHandler.removeCallbacks(mUpdateTimeTask);
        totalTime = 0;
        currentDistance = 0;
        latLonList = null;
        firstChange = false;

        if (intervalRounds>0 && intervals.size()>=intervalRounds){

            editor.putBoolean(INTERVAL_COMPLETED, true);
            intent.putExtra(INTERVAL_COMPLETED, true);
//            play( true, false);
        }


        sendBroadcast(intent);


        //todo async here

       new PerformAsyncTask(3).execute();

    }

    private void refreshInterval(float pace){


        long interval = (100*pace/6) > 3000 ? ( (100*pace/6) < 6000 ?  (long)(100*pace/6)  : 6000    ) : 3000;
//        Toast.makeText(application,"Refresh rate of "+interval+" msecs", Toast.LENGTH_SHORT).show();

        mLocationRequest.setInterval(interval-1000);
        mLocationRequest.setFastestInterval(interval - 2000);


        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(INTERVAL_DISTANCE, currentDistance);
        intent.putExtra(INTERVAL_SPEED, pace);
        intent.putExtra(INTERVAL_TIME, totalTime);
        intent.putExtra(LATLONLIST, latLonList);
        sendBroadcast(intent);

        new PerformAsyncTask(2).execute();

    }


    private void startCountDownForNextInterval(long remainingTime){

        if (countDownTimer!=null)
            countDownTimer.cancel();

        countDownTimer =   new CountDownTimer(remainingTime, 1000) {
            public void onTick(long millisUntilFinished) {

                editor.putLong(COUNTDOWN_REMAINING, millisUntilFinished);
                editor.apply();
            }
            public void onFinish() {
                mStartTime = SystemClock.uptimeMillis();
                play( false, true);
//                startReceiving();

                if (!mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }else{
                    startReceiving();
                }
                editor.putBoolean(IS_RUNNING, true);
                editor.putLong(MSTART_TIME, mStartTime);
                editor.apply();

                //Log.v("SERVICE", "Timer expired");
            }
        }.start();
    }

    public void play(boolean completed, boolean starting) {


        if (hasVibration) {

            if (starting) {
                v.vibrate(1000);
            } else
                v.vibrate(2000);
        }


        if (hasSound){

            String finalFinish=speechTextFinish;

            if (starting) {
                speechFinal = speechTextStart;
            } else{

                if (completed){
                    speechFinal = "Completed";
                }else {
                    if (intervalRounds>0) finalFinish = speechTextFinish+" . "+(intervalRounds-intervals.size()-1)+" rounds remaining";
                    speechFinal = finalFinish;
                }

            }

            mSpeakText.run();



        }

    }


    @Override
    public void onDestroy() {

        try {
            stopLocationUpdates();
        }catch (Exception e){

        }

        if (countDownTimer!=null)
        countDownTimer.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private Runnable mUpdateTimeTask = new Runnable(){

        public void run() {

//            final long start = mStartTime;
            totalTime = SystemClock.uptimeMillis()- mStartTime;
            mHandler.postDelayed(this, 1000);

        }
    };

    private Runnable mSpeakText = new Runnable(){

        public void run() {

            textToSpeech.speak(speechFinal, TextToSpeech.QUEUE_FLUSH, null);


        }
    };



    @Override
    public void onConnected(Bundle bundle) {

        createLocationRequest();
        startReceiving();

    }

    private void  startReceiving(){

        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        else
            Toast.makeText(application, "google client not connected", Toast.LENGTH_SHORT).show();


        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 1000);

    }



    protected void stopLocationUpdates() {

//        Toast.makeText(application, "stopping updates", Toast.LENGTH_SHORT).show();
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this,  "Google services api failed to connect", Toast.LENGTH_SHORT).show();
    }



    private class PerformAsyncTask extends AsyncTask<Void, Void, Void> {

        int type;



        public PerformAsyncTask( int type) {
            this.type = type;

        }

        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... unused) {

            if (type==0){
                editor.putFloat(INTERVAL_DISTANCE, intervalDistance);
                editor.putLong(INTERVAL_TIME, intervalTime);
                editor.putInt(INTERVAL_ROUNDS, intervalRounds);
                editor.apply();

            }

            else if (type==2){

                editor.putFloat(TOTAL_DIST, currentDistance);
                editor.putLong(TOTAL_TIME, totalTime);
                editor.putString(LATLONLIST, latLonList);
                editor.apply();
            }else if (type==3){
                editor.putBoolean(IS_RUNNING, false);
                editor.putFloat(TOTAL_DIST, 0);
                editor.putFloat(TOTAL_TIME ,0);
                editor.putString(LATLONLIST,null);

                String json = gson.toJson(intervals, listOfObjects);
                editor.putString(INTERVALS, json);
                editor.apply();
            }

            return null;

        }

        @Override
        protected void onPostExecute(Void result) {

        }

    }


}