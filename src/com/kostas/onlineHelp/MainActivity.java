package com.kostas.onlineHelp;

//import com.facebook.FacebookSdk;
import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

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
    static final int PAGER_SIZE = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setupPager();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
//        FacebookSdk.sdkInitialize(getApplicationContext());
    }

    /**
     * Initializes the pager, sets adapter and listener
     * Sets the bottom buttons.
     */
    private void setupPager() {
        mPager = (NonSwipeableViewPager) findViewById(R.id.pager);

        mPager.setOffscreenPageLimit(2);

        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(),  PAGER_SIZE));

        setBottomButtons(mPager);

        final Activity activity = this;
        mPager.setOnPageChangeListener(new NonSwipeableViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int position) {

                mPager.setCurrentItem(position);

                if (getActiveFragment(getSupportFragmentManager(), position) instanceof FrgShowRuns) {
                    ((FrgShowRuns)getActiveFragment(getSupportFragmentManager(), position)).getRunsFromDb(activity, false);

                }else  if (getActiveFragment(getSupportFragmentManager(), position) instanceof FrgPlans) {
                    ((FrgPlans)getActiveFragment(getSupportFragmentManager(), position)).getPlansFromDb(activity, false);

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
                    return FrgShowRuns.init(0);
                }
                case 1: {
                    return FrgPlans.init(1);
                }
                default: return FrgShowRuns.init(position);
            }
        }

    }

    @Override
    public void onBackPressed() {

        if  (getTitle().toString().equals(drawerTitles[0])) {

            if (((ExtApplication) getApplicationContext()).isInRunningMode()) {
                Toast.makeText(getApplication(), "Interval in progress", Toast.LENGTH_SHORT).show();
            } else {
                //todo check why it is not working
                super.onBackPressed();
//            finish();
            }
        }else{
            mPager.setCurrentItem(0, true);
            setTitle(drawerTitles[0]);
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