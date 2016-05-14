package com.kostas.onlineHelp;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.LinearLayout;
import com.kostas.custom.NonSwipeableViewPager;


import java.util.HashMap;
import java.util.Map;

/**
 * A fragment activity with a bottom button layout
 */
public class BaseFrgActivityWithBottomButtons extends FragmentActivity {
    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    /**
     * Key represents the position of the button while value the layoutId of the button
     */
    Map<Integer, Integer> bottomButtons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //showBottomButtons();
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
        bottomButtons.put(2, R.id.btn_settings);
        for (int counter = 0; counter < ActMain.PAGER_SIZE; counter++) {
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
        Intent intent = new Intent(this, ActMain.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        finish();
    }

    /**
     * Starts a new IntervalActivity
     */
     public void startNewInterval() {
        if(!(this instanceof ActIntervalNew)){
            Intent intent = new Intent(this, ActIntervalNew.class);
            startActivity(intent);
        }
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
                    startMain(mPager, position);
                }

        });
    }
}
