package com.kostas.onlineHelp;

import android.app.AlertDialog;
import android.content.*;
import android.location.Location;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Running;
import com.kostas.model.Database;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.kostas.service.RunningService.*;

public class ActIntervalResults extends BaseFrgActivityWithBottomButtons {
    SharedPreferences app_preferences;
    ListView completedIntervalsListView;
    List<Interval> intervalsList = new ArrayList<Interval>();
    IntervalAdapterItem adapterInterval;
    AdRequest adRequest;
    Button buttonSave;
    Button buttonDismiss;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        setViewsAndButtons();
        setIntervalsListAndAdapter();
        if (((ExtApplication) getApplication()).isOnline()) {
            new LoadAsyncAd().execute();
        }
        setListeners();

        ((ExtApplication) getApplication()).setInResultsAct(true);
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

    private void setViewsAndButtons() {
        app_preferences = this.getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        completedIntervalsListView = (ListView) findViewById(R.id.completedIntervals);
        buttonDismiss = (Button) findViewById(R.id.buttonDismissInterval);
        buttonSave = (Button) findViewById(R.id.buttonSaveRunWithIntervals);
    }

    /**
     * Clears the app prefs that where changed during the finished interval session
     */
    public void resetAppPrefs() {
        SharedPreferences.Editor editor = app_preferences.edit();
        boolean hasNoSound = app_preferences.getBoolean(NO_SOUND, false);
        boolean hasNoVibration = app_preferences.getBoolean(NO_VIBRATION, false);
        editor.clear().apply();
        editor.putBoolean(NO_SOUND, hasNoSound);
        editor.putBoolean(NO_VIBRATION, hasNoVibration);
        editor.apply();
    }

    /**
     * Clears views and values after finishing an interval session
     */
    private void clear() {
        resetAppPrefs();
        intervalsList = null;
        ((ExtApplication) getApplication()).setInResultsAct(false);
        Intent intent = new Intent(this, ActMain.class);
        startActivity(intent);
        finish();
    }

    /**
     * Assigns listeners for every button in the layout
     */
    public void setListeners() {
        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDelete();
            }
        });
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveRunWithIntervalsDB();
            }
        });
    }

    private void confirmDelete() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder
                .setMessage("Delete current progress?")
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                            clear();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Stops the interval after user selection or after user has completed the selected
     * amount of intervals.
     */
    private void setIntervalsListAndAdapter() {
        Gson gson = new Gson();
        Type listOfObjects = new TypeToken<List<Interval>>() {}.getType();
        String intervalsGson = app_preferences.getString(INTERVALS, "");
        ArrayList<Interval> intervalsListJson = gson.fromJson(intervalsGson, listOfObjects);
        intervalsList = intervalsListJson != null ? intervalsListJson : new ArrayList<Interval>();

        float coveredDist = app_preferences.getFloat(TOTAL_DIST, 0);
        if (coveredDist > 0) {//an interrupted run must be added to list
            Type listOfLocation = new TypeToken<List<Location>>() {}.getType();
            String locsGson = app_preferences.getString(LATLONLIST, "");
            List<Location> locationList = gson.fromJson(locsGson, listOfLocation);
            StringBuilder sb = new StringBuilder(locationList.get(0).getLatitude() + "," + locationList.get(0).getLongitude());
            locationList.remove(0);
            for (Location l : locationList) {
                sb.append("," + l.getLatitude() + "," + l.getLongitude() + "," + l.getLatitude() + "," + l.getLongitude());
            }

            long millis = SystemClock.uptimeMillis() - app_preferences.getLong(MSTART_TIME, 0);
            intervalsList.add(new Interval(-1, sb.toString(), millis, coveredDist));
        }

        adapterInterval = new IntervalAdapterItem(this, this.getApplicationContext(),
                R.layout.list_interval_row, intervalsList);
        completedIntervalsListView.setAdapter(adapterInterval);
    }

    /**
     * Save the run and every interval in the list in the db
     */
    private void saveRunWithIntervalsDB() {
        buttonDismiss.setClickable(false);
        buttonSave.setClickable(false);
        long intervalTime = app_preferences.getLong(INTERVAL_TIME, 0);
        float intervalDistance = app_preferences.getFloat(INTERVAL_DISTANCE, 0);
        Running running = new Running(-1, "", intervalTime, new SimpleDateFormat("dd/MM/yyyy, hh:mm a").format(new Date()), intervalDistance, intervalsList);
        Database db = new Database(getApplicationContext());
        db.addRunning(running);
        ((ExtApplication) getApplication()).setNewIntervalInDb(true);
        //Toast.makeText(getApplication(), "Saved in Diary", Toast.LENGTH_SHORT).show();
        clear();
    }


    private class LoadAsyncAd extends AsyncTask<Void, Void, Void> {

        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... unused) {
            placeAds();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ((AdView)findViewById(R.id.adViewInterval2)).loadAd(adRequest);
        }

    }


    /**
     *  If the service is running and also the mode is INTERVAL_IN_PROGRESS i need to start the receiver,
     *  get the values from app_prefs and fix every ui element based on these.
     **/
    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {

        confirmDelete();

    }
}