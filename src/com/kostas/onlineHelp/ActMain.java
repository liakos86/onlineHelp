package com.kostas.onlineHelp;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.LinearLayout;
import com.kostas.custom.NonSwipeableViewPager;
import com.kostas.fragments.FrgPlans;
import com.kostas.fragments.FrgShowRuns;
import com.kostas.service.RunningService;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
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

                Fragment fragment = getActiveFragment(getSupportFragmentManager(),position);
                if(fragment instanceof FrgShowRuns){
                    ((FrgShowRuns)fragment).refreshPaceTextsIfNeeded();
                }else if(fragment instanceof FrgPlans){
                    ((FrgPlans)fragment).changeNewPlanTextsIfNeeded();
                }

                invalidateOptionsMenu();
            }

            public Fragment getActiveFragment(FragmentManager fragmentManager, int position) {
                final String name = makeFragmentName(mPager.getId(), position);
                final Fragment fragmentByTag = fragmentManager.findFragmentByTag(name);
                if (fragmentByTag == null) {
                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    fragmentManager.dump(AppConstants.EMPTY, null, new PrintWriter(outputStream, true), null);
                }
                return fragmentByTag;
            }

            private String makeFragmentName(int viewId, int index) {
                return "android:switcher:" + viewId + ":" + index;
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

    /**
     * upon resuming i need to check three things.
     * 1. if the service is running and there is a run in progress or the app is in running act i need to start the Interval act.
     * 2. if the run has stopped and the app is in result, i go to results act.
     * 3. if nothing of the above, but a new run is added, i need to refresh my runs.
     */
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences app_preferences = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

//        SharedPreferences.Editor editor = app_preferences.edit();
//        editor.remove("mongoId").apply();

        if ((isMyServiceRunning() && (app_preferences.getBoolean(AppConstants.INTERVAL_IN_PROGRESS, false)))||(((ExtApplication) getApplication()).isInRunningAct())) {//service is on
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
