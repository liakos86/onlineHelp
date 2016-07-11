package com.kostas.onlineHelp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.kostas.custom.NonSwipeableViewPager;
import com.kostas.dbObjects.User;
import com.kostas.fragments.FrgShowRuns;
import com.kostas.mongo.SyncHelper;
import com.kostas.service.RunningService;

import java.util.Map;

/**
 * The activity_main activity that contains the pager to choose fragment or new interval
 */
public class ActMain extends BaseFrgActivityWithBottomButtons {

    /**
     * Shared prefs name
     */
    public static final String PREFS_NAME = "INTERVAL_PREFS";

    /**
     * A NonSwipeableViewPager that does not allow swiping
     */
    private NonSwipeableViewPager mPager;

    /**
     * The total size of the pager objects
     */
    static final int PAGER_SIZE = 4;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupPager();


//        SharedPreferences app_preferences = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//
//        if (app_preferences.getString("mongoId", null) != null) {
//            new AsyncLoadFriends((ExtApplication)getApplication()).execute();
//        }

//        FacebookSdk.sdkInitialize(getApplicationContext());
    }



    /**
     * Initializes the pager, sets adapter and listener
     * Sets the bottom buttons.
     */
    private void setupPager() {
        mPager = (NonSwipeableViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(3);
        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(),  PAGER_SIZE));
        setBottomButtons(mPager);
        setSelectedBottomButton(bottomButtons, 0);

        mPager.setOnPageChangeListener(new NonSwipeableViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int position) {
                ((ExtApplication) getApplication()).setPosition(position);
                mPager.setCurrentItem(position);
                setSelectedBottomButton(bottomButtons, position);
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    /**
     * Sets the state of the pressed button to 'selected'
     *
     * @param bottomButtons
     * @param position
     */
    private void setSelectedBottomButton(Map<Integer, Integer> bottomButtons, int position) {
        for (int key = 0; key < bottomButtons.size(); key++) {
            LinearLayout btn = (LinearLayout) findViewById(bottomButtons.get(key));
            btn.setSelected(key == position);
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

//    private class AsyncLoadFriends extends AsyncTask<Void, Void, Integer> {
//        private ExtApplication application;
//
//        public AsyncLoadFriends(ExtApplication application) {
//            this.application = application;
//        }
//
//        protected void onPreExecute() {
//
//        }
//
//        @Override
//        protected Integer doInBackground(Void... unused) {
//
//
//            SyncHelper sh = new SyncHelper(application);
//         return sh.getMyMongoUser();
//
//
//
//
//        }
//
//        @Override
//        protected void onPostExecute(Integer result) {
//
//            if (result == -1){
//                Toast.makeText(getApplication(), "User profile could not be loaded", Toast.LENGTH_SHORT).show();
//            }
//
//        }
//
//
//    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences app_preferences = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

//        SharedPreferences.Editor editor = app_preferences.edit();
//        editor.remove("mongoId").apply();

        if ((isMyServiceRunning() && (app_preferences.getBoolean(RunningService.INTERVAL_IN_PROGRESS, false)))||(((ExtApplication) getApplication()).isInRunningAct())) {//service is on
            startIntervalAct();
        }else  if (((ExtApplication) getApplication()).isInResultsAct()) {
            startResultsAct();
        }else{
            MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
            if (((ExtApplication) getApplication()).isNewIntervalInDb() && adapter.fragments[0] != null) {
                ((FrgShowRuns)adapter.fragments[0]).refreshAfterAdd();
                ((ExtApplication) getApplication()).setNewIntervalInDb(false);
            }
        }
    }
}
