package com.kostas.onlineHelp;

import android.app.*;
import android.content.*;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Plan;
import com.kostas.dbObjects.Running;
import com.kostas.model.Database;
import com.kostas.service.RunningService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class FrgInterval extends BaseFragment{


    GoogleMap googleMap;
    CountDownTimer countDownTimer;
    private long  intervalTime=10;//mStartTime = 0L, totalTime=0L
//    private Handler mHandler = new Handler();
    private LocationManager locationManager;
//    private String provider;
    Location lastLocation;
    SharedPreferences app_preferences ;

    RelativeLayout   buttonDismiss, buttonSave;
    Button buttonSetIntervalValues, buttonSavePlan;
    ImageButton buttonStart, buttonStop, buttonBack;
//    boolean   running=false;//paused=false;
    String timerStop1;
    LinearLayout layoutBottomButtons;
    EditText editTextIntervalDistance ;
    float intervalDistance=50;
    ViewFlipper flipper;
    NumberPickerKostas intervalTimePicker, intervalDistancePicker, intervalRoundsPicker;
    FrgInterval instance;
    ProgressWheel timerProgressWheel, distanceProgressWheel;
    ListView completedIntervalsListView;
    List <Interval> intervalsList;
    IntervalAdapterItem adapterInterval;
    int rounds=0;
    Spinner plansSpinner;
    SpinnerAdapter plansAdapter;
    List<Plan>plans = new ArrayList<Plan>();


    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {


            //since we have broadcast an interval is finished, save it and reset countdown for next interval
            Bundle bundle = intent.getExtras();
            if (bundle != null) {


                if (bundle.getFloat(RunningService.INTERVAL_DISTANCE)!=0){

                    setDistanceProgress(bundle.getFloat(RunningService.INTERVAL_DISTANCE));

                }
                else if((bundle.getBoolean(RunningService.LOCATION_FOUND,false))){

                    stopRunningService();


                    SharedPreferences.Editor editor = app_preferences.edit();
                    editor.putBoolean("SEARCHING", false);
                    editor.commit();

                    lastLocation = (Location)bundle.get(RunningService.SINGLE_UPDATE);
                    if (lastLocation!=null) setAddressTextAndMap();
//                    toggleNoGps(!(bundle.getBoolean(RunningService.LOCATION_FOUND,false)));


                }
                else {
                    String latLonList = bundle.getString(RunningService.LATLONLIST);
                    long millis = bundle.getLong(RunningService.INTERVAL_TIME);
                    intervalsList.add(new Interval(-1, latLonList, millis, intervalDistance));
                    adapterInterval.notifyDataSetChanged();


                    prepareForNextInterval(bundle.getBoolean(RunningService.INTERVAL_COMPLETED, false));
                }


            }else{
                Log.v("LATLON", "Bundle is null");

            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.frg_interval, container, false);


        setPlansSpinner(v);
        getPlansFromDb();
        setTextViewsAndButtons(v);
        setListeners(v);
//        selectProvider();
        getLastLocation(v);
        setSaveListener();



        initializeMap();

       return  v;
    }

    private void setPlansSpinner(View v){
        plansSpinner = (Spinner) v.findViewById(R.id.plansSpinner);

        plansAdapter = new SpinnerAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, plans);

        plansSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListener());

        plansSpinner.setAdapter(plansAdapter);

    }

    private void setTextViewsAndButtons(View v){
        app_preferences = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);


        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        instance = this;

        intervalsList = new ArrayList<Interval>();

        buttonSetIntervalValues = (Button) v.findViewById(R.id.buttonSetIntervalValues);
        buttonSavePlan = (Button) v.findViewById(R.id.buttonSavePlan);
        buttonStart = (ImageButton) v.findViewById(R.id.buttonStart);
        buttonStop = (ImageButton) v.findViewById(R.id.buttonStop);
        buttonBack = (ImageButton) v.findViewById(R.id.buttonBack);
        flipper = (ViewFlipper) v.findViewById(R.id.flipper);
