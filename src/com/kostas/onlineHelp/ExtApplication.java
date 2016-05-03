package com.kostas.onlineHelp;

import android.app.Application;
import com.kostas.service.TTSManager;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

@ReportsCrashes(formKey = "",
        httpMethod = HttpSender.Method.POST,
        customReportContent = {ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT},
        mode = ReportingInteractionMode.TOAST,
        mailTo = "liakos86@gmail.com",
        resToastText = R.string.crash_toast_text)
public class ExtApplication extends Application {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();
    /**
     * Is an interval in progress
     */
    private boolean inRunningMode;
    /**
     * Is the running activity displayed
     */
    private boolean inRunningAct;
    /**
     * Is there a new interval in db? I need to fetch again then
     */
    private boolean newIntervalInDb;
    /**
     * The text to speech manager, to vocalize the run info
     */
    private TTSManager ttsManager;

    /**
     * The position of the pager
     */
    private int position;


    @Override
    public void onCreate() {
        super.onCreate();
        FontsOverride.setDefaultFont(this, "MONOSPACE",  "fonts/OpenSans-Semibold.ttf");

        ACRA.init(this);
        MyAcraSender mySender = new MyAcraSender(this);
        ACRA.getErrorReporter().addReportSender(mySender);
        ttsManager = new TTSManager();
        ttsManager.init(this);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isNewIntervalInDb() {
        return newIntervalInDb;
    }

    public void setNewIntervalInDb(boolean newIntervalInDb) {
        this.newIntervalInDb = newIntervalInDb;
    }

    public boolean isInRunningMode() {
        return inRunningMode;
    }

    public void setInRunningMode(boolean inRunningMode) {
        this.inRunningMode = inRunningMode;
    }

    public boolean isInRunningAct() {
        return inRunningAct;
    }

    public void setInRunningAct(boolean inRunningAct) {
        this.inRunningAct = inRunningAct;
    }

    public TTSManager getTtsManager() {
        return ttsManager;
    }
}