package com.kostas.onlineHelp;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseDrawer extends BaseFragmentActivity {
//    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    static final int POSITION_NEW_INTERVAL = 0;
    static final int POSITION_MY_DIARY = 1;
    static final int POSITION_MY_PLANS = 2;
    static final int POSITION_EXIT = 3;

    static final String POSITION = "pos";
    private static final Long EXIT_CODE = -99L;
    protected ActionBarDrawerToggle mDrawerToggle;
    Map<Integer, Integer> bottomButtons;
    String[] drawerTitles;
    int[] drawerTitlesColors;
    DrawerLayout mDrawerLayout;
    private boolean logout = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerTitles = getResources().getStringArray(R.array.drawer_titles);

        drawerTitlesColors = getResources().getIntArray(R.array.drawer_titles_colors);
    }


    protected void setDrawer() {
        setDrawer(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!logout){
            closeDrawer();
        }
    }

    protected void setDrawer(NonSwipeableViewPager mPager) {

        ListView mDrawerList = getDrawerList();

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener(mPager));

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.hamburger,
                0, 0) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private ListView getDrawerList() {
        List<DrawerItem> dataList = new ArrayList<DrawerItem>();

        int drawerGrey = getResources().getColor(R.color.drawer_grey);

        Typeface tf = Typeface.create("Helvetica", Typeface.BOLD);


//        tf = Typeface.create("Helvetica", Typeface.NORMAL);
        int drawerBlack = getResources().getColor(R.color.drawer_black);
        for (int i = 0; i < drawerTitles.length; i++) {
            Drawable imgRes = null;
            if ((i == POSITION_MY_DIARY)) {
                imgRes = getResources().getDrawable(R.drawable.diary_32);
            }
            else if (i == POSITION_EXIT) {
                imgRes = getResources().getDrawable(R.drawable.exit_32);
                tf = Typeface.create("Helvetica", Typeface.BOLD);
            }
            else if (i == POSITION_MY_PLANS) {
                imgRes = getResources().getDrawable(R.drawable.plan_32);
            }
            else if (i == POSITION_NEW_INTERVAL) {
                imgRes = getResources().getDrawable(R.drawable.interval_32);
            }
//            if (app.getUserID()!=null&&app.getUserID()==-1){

//                drawerTitlesColors[1] = getResources().getColor(R.color.death_grey);
//                drawerTitlesColors[drawerTitles.length-2] = getResources().getColor(R.color.death_grey);

//                drawerTitles[drawerTitles.length-1]="Exit";
//            }
            dataList.add(new DrawerItem(drawerTitles[i],  drawerTitlesColors[i]  ,null,0,View.GONE,View.VISIBLE,
                    i%2==0?drawerBlack:drawerGrey,imgRes,tf,null,null,null,0));
        }

        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new CustomDrawerAdapter(this, R.layout.custom_drawer_item, dataList));
        return mDrawerList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (null != mDrawerToggle && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (null != mDrawerToggle) mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (null != mDrawerToggle) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public class DrawerItemClickListener implements ListView.OnItemClickListener {
        private NonSwipeableViewPager mPager;

        public DrawerItemClickListener(NonSwipeableViewPager mPager) {
            this.mPager = mPager;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            switch (position) {
                case POSITION_NEW_INTERVAL:
                    startMain(mPager, position);
                    break;

                case POSITION_MY_DIARY:
                    startMain(mPager, position);
                    break;

                case POSITION_MY_PLANS:
                    startMain(mPager, position);
                    break;

                case POSITION_EXIT:
                    finish();
//                    startMain(mPager, ActMain.MY_PLAN_PAGER_POSITION);
                    break;

                default:
                    startMain(mPager, position);
                    break;
            }

            setTitle(drawerTitles[position]);
            closeDrawer();
        }
    }



    private void startMain(NonSwipeableViewPager mPager, int position) {

        ((ExtApplication)getApplication()).setPosition(position);

        if (null != mPager) {
            mPager.setCurrentItem(position);
        } else {
            startMainWhenNoPager(position);
        }
    }

    private void startMainWhenNoPager(int position) {
        ((ExtApplication)getApplication()).setPosition(position);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
        finish();
    }

    protected void closeDrawer(){
        mDrawerLayout.closeDrawers();
    }

    final protected void onDrawerItemClicked(int position, long id) {
        if (beforeDrawerItemClicked(position, id))
            if (doDrawerItemClicked(position, id))
                afterDrawerItemClicked(position, id);
    }

    // passthru
    protected boolean beforeDrawerItemClicked(int position, long id) {
        return true;
    }

    // passthru
    protected boolean doDrawerItemClicked(int position, long id) {
        return true;
    }

    protected boolean afterDrawerItemClicked(int position, long id) {
        closeDrawer();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
