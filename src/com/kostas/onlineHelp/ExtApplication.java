package com.kostas.onlineHelp;

import android.app.Application;
import android.content.Context;
//import com.parse.Parse;
//import com.parse.PushService;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
//import uk.co.senab.bitmapcache.BitmapLruCache;


@ReportsCrashes(formKey = "",
        httpMethod = HttpSender.Method.POST,
        customReportContent = {ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT},
        mode = ReportingInteractionMode.TOAST,
        mailTo = "liakos86@gmail.com",

        resToastText = R.string.crash_toast_text)
public class ExtApplication extends Application {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    private int position;
    private boolean inRunningMode;


    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
        MyAcraSender mySender = new MyAcraSender(this);
        ACRA.getErrorReporter().addReportSender(mySender);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isInRunningMode() {
        return inRunningMode;
    }

    public void setInRunningMode(boolean inRunningMode) {
        this.inRunningMode = inRunningMode;
    }
}