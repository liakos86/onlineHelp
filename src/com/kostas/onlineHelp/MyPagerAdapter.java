package com.kostas.onlineHelp;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.kostas.fragments.FrgFriends;
import com.kostas.fragments.FrgPlans;
import com.kostas.fragments.FrgSettings;
import com.kostas.fragments.FrgShowRuns;


/**
 * Pager adapter class.
 *
 */
public class MyPagerAdapter extends FragmentPagerAdapter {

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
            case 2: {
                if (fragments[position] == null){
                    fragments[position] = FrgFriends.init(2);
                }
                break;
            }
            case 3: {
                if (fragments[position] == null){
                    fragments[position] = FrgSettings.init(3);
                }
                break;
            }
        }
        return fragments[position];
    }

}