//        noGps = (LinearLayout) v.findViewById(R.id.noGps);
        intervalTimePicker = (NumberPickerKostas) v.findViewById(R.id.intervalTimePicker);
        intervalTimePicker.setValue(10);
        intervalDistancePicker = (NumberPickerKostas) v.findViewById(R.id.intervalDistancePicker);
        intervalDistancePicker.setValue(50);

        intervalRoundsPicker = (NumberPickerKostas) v.findViewById(R.id.intervalRoundsPicker);

        timerProgressWheel = (ProgressWheel) v.findViewById(R.id.timerProgressWheel);
        distanceProgressWheel = (ProgressWheel) v.findViewById(R.id.distanceProgressWheel);
        completedIntervalsListView = (ListView) v.findViewById(R.id.completedIntervals);
        layoutBottomButtons = (LinearLayout) v.findViewById(R.id.layoutBottomButtons);
        buttonDismiss = (RelativeLayout) v.findViewById(R.id.buttonDismissInterval);
        buttonSave = (RelativeLayout) v.findViewById(R.id.buttonSaveRunWithIntervals);




         adapterInterval = new  IntervalAdapterItem(getActivity(), getActivity().getApplicationContext(),
                R.layout.list_interval_row, intervalsList);
        completedIntervalsListView.setAdapter(adapterInterval);




//        plansSpinner.setSelection(-1);


    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RunningService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void getInRunningMode(boolean isRunning, boolean isCompleted, long millisecondsLeft, float distanceCovered){

        ((ExtApplication)getActivity().getApplication()).setInRunningMode(true);

        flipper.setDisplayedChild(1);
        buttonStart.setVisibility(View.INVISIBLE);
        buttonStop.setVisibility(View.VISIBLE);
        buttonBack.setVisibility(View.INVISIBLE);
        completedIntervalsListView.setVisibility(View.VISIBLE);

        if (isRunning && !isCompleted){
            setDistanceProgress(distanceCovered);
            timerProgressWheel.setVisibility(View.INVISIBLE);
            distanceProgressWheel.setVisibility(View.VISIBLE);

        }else if (!isRunning && !isCompleted){
            final int step = (int)( 360000 / intervalTime );
            distanceProgressWheel.setVisibility(View.INVISIBLE);
            timerProgressWheel.setVisibility(View.VISIBLE);
            timerProgressWheel.setText(millisecondsLeft / 1000 + " secs");

//            timerProgressWheel.setProgress((int) ((millisecondsLeft/intervalTime) * 360));
            Log.v("LATLNG", "Step is: " + step);


            if (!(isMyServiceRunning()))
            startRunningService();

            if (countDownTimer!=null) countDownTimer.cancel();

            countDownTimer =   new CountDownTimer(millisecondsLeft, 1000) {
                public void onTick(long millisUntilFinished) {
                    timerProgressWheel.setText(millisUntilFinished/1000+" secs");
                    timerProgressWheel.setProgress((int) (timerProgressWheel.getProgress() - step));
                }
                public void onFinish() {
                    timerProgressWheel.setVisibility(View.INVISIBLE);
                    timerProgressWheel.setProgress(0);
                    distanceProgressWheel.setText(0+" / "+(int)intervalDistance);
                    distanceProgressWheel.setProgress(0);
                    distanceProgressWheel.setVisibility(View.VISIBLE);



                }
            }.start();

        }else if (isCompleted){
            doStop();
        }
    }


    private void prepareForNextInterval(boolean completed){

        if (completed){

            doStop();

        }else {

//        stopRunningService(false);
            distanceProgressWheel.setVisibility(View.INVISIBLE);
            distanceProgressWheel.setProgress(0);
            distanceProgressWheel.setText(0 + " / " + (int) intervalDistance);
            timerProgressWheel.setVisibility(View.VISIBLE);
            final int step = (int) (360000 / intervalTime);
            countDownTimer = new CountDownTimer(intervalTime, 1000) {
                public void onTick(long millisUntilFinished) {
                    timerProgressWheel.setText(millisUntilFinished / 1000 + " secs");
                    timerProgressWheel.setProgress((int) (timerProgressWheel.getProgress() - step));
                }

                public void onFinish() {
                    timerProgressWheel.setVisibility(View.INVISIBLE);
                    timerProgressWheel.setProgress(0);
                    distanceProgressWheel.setVisibility(View.VISIBLE);
//                getUpdates(false);
                    completedIntervalsListView.setVisibility(View.VISIBLE);
                }
            }.start();


        }

    }

    private void setDistanceProgress(float progress){

            distanceProgressWheel.setProgress(((int)((progress/intervalDistance) *360)));
            distanceProgressWheel.setText((int)progress+" / "+(int)intervalDistance);

    }


