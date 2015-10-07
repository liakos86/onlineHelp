package com.kostas.onlineHelp;

import android.os.Bundle;

/**
 * Created by liakos on 5/10/2015.
 */
public class FrgSettings extends BaseFragment{


    static FrgShowRuns init(int val) {
        FrgShowRuns truitonList = new FrgShowRuns();

        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);

        return truitonList;
    }
}
