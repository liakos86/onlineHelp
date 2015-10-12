package com.kostas.onlineHelp;

import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
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

        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(), PAGER_SIZE));

//        setBottomButtons(mPager);

//        pagerTitles = getResources().getStringArray(R.array.pager_titles);


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
                    ((FrgShowRuns)getActiveFragment(getSupportFragmentManager(), position)).getRunsFromDb();

                }else  if (getActiveFragment(getSupportFragmentManager(), position) instanceof FrgPlans) {
                    ((FrgPlans)getActiveFragment(getSupportFragmentManager(), position)).getPlansFromDb();

                }else  if (getActiveFragment(getSupportFragmentManager(), position) instanceof FrgInterval) {
                    ((FrgInterval)getActiveFragment(getSupportFragmentManager(), position)).getPlansFromDb();

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
}