//    public boolean selectProvider(){
//
//        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//        provider =  (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) ? LocationManager.GPS_PROVIDER : null;
//
//        if (provider==null){
////            noGps.setVisibility(View.VISIBLE);
////            Toast.makeText(getActivity(), "Please enable location services",
////                    Toast.LENGTH_SHORT).show();
//            return false;
//        }else {
//            return true;
//
//        }
//
//    }


    public void resetAppPrefs(){
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.remove(RunningService.LATLONLIST);
        editor.remove(RunningService.TOTAL_DIST);
        editor.remove(RunningService.TOTAL_TIME);
        editor.remove(RunningService.INTERVAL_ROUNDS);
        editor.remove(RunningService.INTERVAL_COMPLETED);
        editor.remove(RunningService.INTERVAL_TIME);
        editor.remove(RunningService.INTERVAL_DISTANCE);
        editor.remove(RunningService.INTERVAL_ROUNDS);
        editor.remove(RunningService.INTERVALS);
        editor.remove(RunningService.IS_RUNNING);
        editor.remove(RunningService.COUNTDOWN_REMAINING);
        editor.remove(RunningService.MSTART_TIME);

        editor.commit();
    }

    private void clear(){

        ((ExtApplication)getActivity().getApplication()).setInRunningMode(false);

        intervalDistance = 0;
        intervalTime = 0;
        intervalsList.clear();
        buttonBack.setVisibility(View.VISIBLE);

//        totalTime = 0;
        clearViews();
        hideFrame();
    }


    public void setListeners(View v){



        buttonSetIntervalValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



//                if (provider!=null && validateDistanceAndTime())
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && validateDistanceAndTime()){
                    if (isMyServiceRunning()){
                        stopRunningService();
                        SharedPreferences.Editor editor = app_preferences.edit();
                        editor.putBoolean("SEARCHING" , false);
                        editor.commit();
                    }

                    showFrame();
                }
                else if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)))
                    Toast.makeText(getActivity(), "Please enable GPS", Toast.LENGTH_SHORT).show();
            }
        });


        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                clear();


            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveRunWithIntervalsDB();
            }
        });



        buttonSavePlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                alertDialogPlan();

            }
        });





    }


    private void alertDialogPlan(){

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
// ...Irrelevant code for customizing the buttons and title
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.new_plan_alert_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Confirm new plan");

        dialogBuilder
                .setCancelable(false)
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Save", null);

        final AlertDialog alertDialog = dialogBuilder.create();


        final EditText descText = (EditText) dialogView.findViewById(R.id.dialogDesc);
        ((TextView) dialogView.findViewById(R.id.dialogMeters)).setText("Run "+intervalDistancePicker.getValue()+" meters");
        ((TextView) dialogView.findViewById(R.id.dialogSeconds)).setText("With "+intervalTimePicker.getValue()+" secs rest");
        final TextView error = ((TextView) dialogView.findViewById(R.id.dialogError));


        if (intervalRoundsPicker.getValue()>0)
            ((TextView) dialogView.findViewById(R.id.dialogRounds)).setText("Repeat "+intervalRoundsPicker.getValue()+" times");
        else  (dialogView.findViewById(R.id.dialogRounds)).setVisibility(View.GONE);






        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                       dialog.dismiss();

                    }
                });

                 b = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Do something

                        if (descText.getText().toString().length()>0 && descText.getText().length()<21){
                            savePlan(descText.getText().toString());
                            error.setVisibility(View.INVISIBLE);
                            alertDialog.dismiss();

                        }
                        else
                            error.setVisibility(View.VISIBLE);

                    }
                });
            }
        });





        alertDialog.show();




    }



    //todo only in pro version
    public  void getPlansFromDb(){
        Database db = new Database(getActivity());

        plans.clear();

        List<Plan> newPlans = db.fetchPlansFromDb();
        plans.add(new Plan("Select a plan"));

        for (Plan plan : newPlans){
            plans.add(plan);
        }

        if (plans.size()==1) plansSpinner.setVisibility(View.GONE);
        else if (plansAdapter!=null) {plansAdapter.notifyDataSetChanged();plansSpinner.setVisibility(View.VISIBLE);}
    }

    private void savePlan(String desc){

        intervalDistance = intervalDistancePicker.getValue();
        intervalTime = intervalTimePicker.getValue();
        rounds = intervalRoundsPicker.getValue();

        Database db = new Database(getActivity().getApplicationContext());
        db.addPlan(new Plan(-1, desc, (int)intervalDistance, (int)intervalTime, rounds));

        getPlansFromDb();
        plansSpinner.setVisibility(View.VISIBLE);



        Toast.makeText(getActivity(), "Plan saved", Toast.LENGTH_SHORT).show();
    }

    private boolean validateDistanceAndTime(){

        try {

            intervalDistance = ((float)intervalDistancePicker.getValue());
            intervalTime = Long.valueOf(intervalTimePicker.getValue()*1000);
            rounds = intervalRoundsPicker.getValue();

//            timerProgressWheel.setStartAngle(30);

            if (intervalDistance < 50){
                Toast.makeText(getActivity(),"Enter a valid distance in meters >= 50", Toast.LENGTH_SHORT).show();
                return false;
            }else if (intervalTime<10){
                Toast.makeText(getActivity(),"Interval time cannot be less than 10", Toast.LENGTH_SHORT).show();
                intervalTimePicker.clearFocus();
                return false;
            }

        }catch (Exception e){
            Toast.makeText(getActivity(),"Enter a valid distance in meters", Toast.LENGTH_LONG).show();
            return false;
        }

        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(buttonSetIntervalValues.getWindowToken(), 0);

        return true;
    }

    private void fixListAndAdapter( List<Interval> intervals ){
        if (intervals!=null && intervals.size()>0){
            intervalsList.clear();
            for (Interval interval : intervals){
                intervalsList.add(interval);
            }
            adapterInterval.notifyDataSetChanged();
        }
    }

    /* Request updates at startup */
    @Override
    public void onResume() {
        super.onResume();

        Log.v("LIFECYCLE", "RESUME");

//        if (isMyServiceRunning()){
//            Toast.makeText(getActivity(), "service on", Toast.LENGTH_SHORT).show();
//        }


        //service is on and i am running
        if (isMyServiceRunning() && !(app_preferences.getBoolean("SEARCHING", false))){

            intervalDistance = app_preferences.getFloat(RunningService.INTERVAL_DISTANCE, 0);
            intervalTime = app_preferences.getLong(RunningService.INTERVAL_TIME, 0);
            Gson gson = new Gson();
            Type listOfObjects = new TypeToken<List<Interval>>(){}.getType();
            String intervalsGson = app_preferences.getString(RunningService.INTERVALS,"");
            List <Interval>intervals = (List<Interval>) gson.fromJson(intervalsGson, listOfObjects);

            fixListAndAdapter(intervals);

            getActivity().registerReceiver(receiver, new IntentFilter(RunningService.NOTIFICATION));




                getInRunningMode(app_preferences.getBoolean(RunningService.IS_RUNNING, false),
                        app_preferences.getBoolean(RunningService.INTERVAL_COMPLETED, false),
                        app_preferences.getLong(RunningService.COUNTDOWN_REMAINING, 0),
                        app_preferences.getFloat(RunningService.TOTAL_DIST, 0));


        }
        //service is on and i am searching for loc fix
        else  if (isMyServiceRunning() && (app_preferences.getBoolean("SEARCHING", false))){
            getActivity().registerReceiver(receiver, new IntentFilter(RunningService.NOTIFICATION));

        }
        //service not running and i start loc fix update
        else {
//            toggleNoGps(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)));

            Log.v("SERVICE", "getting location..");

            if ((locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) /**&& (lastLocation == null)**/  ) {

                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putBoolean("SEARCHING", true);
                editor.commit();
                Intent intent = new Intent(getActivity().getBaseContext(), RunningService.class);
                intent.putExtra(RunningService.SINGLE_UPDATE, true);
                getActivity().startService(intent);
                getActivity().registerReceiver(receiver, new IntentFilter(RunningService.NOTIFICATION));

            }
        }

        }



    //
