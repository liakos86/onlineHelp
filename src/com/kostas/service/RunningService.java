package com.kostas.service;

/**
 * Created by KLiakopoulos on 6/19/2015.
 */

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.FrgInterval;
import com.kostas.onlineHelp.MainActivity;
import com.kostas.onlineHelp.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class RunningService extends IntentService implements LocationListener{
    public static final String INTERVAL_DISTANCE = "intervalDistance";
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
    public static final String SINGLE_UPDATE = "single_update";
    public static final String LOCATION_FOUND = "location_found";

//    FusedLocationProviderApi providerApi;


    private Handler mHandler = new Handler();

    public String latLonList;
    public float intervalDistance, currentDistance;
    private long mStartTime, totalTime,  intervalTime;
    private int intervalRounds;
    public Location lastLocation;
    SharedPreferences app_preferences;
    boolean firstChange;
    boolean singleUpdate;
    private LocationManager locationManager;
    private Context con;
    private FrgInterval instance;
    CountDownTimer countDownTimer;
    MediaPlayer mp = new MediaPlayer();
    LocationListener listener;
    ExtApplication application;


    //i need the list here. The app might be killed and user might complete several intervals
    private List<Interval> intervals = new ArrayList<Interval>();




    @Override
    protected void onHandleIntent(Intent intent) {

        Toast.makeText(this,  "handle intent", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {

        super.onCreate();

    }



    public RunningService() {
        super("RunningService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        //it will start two times:
        //the first is with intent values
        //the second is with null intent and values from shared prefs

        application = (ExtApplication)getApplication();

        app_preferences = getApplication().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        listener = this;

        if (intent!=null){//first

            if (intent.getBooleanExtra(SINGLE_UPDATE, false)){

                singleUpdate = true;

                locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);


                mHandler.postDelayed(singleUpdateTask, 5000);


                return START_NOT_STICKY;

            }else {

                intervals = new ArrayList<Interval>();
                intervalDistance = intent.getFloatExtra(INTERVAL_DISTANCE, 0);
                intervalTime = intent.getLongExtra(INTERVAL_TIME, 0);
                intervalRounds = intent.getIntExtra(INTERVAL_ROUNDS, 0 );
                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putFloat(INTERVAL_DISTANCE, intervalDistance);
                editor.putLong(INTERVAL_TIME, intervalTime);
                editor.putInt(INTERVAL_ROUNDS, intervalRounds);
                mStartTime = SystemClock.uptimeMillis();
                editor.putLong(MSTART_TIME, mStartTime);
                editor.commit();

                startCountDownForNextInterval(intervalTime);
            }



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

                latLonList = app_preferences.getString(LATLONLIST, "");
                currentDistance = app_preferences.getFloat(TOTAL_DIST, 0);
                firstChange = true;
                mStartTime = app_preferences.getLong(MSTART_TIME, SystemClock.uptimeMillis());

               startReceiving();



            }else{
                latLonList = "";
                currentDistance =0;
                firstChange = false;
                long remainingTime = app_preferences.getLong(COUNTDOWN_REMAINING, 0);
                startCountDownForNextInterval(remainingTime);
            }


            Log.v("SERVICE", "Service restart");
        }

        return START_STICKY;
    }


    private void  startReceiving(){
        locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);

//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 8, this);

            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 200);

//        }else{
//
//            finishInterval();
//
//
//        }
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {//killing the app here, store in shared prefs

       // no need to store anything. onRefresh() & onFinish() have done this
        Log.v("SERVICE", "Task remove");

        super.onTaskRemoved(rootIntent);
    }



    @Override
    public boolean stopService(Intent name) {

        Log.v("SERVICE", "Service Stopped programmaticaly");
        locationManager.removeUpdates(this);
        return super.stopService(name);
    }



    @Override
    public void onLocationChanged(Location location) {

//        Toast.makeText(application, location.getSpeed()+"", Toast.LENGTH_SHORT).show();

        if (singleUpdate){
            Log.v("SERVICE", "found");

            lastLocation = location;
            broadcastLocation(true);
            mHandler.removeCallbacks(singleUpdateTask);
            return;
        }

        if (!firstChange && (latLonList==null || latLonList.equals("")||latLonList.equals(" "))){
            firstChange = true;

            //todo this results in accounting the first point we get
            latLonList = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
//            latLonList="";
            lastLocation = location;
            return;
        }


        float newDistance;
        if (lastLocation!=null) {
            newDistance = (float) (distance_between(lastLocation, location));
            currentDistance += newDistance;


            if (latLonList.equals("")) {
                latLonList = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
            } else {
                latLonList += "," + String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
            }

            if (currentDistance >= intervalDistance){
                finishInterval();
            }else if (currentDistance > 0){
                refreshInterval();
            }


        }

        lastLocation = location;


    }

    double distance_between(Location l1, Location l2)
    {
        //float results[] = new float[1];
    /* Doesn't work. returns inconsistent results
    Location.distanceBetween(
            l1.getLatitude(),
            l1.getLongitude(),
            l2.getLatitude(),
            l2.getLongitude(),
            results);
            */
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
        double d = R * c * 1000;

//        log_write("dist betn "+
//                        d + " " +
//                        l1.getLatitude()+ " " +
//                        l1.getLongitude() + " " +
//                        l2.getLatitude() + " " +
//                        l2.getLongitude()
//        );

        return d;
    }

    private void broadcastLocation(boolean found){

        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(LOCATION_FOUND, found);
        intent.putExtra(SINGLE_UPDATE, lastLocation);
        sendBroadcast(intent);

    }


    private void finishInterval() {

        locationManager.removeUpdates(this);


        //user might complete several intervals when app killed
        SharedPreferences.Editor editor = app_preferences.edit();




        intervals.add(new Interval(latLonList, totalTime, intervalDistance));
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(INTERVAL_TIME, totalTime);
        intent.putExtra(LATLONLIST, latLonList);

        mHandler.removeCallbacks(mUpdateTimeTask);
        totalTime = 0;
        currentDistance = 0;
        latLonList = "";
        firstChange = false;

        if (intervalRounds>0 && intervals.size()>=intervalRounds){

            editor.putBoolean(INTERVAL_COMPLETED, true);
            intent.putExtra(INTERVAL_COMPLETED, true);
            play(getApplication(), R.raw.interval_completed);
        }else{
            play(getApplication(), R.raw.interval_finish);
            startCountDownForNextInterval(intervalTime);
        }







        editor.putBoolean(IS_RUNNING, false);

        Gson gson = new Gson();
        Type listOfObjects = new TypeToken<List<Interval>>(){}.getType();
        String json = gson.toJson(intervals, listOfObjects);
        editor.putString(INTERVALS, json);
        editor.commit();



        sendBroadcast(intent);
    }

    private void refreshInterval(){


        Log.v("SERVICE", currentDistance+"");
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putFloat(TOTAL_DIST, currentDistance);
        editor.putLong(TOTAL_TIME, totalTime);
        editor.putString(LATLONLIST, latLonList);
        editor.commit();

        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(INTERVAL_DISTANCE, currentDistance);
        sendBroadcast(intent);

    }


    private void startCountDownForNextInterval(long remainingTime){

        final SharedPreferences.Editor editor = app_preferences.edit();
        countDownTimer =   new CountDownTimer(remainingTime, 1000) {
            public void onTick(long millisUntilFinished) {

//                Log.v("SERVICE", millisUntilFinished/1000+"");
                editor.putLong(COUNTDOWN_REMAINING, millisUntilFinished);
                editor.commit();
            }
            public void onFinish() {
                mStartTime = SystemClock.uptimeMillis();
                play(getApplication(), R.raw.interval_start);
                startReceiving();
                editor.putBoolean(IS_RUNNING, true);
                editor.putLong(MSTART_TIME, mStartTime);
                editor.commit();

                Log.v("SERVICE", "Timer expired");
            }
        }.start();



    }

    public void play(Context c, int rid) {
        stop();

        mp = MediaPlayer.create(c, rid);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop();
            }
        });

        mp.start();


        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(2000);

    }

    public void stop() {
        if (mp != null) {
            mp.release();
            mp = null;
        }
    }



    @Override
    public void onProviderEnabled(String provider) {

        Log.v("SERVICE", "Enabled provider");
    }

    @Override
    public void onProviderDisabled(String provider) {


        Log.v("SERVICE", "Disabled provider");
        broadcastLocation(false);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
//        Toast.makeText(this, "Status changed " + provider,
//                Toast.LENGTH_SHORT).show();

    }


    public  void requestSingleUpdate(){

        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
        Log.v("SERVICE", "single_update..");


    }

    @Override
    public void onDestroy() {




        Log.v("SERVICE", "destroy");

        try {
            locationManager.removeUpdates(listener);
            mHandler.removeCallbacks(singleUpdateTask);
        }catch (Exception e){
            if (locationManager == null)  Log.v("SERVICE", "null locationManager");
            if (listener == null)  Log.v("SERVICE", "null listener");
        }

        if (countDownTimer!=null)
        countDownTimer.cancel();
        super.onDestroy();
    }



    private Runnable mUpdateTimeTask = new Runnable(){

        public void run() {

            final long start = mStartTime;
            totalTime = SystemClock.uptimeMillis()- start;
            mHandler.postDelayed(this, 200);


        }
    };

    private Runnable singleUpdateTask = new Runnable(){

        public void run() {

            requestSingleUpdate();
            mHandler.postDelayed(this, 5000);


        }
    };


}