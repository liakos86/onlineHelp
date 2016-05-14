package com.kostas.onlineHelp;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import com.kostas.custom.MyAcraSender;
import com.kostas.service.TTSManager;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    /*
 * isOnline - Check if there is a NetworkConnection
 * @return boolean
 */
    public boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();

        if (is3g){
            Toast.makeText(this, manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getSubtypeName(),Toast.LENGTH_SHORT).show();
        }

        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();

        return isWifi || is3g ;
    }

    /**
     * For testing purposes returns an md5 hash of the device to add testing ads
     * @param s
     * @return
     */
    public static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            //Log.v("SERVICE",e.getMessage());
        }
        return "";
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