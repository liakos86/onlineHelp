package com.kostas.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.*;
import android.speech.tts.TextToSpeech;
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
//    public static final String SINGLE_UPDATE = "single_update";
//    public static final String LOCATION_FOUND = "location_found";

    private TextToSpeech textToSpeech;
    private String speechTextStart, speechTextFinish;

    private Handler mHandler = new Handler();
    public String latLonList;
    public float intervalDistance, currentDistance;
    private long mStartTime, totalTime,  intervalTime;
    private int intervalRounds;
    public Location lastLocation;
    SharedPreferences app_preferences;
    SharedPreferences.Editor editor;
    boolean firstChange, noSound, noVibration;
//    boolean singleUpdate;
    CountDownTimer countDownTimer;
//    MediaPlayer mp = new MediaPlayer();
    ExtApplication application;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;

    Vibrator v;

    //i need the list here. The app might be killed and user might complete several intervals
    private List<Interval> intervals = new ArrayList<Interval>();

    @Override
    protected void onHandleIntent(Intent intent) {}


    @Override
    public void onCreate() {
        super.onCreate();
        application = (ExtApplication)getApplication();
        app_preferences = getApplication().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        editor = app_preferences.edit();



//        Toast.makeText(application, "Connecting google", Toast.LENGTH_SHORT).show();
        buildGoogleApiClient();

        noSound = app_preferences.getBoolean("noSound", false);
        noVibration = app_preferences.getBoolean("noVibration", false);
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

//        if (singleUpdate){
//            lastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                    mGoogleApiClient);
////            Toast.makeText(application, "single update", Toast.LENGTH_SHORT).show();
//            mLocationRequest.setExpirationDuration(10000);
//            mLocationRequest.setNumUpdates(1);
//        }else {
//            Toast.makeText(application, "multiple update", Toast.LENGTH_SHORT).show();
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
    public int onStartCommand(Intent intent, int flags, int startId) {

        //it will start two times:
        //the first is with intent values
        //the second is with null intent and values from shared prefs

        if (intent!=null){//first

//            if (intent.getBooleanExtra(SINGLE_UPDATE, false)){
//                singleUpdate = true;
//                mGoogleApiClient.connect();
//            }
//            else {
//                singleUpdate = false;
                intervals = new ArrayList<Interval>();

                intervalDistance = intent.getFloatExtra(INTERVAL_DISTANCE, 0);
                intervalTime = intent.getLongExtra(INTERVAL_TIME, 0);
                intervalRounds = intent.getIntExtra(INTERVAL_ROUNDS, 0 );

                editor.putFloat(INTERVAL_DISTANCE, intervalDistance);
                editor.putLong(INTERVAL_TIME, intervalTime);
                editor.putInt(INTERVAL_ROUNDS, intervalRounds);
                editor.apply();

                startCountDownForNextInterval(intervalTime);
//            }

        }else{//second

            Gson gson = new Gson();
            Type listOfObjects = new TypeToken<List<Interval>>(){}.getType();
            String intervalsGson = app_preferences.getString(INTERVALS,"");
            intervals = (List<Interval>) gson.fromJson(intervalsGson, listOfObjects);

            if (intervals==null){
                intervals = new ArrayList<Interval>();
            }

            intervalDistance = app_preferences.getFloat(INTERVAL_DISTANCE, 0);
            intervalTime = app_preferences.getLong(INTERVAL_TIME, 0);
            intervalRounds = app_preferences.getInt(INTERVAL_ROUNDS, 0);



            if (app_preferences.getBoolean(IS_RUNNING, false)) {

                latLonList = app_preferences.getString(LATLONLIST, null);
                currentDistance = app_preferences.getFloat(TOTAL_DIST, 0);
                firstChange = true;
                mStartTime = app_preferences.getLong(MSTART_TIME, SystemClock.uptimeMillis());

//                singleUpdate= false;

                if (!mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }else{
                    Toast.makeText(application, "was connected after killing app", Toast.LENGTH_SHORT).show();
                    startReceiving();
                }

//               startReceiving();



            }else{
                latLonList = null;
                currentDistance = 0;
                firstChange = false;
                long remainingTime = app_preferences.getLong(COUNTDOWN_REMAINING, 0);
                startCountDownForNextInterval(remainingTime);
            }


            //Log.v("SERVICE", "Service restart");
        }

        return START_STICKY;
    }





    @Override
    public void onTaskRemoved(Intent rootIntent) {//killing the app here, store in shared prefs

       // no need to store anything. onRefresh() & onFinish() have done this
        //Log.v("SERVICE", "Task remove");

        super.onTaskRemoved(rootIntent);
    }



    @Override
    public boolean stopService(Intent name) {

        //Log.v("SERVICE", "Service Stopped programmaticaly");
        stopLocationUpdates();
        return super.stopService(name);
    }


    @Override
    public void onLocationChanged(Location location) {

        //the loc fix can be of any accuracy
//        if (singleUpdate){
//
//            lastLocation = location;
//            mHandler.removeCallbacks(mExpiredRunnable);
//            broadcastLocationOrSpeed();
//            return;
//        }

        //the first update can be a max of 20m accurate
        if (!firstChange && latLonList==null && location.getAccuracy() < 20){
            firstChange = true;

            //todo this results in accounting the first point we get
            latLonList = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
//            latLonList="";
            lastLocation = location;
            return;
        }


        //every following update must be of at least 15m accurate
        if (location.getAccuracy() < 15){


        //if i have a last & new loc and a right accuracy
        if (lastLocation!=null && (location.getTime()>lastLocation.getTime())) {

        float    newDistance = (float) distance_between(lastLocation, location);
            currentDistance += newDistance;


//            if (latLonList.equals("")) {
//                latLonList = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
//            } else {
                latLonList += "," + String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
//            }

            if (currentDistance >= intervalDistance){
                finishInterval();
            }else if (currentDistance > 0){
                refreshInterval((location.getTime()-lastLocation.getTime())/ newDistance);
//                play(application, R.raw.location_update);
//                Toast.makeText(application, "Accuracy: "+location.getAccuracy(), Toast.LENGTH_SHORT).show();
            }


        }

          if (lastLocation==null || (location.getTime()>lastLocation.getTime()))  lastLocation = location;

        }


    }

    double distance_between(Location l1, Location l2)
    {

        double lat1=l1.getLatitude();
        double lon1=l1.getLongitude();
        double lat2=l2.getLatitude();
        double lon2=l2.getLongitude();
        double R = 6371; // km
        double dLat = (lat2-lat1)*Math.PI/180;
        double dLon = (lon2-lon1)*Math.PI/180;
        lat1 = lat1*Math.PI/180;
        lat2 = lat2*Math.PI/180;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));


        return R * c * 1000;
    }

