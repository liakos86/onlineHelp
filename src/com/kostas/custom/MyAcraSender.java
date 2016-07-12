package com.kostas.custom;

import android.content.Context;
import android.content.SharedPreferences;
import com.kostas.dbObjects.User;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.ActMain;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

/**
* Created by liakos on 21/9/2015.
*/

public class MyAcraSender implements ReportSender {

   ExtApplication application;

    public MyAcraSender(ExtApplication application){
        this.application = application;
    }

    @Override
    public void send(CrashReportData report) throws ReportSenderException {
        SharedPreferences sharedPreferences = application.getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        String mongoId  = sharedPreferences.getString("mongoId", null);
        String username  = sharedPreferences.getString("username", null);
        String friends  = sharedPreferences.getString("friends", null);
        String friendRequests  = sharedPreferences.getString("friendRequests", null);
        int sharedRunsNum = sharedPreferences.getInt(User.SHARED_RUNS_NUM, 0);
        editor.clear().apply();
       editor.putString("mongoId", mongoId);
        editor.putString("username", username);
        editor.putString("friends", friends);
        editor.putString("friendRequests", friendRequests);
        editor.putInt(User.SHARED_RUNS_NUM, sharedRunsNum);

        editor.apply();
    }
}