//    /* Remove the locationlistener updates when Activity is paused */
    @Override
    public void onPause() {

//        Toast.makeText(getActivity(),"pause",Toast.LENGTH_SHORT).show();

        if (isMyServiceRunning() && app_preferences.getBoolean("SEARCHING", false)){
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putBoolean("SEARCHING", false);
            editor.commit();
            stopRunningService();
        }



        Log.v("LIFECYCLE", "PAUSE");

        super.onPause();
    }


    @Override
    public void onStop() {

//        Toast.makeText(getActivity(),"stop",Toast.LENGTH_SHORT).show();

        try {
            if (isMyServiceRunning())
                getActivity().unregisterReceiver(receiver);
        }catch(Exception e){
            Log.v("LATLNG", "Exception: Receiver not registered");
            e.printStackTrace();
        }


        Log.v("LIFECYCLE", "STOP");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.v("LIFECYCLE", "DESTROY");
        Log.v("LATLNG", "Destroy");

        super.onDestroy();
    }

    @Override
    public void onStart() {
        Log.v("LIFECYCLE", "START");
        super.onStart();
    }



    public void getLastLocation(View v){




        Location bestLocation = null;

        long minTime=Long.MAX_VALUE;  float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MIN_VALUE;

        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider: matchingProviders) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();



                if ((time > minTime && accuracy < bestAccuracy)) {
                    bestLocation = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if (time < minTime &&
                        bestAccuracy == Float.MAX_VALUE && time > bestTime){
                    bestLocation = location;
                    bestTime = time;
                }
            }
        }
        lastLocation =  bestLocation;

        if (lastLocation!=null) {
            TextView myAddress = (TextView) v.findViewById(R.id.myAddressText);
            myAddress.setText("Currently at: " + getMyLocationAddress(lastLocation.getLatitude(), lastLocation.getLongitude()));

        }

    }

    private void setAddressTextAndMap(){
        if (lastLocation!=null) {
            TextView myAddress = (TextView) getView().findViewById(R.id.myAddressText);

            try {
                myAddress.setText("Currently at: " + getMyLocationAddress(lastLocation.getLatitude(), lastLocation.getLongitude()));

            }catch (Exception e){
                Log.v("SERVICE", "could not find address of location");
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 12));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(12), 1000, null);

        }
    }