//    private void broadcastLocationOrSpeed(){
//
//        Intent intent = new Intent(NOTIFICATION);
//
////        if (forLocation) {
//
//            intent.putExtra(LOCATION_FOUND, true);
//            intent.putExtra(SINGLE_UPDATE, lastLocation);
//
////        }
////        else{
////            intent.putExtra(SPEED_UPDATE, true);
////            intent.putExtra("speed", lastLocation.getSpeed());
////        }
//        sendBroadcast(intent);
//
//    }


    private void finishInterval() {

        stopLocationUpdates();
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
            play( true, false);
        }else{
            play( false, false);
            startCountDownForNextInterval(intervalTime);
        }

        editor.putBoolean(IS_RUNNING, false);
        editor.putFloat(TOTAL_DIST ,0);
        editor.putFloat(TOTAL_TIME ,0);

        Gson gson = new Gson();
        Type listOfObjects = new TypeToken<List<Interval>>(){}.getType();
        String json = gson.toJson(intervals, listOfObjects);
        editor.putString(INTERVALS, json);
        editor.apply();



        sendBroadcast(intent);
    }

    private void refreshInterval(float pace){


        long interval = (100*pace/6) > 3000 ? ( (100*pace/6) < 6000 ?  (long)(100*pace/6)  : 6000    ) : 3000;
//        Toast.makeText(application,"Refresh rate of "+interval+" msecs", Toast.LENGTH_SHORT).show();

        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(interval - 1000);


        //Log.v("SERVICE", currentDistance+"m  so far");
        editor.putFloat(TOTAL_DIST, currentDistance);
        editor.putLong(TOTAL_TIME, totalTime);
        editor.putString(LATLONLIST, latLonList);
        editor.apply();

        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(INTERVAL_DISTANCE, currentDistance);
        intent.putExtra(INTERVAL_SPEED, pace);
        sendBroadcast(intent);

    }


    private void startCountDownForNextInterval(long remainingTime){

        if (countDownTimer!=null)
            countDownTimer.cancel();

        countDownTimer =   new CountDownTimer(remainingTime, 1000) {
            public void onTick(long millisUntilFinished) {

//                //Log.v("SERVICE", millisUntilFinished/1000+"");
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
//        stop();
//
//        mp = MediaPlayer.create(c, rid);
//        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer) {
//                stop();
//            }
//        });
//
//        mp.start();


//

        if (!noVibration) {

            if (starting) {
                v.vibrate(1000);
            } else
                v.vibrate(2000);
        }


        if (!noSound){
            if (starting) {
                textToSpeech.speak(speechTextStart, TextToSpeech.QUEUE_FLUSH, null);
            } else{

                if (completed){
                    textToSpeech.speak("Completed", TextToSpeech.QUEUE_FLUSH, null);
                }else {
                    textToSpeech.speak(speechTextFinish, TextToSpeech.QUEUE_FLUSH, null);
                }

            }
        }

    }

//    public void stop() {
//        if (mp != null) {
//            mp.release();
//            mp = null;
//        }
//    }




//    public  void requestSingleUpdate(){
//
//        if (++updates < 6) {
//            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
//        }else{
//            broadcastLocationOrSpeed(true);
//            mHandler.removeCallbacks(singleUpdateTask);
//        }
//        //Log.v("SERVICE", "single_update..");
//
//
//    }

    @Override
    public void onDestroy() {




        //Log.v("SERVICE", "destroy");

        try {
            stopLocationUpdates();
//            locationManager.removeUpdates(listener);
        }catch (Exception e){
//            if (locationManager == null)  //Log.v("SERVICE", "null locationManager");
//            if (listener == null)  //Log.v("SERVICE", "null listener");
        }

        if (countDownTimer!=null)
        countDownTimer.cancel();
        super.onDestroy();
    }



    private Runnable mUpdateTimeTask = new Runnable(){

        public void run() {

            final long start = mStartTime;
            totalTime = SystemClock.uptimeMillis()- start;
            mHandler.postDelayed(this, 3000);


        }
    };

//    private final Runnable mExpiredRunnable = new Runnable() {
//        @Override
//        public void run() {
//            broadcastLocationOrSpeed();
//        }
//    };



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


//        if (singleUpdate) {
//            mHandler.removeCallbacks(mExpiredRunnable);
//            mHandler.postDelayed(mExpiredRunnable, 10000);
//        }else {
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 3000);
//        }

    }


    //will start receiving updates and if it is a single update it will stop in 10 secs
//    protected void startLocationUpdates() {
//
//
//        if (mGoogleApiClient.isConnected())
//            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//        else
//            Toast.makeText(application, "google client not connected", Toast.LENGTH_SHORT).show();
//
//
////        if (singleUpdate)
////            mHandler.postDelayed(mExpiredRunnable, 10000);
//    }

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
}