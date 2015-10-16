package com.kostas.onlineHelp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.widget.Toast;
import com.kostas.service.RunningService;


import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class MainActivity extends BaseDrawer {

    public static final String PREFS_NAME = "INTERVAL_PREFS";
    /**
     * Called when the activity is first created.
     */

    private NonSwipeableViewPager mPager;
    static final int PAGER_SIZE = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getPager();
        setDrawer(mPager);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }



    private void getPager() {
        mPager = (NonSwipeableViewPager) findViewById(R.id.pager);

        mPager.setOffscreenPageLimit(2);

        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(),  PAGER_SIZE));

//        setBottomButtons(mPager);

//        pagerTitles = getResources().getStringArray(R.array.pager_titles);


        final Activity activity = this;
        mPager.setOnPageChangeListener(new NonSwipeableViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

//                mPager.setCurrentItem(i);
//                setSelectedBottomButton(bottomButtons,i);

            }

            @Override
            public void onPageSelected(int position) {

                mPager.setCurrentItem(position);
//                setSelectedBottomButton(bottomButtons,position);


                if (getActiveFragment(getSupportFragmentManager(), position) instanceof FrgShowRuns) {
                    ((FrgShowRuns)getActiveFragment(getSupportFragmentManager(), position)).getRunsFromDb(activity, false);

                }else  if (getActiveFragment(getSupportFragmentManager(), position) instanceof FrgPlans) {
                    ((FrgPlans)getActiveFragment(getSupportFragmentManager(), position)).getPlansFromDb(activity, false);

                }else  if (getActiveFragment(getSupportFragmentManager(), position) instanceof FrgInterval) {
                    ((FrgInterval)getActiveFragment(getSupportFragmentManager(), position)).getPlansFromDb(activity, false);

                }


                invalidateOptionsMenu();
            }

            public Fragment getActiveFragment(FragmentManager fragmentManager, int position) {
                final String name = makeFragmentName(mPager.getId(), position);
                final Fragment fragmentByTag = fragmentManager.findFragmentByTag(name);
                if (fragmentByTag == null) {
                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    fragmentManager.dump("", null, new PrintWriter(outputStream, true), null);
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
                    return FrgInterval.init(0);

                }
                case 1: {
                    return FrgShowRuns.init(1);
                }
                case 2: {
                    return FrgPlans.init(2);
                }
                case 3: {
                    return FrgSettings.init(3);
                }
                default: return FrgInterval.init(position);


            }
        }

    }


    @Override
    public void onBackPressed() {

        if (((ExtApplication) getApplicationContext()).isInRunningMode()){
            Toast.makeText(getApplication(), "Interval in progress", Toast.LENGTH_SHORT).show();
        }
        else {
            //todo check why it is not working
            super.onBackPressed();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

//    public void doAsync(int position){
//        new PerformAsyncTask(this, position).execute();
//    }
//
//
//    public class PerformAsyncTask extends AsyncTask<Void, Void, Void> {
//        private Activity activity;
//        private int whichFragment;
//
//
//
//        public PerformAsyncTask(Activity activity, int whichFragment) {
//            this.activity = activity;
//            this.whichFragment = whichFragment;
//
//        }
//
//        protected void onPreExecute() {
//
//        }
//
//        @Override
//        protected Void doInBackground(Void... unused) {
//
//             String name =
//            "android:switcher:" + mPager.getId() + ":" + whichFragment;
//
//            if (whichFragment==0) {
//                ((FrgInterval)getSupportFragmentManager().findFragmentByTag(name)).getPlansFromDb(activity);
//
//            }else  if (whichFragment==1) {
//                ((FrgShowRuns)getSupportFragmentManager().findFragmentByTag(name)).getRunsFromDb(activity);
//
//            }else  if (whichFragment==2) {
//                ((FrgPlans)getSupportFragmentManager().findFragmentByTag(name)).getPlansFromDb(activity);
//
//            }
//            return null;
//
//        }
//
//        @Override
//        protected void onPostExecute(Void result) {
//
//        }
//
//    }


}
