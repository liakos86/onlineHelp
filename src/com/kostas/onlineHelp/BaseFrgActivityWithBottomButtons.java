package com.kostas.onlineHelp;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.LinearLayout;


import java.util.HashMap;
import java.util.Map;

/**
 * A fragment activity with a bottom button layout
 */
public class BaseFrgActivityWithBottomButtons extends FragmentActivity {
    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    /**
     * The bottom button position of the diary.
     */
    static final int POSITION_MY_DIARY = 0;

    /**
     * The bottom button position of the plans.
     */
    static final int POSITION_MY_PLANS = 1;

    /**
     * The bottom button position of the new interval.
     */
    static final int POSITION_NEW_INTERVAL = 2;

    /**
     * Key represents the position of the button while value the layoutId of the button
     */
    Map<Integer, Integer> bottomButtons;

    String []drawerTitles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerTitles = getResources().getStringArray(R.array.drawer_titles);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showBottomButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Populates the hashmap of the buttons and assigns a listener to each one of them
     *
     * @param mPager
     */
    protected void setBottomButtons(NonSwipeableViewPager mPager) {
        bottomButtons = new HashMap<Integer, Integer>();
        bottomButtons.put(0, R.id.btn_my_runs);
        bottomButtons.put(1, R.id.btn_my_plans);
        bottomButtons.put(2, R.id.btn_new_interval);
        for (int counter = 0; counter < MainActivity.PAGER_SIZE+1; counter++) {
            setBottomButtonListener(mPager, bottomButtons.get(counter), counter);
        }
    }

    /**
     * Sets the global position of the bottom buttons and then starts the
     * appropriate fragment or the new interval activity
     *
     * @param mPager
     * @param position the position that the user selected
     */
    private void startMain(NonSwipeableViewPager mPager, int position) {
        if (null != mPager) {
            mPager.setCurrentItem(position);
        } else {
            startMainWhenNoPager(position);
        }
    }

    /**
     * Restarts the activity if the mPager is null.
     *
     * @param position
     */
    private void startMainWhenNoPager(int position) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
        finish();
    }

    /**
     * Starts a new IntervalActivity
     * Firstly it calls the onExit() of the selected fragment to display progress bar
     * Then hides bottom buttons and starts activity
     * @param mPager
     */
    protected void startNewInterval(NonSwipeableViewPager mPager, boolean shouldMakeFragmentLoading) {
        if(!(this instanceof ActivityIntervalNew)){
            Intent intent = new Intent(this, ActivityIntervalNew.class);
            startActivity(intent);

            if (shouldMakeFragmentLoading) {
                MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
                int position = ((ExtApplication) getApplication()).getPosition();
                ((LoadingOnExitFragment) adapter.fragments[position]).onExit();
            }
            hideBottomButtons();
            //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    private void hideBottomButtons(){
        LinearLayout bottomButtons = (LinearLayout) findViewById(R.id.bottomButtons);
            bottomButtons.setVisibility(View.GONE);
    }

    private void showBottomButtons(){
        LinearLayout bottomButtons = (LinearLayout) findViewById(R.id.bottomButtons);
        if (!((ExtApplication) getApplication()).isInRunningAct())
        bottomButtons.setVisibility(View.VISIBLE);
    }

    /**
     * If the positions is 0 or 1 we just change the displayed fragment
     * If the position is 2 we start a new IntervalActivity
     *
     * @param mPager
     * @param btn the xml id of the button
     * @param position the position of the button in the layout
     */
    private void setBottomButtonListener(final NonSwipeableViewPager mPager, int btn, final int position) {
        LinearLayout bottomButton = (LinearLayout) findViewById(btn);
        bottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position != POSITION_NEW_INTERVAL)
                    startMain(mPager, position);
                else
                    startNewInterval(mPager, true);
                }

        });
    }
}