//    private Runnable mUpdateTimeTask = new Runnable(){
//
//        public void run() {
//
//            final long start = mStartTime;
//            totalTime = SystemClock.uptimeMillis()- start;
//
//            int seconds = (int) (totalTime / 1000);
//            int minutes = seconds / 60;
//            seconds = seconds % 60;
//
////            textChalTimer.setText("" + minutes + ":"
////                    + String.format("%02d", seconds));
//
//            timerStop1 = minutes + ":"
//                    + String.format("%02d", seconds);
//
//            mHandler.postDelayed(this, 200);
//
//
//        }
//    };


    static FrgInterval init(int val) {
        FrgInterval truitonList = new FrgInterval();

        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);
        return truitonList;
    }




    private void clearViews(){

         buttonStart.setVisibility(View.VISIBLE);
         buttonStop.setVisibility(View.INVISIBLE);
        buttonBack.setVisibility(View.VISIBLE);
        layoutBottomButtons.setVisibility(View.INVISIBLE);
         timerProgressWheel.setVisibility(View.INVISIBLE);
        distanceProgressWheel.setVisibility(View.INVISIBLE);

    }




    private void showFrame(){
        flipper.setDisplayedChild(1);


    }

    private void hideFrame(){
        flipper.setDisplayedChild(0);
    }

    public void setSaveListener(){


        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                        if (   locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                            getInRunningMode(false, false, intervalTime, 0);
                        }else{
                            Toast.makeText(getActivity(), "Please enable GPS", Toast.LENGTH_SHORT).show();
                        }
                    }

        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmStop();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideFrame();
            }
        });


    }


    private void confirmStop(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        alertDialogBuilder.setTitle("Quit Interval")
                .setMessage("Stop running now?")
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        doStop();

                    }

                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void doStop(){

        stopRunningService();
        if (countDownTimer!=null)
        countDownTimer.cancel();
        timerProgressWheel.setProgress(0);
        timerProgressWheel.setVisibility(View.INVISIBLE);
        distanceProgressWheel.setProgress(0);
        distanceProgressWheel.setVisibility(View.INVISIBLE);
        resetAppPrefs();
        buttonStop.setVisibility(View.INVISIBLE);
        layoutBottomButtons.setVisibility(View.VISIBLE);

        if (intervalsList.size()==0) buttonDismiss.performClick();
    }

    private void saveRunWithIntervalsDB(){



        Running running = new Running(-1, "", intervalTime,  new SimpleDateFormat("dd/MM/yyyy, hh:mm a").format(new Date()), intervalDistance, intervalsList );
        Database db = new Database(getActivity().getApplicationContext());

         db.addRunning(running);

        Toast.makeText(getActivity(), "Saved in Diary", Toast.LENGTH_SHORT).show();

       clear();
    }




    private void stopRunningService(){
        Log.v("LATLNG","STOP SERVICE");


            try {

                getActivity().stopService(new Intent(getActivity().getBaseContext(), RunningService.class));
                getActivity().unregisterReceiver(receiver);


                NotificationManager mNotifyMgr =
                    (NotificationManager) getActivity().getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                //Builds the notification and issues it.
                mNotifyMgr.cancel(123);





            }catch (Exception e){
                Log.v("LATLNG", "Exception: Receiver was not registered");
            }




    }


    private void startRunningService() {



        Log.v("LATLNG", "START SERVICE");
        Intent intent = new Intent(getActivity().getBaseContext(), RunningService.class);
        intent.putExtra(RunningService.INTERVAL_DISTANCE, intervalDistance);
        intent.putExtra(RunningService.INTERVAL_TIME, intervalTime);
        intent.putExtra(RunningService.INTERVAL_ROUNDS, rounds);
        getActivity().startService(intent);

//        if (firstTime)
        getActivity().registerReceiver(receiver, new IntentFilter(RunningService.NOTIFICATION));


        if (Build.VERSION.SDK_INT > 15) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getActivity());
            mBuilder.setSmallIcon(R.drawable.interval_flag);
            mBuilder.setContentTitle("Interval in progress");
            mBuilder.setContentText("Click to get into");
            mBuilder.setOngoing(true);

            Intent resultIntent = new Intent(getActivity(), MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
            stackBuilder.addParentStack(MainActivity.class);

// Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);


            Notification notification = mBuilder.build();

            notification.ledARGB = getResources().getColor(R.color.interval_green);
            notification.flags = Notification.FLAG_SHOW_LIGHTS;
            notification.ledOffMS = 500;
            notification.ledOnMS = 1500;


            NotificationManager notificationManager =
                    (NotificationManager) getActivity().getSystemService(android.content.Context.NOTIFICATION_SERVICE);


            // notificationID allows you to update the notification later on.
            notificationManager.notify(123, notification);
        }


    }


