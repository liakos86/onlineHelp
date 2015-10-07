package com.kostas.onlineHelp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.kostas.service.RunningService;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

/**
 * Created by liakos on 21/9/2015.
 */

@ReportsCrashes(formKey = "",
        httpMethod = HttpSender.Method.POST,
        customReportContent = {ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT},
        mode = ReportingInteractionMode.TOAST,
        mailTo = "liakos86@gmail.com",

        resToastText = R.string.crash_toast_text)
public class MyAcraSender implements ReportSender {

   ExtApplication application;

    public MyAcraSender(ExtApplication application){
        this.application = application;
    }

    @Override
    public void send(CrashReportData report) throws ReportSenderException {
//        SharedPreferences sharedPreferences = application.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.remove(RunningService.IS_RUNNING);
//        editor.commit();
//
//        Toast.makeText(application, "Clearing...", Toast.LENGTH_SHORT).show();
    }
}
