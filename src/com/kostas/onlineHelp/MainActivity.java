package com.kostas.onlineHelp;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.kostas.service.RunningService;

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
                startNewInterval(mPager, false);
        }else{
            MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
            if (((ExtApplication) getApplication()).isNewIntervalInDb()) {
                ((FrgShowRuns)adapter.fragments[0]).getRunsFromDb(this, false);
                ((ExtApplication) getApplication()).setNewIntervalInDb(false);
            }
        }
    }
}
