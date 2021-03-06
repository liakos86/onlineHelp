package com.kostas.custom;

import android.content.Context;
import android.content.SharedPreferences;
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
        sharedPreferences.edit().clear().apply();
    }
}
