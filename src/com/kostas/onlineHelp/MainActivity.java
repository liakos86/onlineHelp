package com.kostas.onlineHelp;

//import com.facebook.FacebookSdk;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.kostas.service.RunningService;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * The main activity that contains the pager to choose fragment or new interval
 */
public class MainActivity extends BaseFrgActivityWithBottomButtons {

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
    static final int PAGER_SIZE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setupPager();

//        FacebookSdk.sdkInitialize(getApplicationContext());
    }

    /**
     * Initializes the pager, sets adapter and listener
     * Sets the bottom buttons.
     */
    private void setupPager() {
        mPager = (NonSwipeableViewPager) findViewById(R.id.pager);

        mPager.setOffscreenPageLimit(1);

        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(),  PAGER_SIZE));

        setBottomButtons(mPager);
        setSelectedBottomButton(bottomButtons, 0);

        final Activity activity = this;
        mPager.setOnPageChangeListener(new NonSwipeableViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int position) {
                mPager.setCurrentItem(position);
                setSelectedBottomButton(bottomButtons, position);
//                Fragment current = ((MyPagerAdapter) mPager.getAdapter()).fragments[position];
//                if (current instanceof FrgShowRuns) {
//                    ((FrgShowRuns)current).getRunsFromDb(activity, false);
//                }else  if (current instanceof FrgPlans) {
//                    ((FrgPlans)current).getPlansFromDb(activity, false);
//                }
                invalidateOptionsMenu();
            }

//            public Fragment getActiveFragment(FragmentManager fragmentManager, int position) {
//                final String name = makeFragmentName(mPager.getId(), position);
//                final Fragment fragmentByTag = fragmentManager.findFragmentByTag(name);
//                if (fragmentByTag == null) {
//                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                    fragmentManager.dump("", null, new PrintWriter(outputStream, true), null);
//                }
//                return fragmentByTag;
//            }
//
//            private String makeFragmentName(int viewId, int index) {
//                return "android:switcher:" + viewId + ":" + index;
//            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

    }

    /**
     * Pager adapter class.
     *
     */
    private class MyPagerAdapter extends FragmentPagerAdapter {

        Fragment[] fragments;
        Activity activity;

        public MyPagerAdapter(FragmentManager supportFragmentManager, int pageCount) {
            super(supportFragmentManager);
            fragments = new Fragment[pageCount];
            for (int i = 0; i < fragments.length; i++)
                fragments[i] = null;
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

       @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: {

                    if (fragments[position] == null){
                        fragments[position] = FrgShowRuns.init(0);
                    }
                    break;
                }
                case 1: {
                    if (fragments[position] == null){
                        fragments[position] = FrgPlans.init(1);
                    }
                    break;
                }
            }
           return fragments[position];
        }

    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
//        if  (getTitle().toString().equals(drawerTitles[0])) {
//
//            if (((ExtApplication) getApplicationContext()).isInRunningMode()) {
//                Toast.makeText(getApplication(), "Interval in progress", Toast.LENGTH_SHORT).show();
//            } else {
//                //todo check why it is not working
//                super.onBackPressed();
////            finish();
//            }
//        }else{
//            mPager.setCurrentItem(0, true);
//            setTitle(drawerTitles[0]);
//        }
    }

    /**
     * Sets the state of the pressed button to 'selected'
     *
     * @param bottomButtons
     * @param postion
     */
    private void setSelectedBottomButton(Map<Integer, Integer> bottomButtons, int postion) {
        for (int key = 0; key < bottomButtons.size(); key++) {
            LinearLayout btn = (LinearLayout) findViewById(bottomButtons.get(key));
            btn.setSelected(key != postion ? false : true);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (isMyServiceRunning() || ((ExtApplication) getApplication()).isInRunningAct()) {//service is on
                startNewInterval();
        }else{
            MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
            if (((ExtApplication) getApplication()).isNewIntervalInDb()) {
                ((FrgShowRuns)adapter.fragments[0]).getRunsFromDb(this, false);
                ((ExtApplication) getApplication()).setNewIntervalInDb(false);
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }


}