//    public void toggleNoGps(boolean show){
//        if (!show){
////            provider = LocationManager.GPS_PROVIDER;
//            getView().findViewById(R.id.noGps).setVisibility(View.INVISIBLE);
//            buttonSetIntervalValues.setBackgroundDrawable(getResources().getDrawable(R.drawable.plus_minus_selector));
//        }else{
////            provider = null;
//            getView().findViewById(R.id.noGps).setVisibility(View.VISIBLE);
//            buttonSetIntervalValues.setBackgroundDrawable(getResources().getDrawable(R.drawable.inactive_button_selector));
//
//        }
//    }


    public String getMyLocationAddress(double lat, double lon) {

        Geocoder geocoder= new Geocoder(getActivity(), Locale.ENGLISH);

        try {

            //Place your latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

            if((addresses != null) && (addresses.size() > 0)) {

                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder();

                for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
                }


                Log.v("LATLNG","I am at: " +strAddress.toString());
                return strAddress.toString() ;

            }

            else
                Log.v("LATLNG", "No location found..!");
            return "No address for this location";

        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "No address for this location";
        }
    }


    public void initializeMap(){
        SupportMapFragment fm = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFrgKostas);
        googleMap = fm.getMap();
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.setIndoorEnabled(false);
        googleMap.setMyLocationEnabled(true);
        if (lastLocation!=null){
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 12));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
        }

    }

    private void loadPlan(int position){
        Plan plan = plans.get(position);
        intervalTimePicker.setValue(plan.getSeconds());
        intervalDistancePicker.setValue(plan.getMeters());
        intervalRoundsPicker.setValue(plan.getRounds());
        intervalRoundsPicker.disableButtonColor(plan.getRounds()==0);

        Toast.makeText(getActivity(), plan.getDescription()+" loaded", Toast.LENGTH_SHORT).show();
    }




    class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

            if (pos>0) loadPlan(pos);

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            Toast.makeText(getActivity(), "nothing", Toast.LENGTH_SHORT).show();
            // Nothing
        }

    }


    class SpinnerAdapter  extends ArrayAdapter<Plan> {

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
            LayoutInflater inflater = getActivity().getLayoutInflater();
            planViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof planViewHolder)) {

                holder = new planViewHolder();
                    convertView = inflater.inflate(R.layout.custom_plans_spinner_item, null);
                    holder.description = (TextView) convertView.findViewById(R.id.planDescriptionSpinner);

                convertView.setTag(holder);
            } else {
                holder = (planViewHolder) convertView.getTag();
            }



                holder.description.setText(data.get(position).getDescription());



            return convertView;
        }

        //this is the spinner header
        public View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            planViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof planViewHolder)) {
                convertView = inflater.inflate(R.layout.custom_spinner_dropdown, null);
                holder = new planViewHolder();
                holder.description = (TextView) convertView.findViewById(R.id.planDescriptionSpinner2);
                holder.arrow = (ImageView) convertView.findViewById(R.id.planArrow);

                convertView.setTag(holder);
            } else {
                holder = (planViewHolder) convertView.getTag();
            }

             if (plansSpinner.isActivated()){
                 holder.arrow.setImageDrawable(getResources().getDrawable(R.drawable.arrow_up));
             }else{
                 holder.arrow.setImageDrawable(getResources().getDrawable(R.drawable.arrow_down));
             }

            holder.description.setText(data.get(position).getDescription());
            return convertView;
        }
    }

    private class planViewHolder{
        TextView description;
//        TextView info;
        ImageView arrow;
    }



}