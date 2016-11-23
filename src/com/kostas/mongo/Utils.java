
package com.kostas.mongo;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {
    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    public static final int DLG_RESULT_OK = 1;
    public static final int DLG_REQUEST_SELECT_PART = 1;
    public static final int DLG_REQUEST_SELECT_USER = 2;
    public static final int DLG_REQUEST_SELECT_EQUIPMENT = 4;
    public static final int DLG_REQUEST_SELECT_USER2 = 12;
    public static final int DLG_REQUEST_SELECT_USER3 = 13;
    public static final int DLG_REQUEST_SELECT_SITE = 14;

    // used on countdowns for autodeletion
    public static final int AUTODELETE_AFTER_SECS = 3;

    // key for pair lists
    public static final String UNDO_ACTION = "action";

    // lazy init
    static Boolean sHasCamera = null;

    public static String padzero(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }

    public static String currentTime() {
        String currenttime;
        Calendar c = Calendar.getInstance();
        int mStart, hStart;
        hStart = c.get(Calendar.HOUR_OF_DAY);
        mStart = c.get(Calendar.MINUTE);
        currenttime = padzero(hStart) + ":" + padzero(mStart);
        return currenttime;
    }

    public static String currentDate() {
        String currentdate;

        Calendar c = Calendar.getInstance();

        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);

        currentdate = mDay + "/" + (mMonth + 1) + "/" + mYear;

        return currentdate;
    }

    public synchronized static String convertStreamToString(InputStream is) {
        //Log.v(TAG, "stream2string start");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();

        try {
            final char[] chars = new char[4 * 1024];
            int len;
            while ((len = reader.read(chars)) >= 0) {
                //Log.v(TAG, "tic");
                sb.append(chars, 0, len);
            }
        } catch (IOException e) {
            //Log.w(TAG, "could not convert stream to string", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                //Log.i(TAG, "erro closing stream", e);
            }
        }
        //Log.v(TAG, "stream2string done");
        return sb.toString();
    }

    public synchronized static String convertNullToSpace(String is) {

        if (is == null)
            is = AppConstants.EMPTY;

        return is;
    }

    public static synchronized String[] append(String[] arr, String element) {
        final List<String> inter = new LinkedList<String>(Arrays.asList(arr));
        inter.add(element);
        final String[] toRet = new String[inter.size()];
        inter.toArray(toRet);
        return toRet;
    }

    public static synchronized String[] append(String[] arr, String[] elements) {
        final List<String> inter = new LinkedList<String>(Arrays.asList(arr));
        final List<String> inter2 = new LinkedList<String>(Arrays.asList(elements));
        inter.addAll(inter2);
        final String[] toRet = new String[inter.size()];
        inter.toArray(toRet);
        return toRet;
    }

    public static synchronized String substringSafe(String in, int start, int end) {
        String toRet = in;
        if (start > in.length())
            start = in.length() - 1;
        if (start < 0)
            start = 0;

        if (end > in.length())
            end = in.length();

        if (end < start || end <= 0)
            toRet = toRet.substring(start);
        else
            toRet.substring(start, end);

        return toRet;
    }

    public static synchronized long diggForLong(String name, long defaultValue, Intent intent,
            Bundle... args) {
        long toRet = Long.MIN_VALUE;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null)
                    continue;
                toRet = args[i].getLong(name, Integer.MIN_VALUE);
                if (toRet != Integer.MIN_VALUE)
                    return toRet;
            }
        }
        if (intent != null)
            toRet = intent.getLongExtra(name, Long.MIN_VALUE);
        if (toRet != Long.MIN_VALUE)
            return toRet;
        return defaultValue;
    }

    public static synchronized String diggForString(String name, String defaultValue,
            Intent intent, Bundle... args) {
        String toRet = defaultValue;

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null)
                    continue;
                if (args[i].containsKey(name)) {
                    toRet = args[i].getString(name);
                    return toRet;
                }
            }
        }

        if (intent != null && intent.hasExtra(name))
            toRet = intent.getStringExtra(name);

        return toRet;
    }

    public static synchronized int diggForInt(String name, int defaultValue,
            Intent intent, Bundle... args) {
        int toRet = defaultValue;

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null)
                    continue;
                if (args[i].containsKey(name)) {
                    toRet = args[i].getInt(name);
                    return toRet;
                }
            }
        }

        if (intent != null && intent.hasExtra(name))
            toRet = intent.getIntExtra(name, defaultValue);

        return toRet;
    }

    public static String implodeConditionally(boolean discardEmptyEntries, String glue,
            String... strings) {
        String toRet = AppConstants.EMPTY;
        // in case the last one is empty and we discard empty entries
        int actualLength = strings.length;
        if (discardEmptyEntries) {
            if (discardEmptyEntries) {
                while ((actualLength > 0) && TextUtils.isEmpty(strings[actualLength - 1]))
                    actualLength--;
            }
            for (int i = 0; i < actualLength - 1; i++) {
                if (discardEmptyEntries && TextUtils.isEmpty(strings[i]))
                    continue;
                toRet = toRet + strings[i] + glue;
            }
            if (actualLength > 0)
                toRet = toRet + strings[actualLength - 1];
        }
        return toRet;
    }

    public static String implode(String glue, String... strings) {
        return implodeConditionally(true, glue, strings);
    }

    /**
     * convertTimestampString with defString=""
     * 
     * @param str
     * @param input
     * @param output
     * @return
     */
    public static final String convertTimestampString(final String str,
            final SimpleDateFormat input, final SimpleDateFormat output) {
        return convertTimestampString(str, input, output, AppConstants.EMPTY);
    }

    /**
     * convert a timestamp string formatted according to input to another one
     * formatted according to output. In case of an erro, defString is returned
     * 
     * @param str
     * @param input
     * @param output
     * @param defString
     * @return
     */
    public static final String convertTimestampString(final String str,
            final SimpleDateFormat input, final SimpleDateFormat output, final String defString) {
        try {
            return output.format(input.parse(str));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return defString;
    }

    /**
     * when in a inner method we have a catch-22: we cannot access a var unless
     * it is final but if it is final we cannot assign values to it, ie <br/>
     * <code>
     * int ext_int;<br/>
     * final int ext_int2;<br/>
     *  //... and in inner class method<br/>
     * ext_int = 2; // error: ext_int is not final<br/>
     * ext_int2 = 2; // error: ext_int2 is final<br/>
     * </code>
     * <br/>
     * is forbidden. So we create a final instance of this and assign values to
     * its member, ie <br />
     * <code>
     * final ext_sel_int = new SelectInteger(0);<br/>
     * // ...<br/>
     * ext_sel_int.selection = 12;<br/></code> <br/>
     * 
     */
    static public class DlgSelectionString {
        public String selection;

        public DlgSelectionString() {
            this(AppConstants.EMPTY);
        }

        public DlgSelectionString(String selection) {
            this.selection = selection;
        }
    }

    /**
     * Dialog for ime edit.<br />
     * Usage:<br/>
     * 
     * <pre>
     * // textview is the view that will receive the results
     * // calendar contains the initial time
     * 
     * Bundle args = new Bundle();
     * args.putInt(Utils.TimePickerFragment.ARG_HOUR, calendar.get(Calendar.HOUR));
     * args.putInt(Utils.TimePickerFragment.ARG_MINUTE, calendar.get(Calendar.MINUTE));
     * DialogFragment fragment = new Utils.TimePickerFragment().setTargetView(textview).setDateFormat(
     *         mFormat);
     * fragment.setArguments(args);
     * fragment.show(mActivity.getSupportFragmentManager(), &quot;timePicker&quot;);
     * 
     * </pre>
     * 
     * 
     */

    static public class TimePickerFragment extends DialogFragment implements
            TimePickerDialog.OnTimeSetListener {
        public final static String ARG_HOUR = "hour";
        public final static String ARG_MINUTE = "minute";

        TextView mView;
        SimpleDateFormat mFormat;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int hour, minute;

            if (getArguments().containsKey(ARG_HOUR)) {
                hour = getArguments().getInt(ARG_HOUR);
                minute = getArguments().getInt(ARG_MINUTE);
                //Log.v(TAG, String.format("timepicker received [%d]:[%d]", hour, minute));
            } else {
                // Use the current time as the default values for the picker
                final Calendar c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
                //Log.v(TAG, String.format("timepicker default to [%d]:[%d]", hour, minute));
            }

            // HARDCODED 24-hours
            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute, true);
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            mView.setText(mFormat.format(calendar.getTime()));
        }

        public TimePickerFragment setTargetView(TextView view) {
            mView = view;
            return this;
        }

        public TimePickerFragment setDateFormat(SimpleDateFormat format) {
            mFormat = format;
            return this;
        }

    }

    /**
     * Dialog for date edit.<br />
     * Usage:<br/>
     * 
     * <pre>
     * // textview is the view that will receive the results
     * // calendar contains the initial date
     * 
     * Bundle args = new Bundle();
     * args.putInt(Utils.DatePickerFragment.ARG_DAY, calendar.get(Calendar.DAY_OF_MONTH));
     * args.putInt(Utils.DatePickerFragment.ARG_MONTH, calendar.get(Calendar.MONTH) + 1);
     * args.putInt(Utils.DatePickerFragment.ARG_YEAR, calendar.get(Calendar.YEAR));
     * DialogFragment fragment = new Utils.DatePickerFragment().setTargetView(tv).setDateFormat(
     *         mFormat);
     * fragment.setArguments(args);
     * fragment.show(mActivity.getSupportFragmentManager(), &quot;datePicker&quot;);
     * 
     * </pre>
     * 
     */

    static public class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {
        public final static String ARG_DAY = "day";
        public final static String ARG_MONTH = "month";
        public final static String ARG_YEAR = "year";

        TextView mView;
        SimpleDateFormat mFormat;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int day, month, year;

            if (getArguments().containsKey(ARG_YEAR)) {
                day = getArguments().getInt(ARG_DAY);
                month = getArguments().getInt(ARG_MONTH);
                year = getArguments().getInt(ARG_YEAR);
            } else {
                // Use the current time as the default values for the picker
                final Calendar c = Calendar.getInstance();
                day = c.get(Calendar.DAY_OF_MONTH);
                month = c.get(Calendar.MONTH);
                year = c.get(Calendar.YEAR);
            }

            // Create a new instance of TimePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mView.setText(mFormat.format(calendar.getTime()));
        }

        public DatePickerFragment setTargetView(TextView view) {
            mView = view;
            return this;
        }

        public DatePickerFragment setDateFormat(SimpleDateFormat format) {
            mFormat = format;
            return this;
        }
    }

    /**
     * onClickListener that automates time editing for a view. Usage:
     * 
     * <pre>
     * 
     * public static final SimpleDateFormat FORMAT_TIME = new SimpleDateFormat(&quot;HH:mm&quot;, Locale.US);
     * 
     * mTextTime.setOnClickListener(new Utils.TimeOnClickListener(getActivity(),
     *         ActActivity.FORMAT_TIME));
     * 
     * </pre>
     * 
     */
    public static class TimeOnClickListener implements View.OnClickListener {
        FragmentActivity mActivity;
        SimpleDateFormat mFormat;

        public TimeOnClickListener(FragmentActivity activity, SimpleDateFormat format) {
            mActivity = activity;
            mFormat = format;
        }

        @Override
        public void onClick(View v) {
            TextView tv = (TextView) v;
            Log.v(TAG, String.format("Parsing time [%s]", tv.getText().toString()));
            Calendar calendar = Calendar.getInstance();
            try {
                Date date = mFormat.parse(tv.getText().toString());
                Log.v(TAG, String.format("Parsed time [%s]", date));
                calendar.setTime(date);
                Log.v(TAG, String.format("Parsed time [%s]", calendar));
            } catch (Exception e) {
                Log.v(TAG, String.format("Error parsing time [%s]", tv.getText()), e);
                // let it be
            }
            Bundle args = new Bundle();
            // HARDCODED 24hours time
            args.putInt(TimePickerFragment.ARG_HOUR, calendar.get(Calendar.HOUR_OF_DAY));
            args.putInt(TimePickerFragment.ARG_MINUTE, calendar.get(Calendar.MINUTE));
            DialogFragment fragment = new TimePickerFragment().setTargetView(tv)
                    .setDateFormat(mFormat);
            fragment.setArguments(args);
            fragment.show(mActivity.getSupportFragmentManager(), "timePicker");
        }
    }

    /**
     * onClickListener that automates date editing for a view. <br />
     * Usage:
     * 
     * <pre>
     * 
     * public static final SimpleDateFormat FORMAT_DATE = new SimpleDateFormat(&quot;dd/MM/yy&quot;, Locale.US);
     * public static final SimpleDateFormat FORMAT_TIME = new SimpleDateFormat(&quot;HH:mm&quot;, Locale.US);
     * 
     * mTextDate.setOnClickListener(new Utils.DateOnClickListener(getActivity(),
     *         ActActivity.FORMAT_DATE));
     * 
     * </pre
     * 
     */
    public static class DateOnClickListener implements View.OnClickListener {
        FragmentActivity mActivity;
        SimpleDateFormat mFormat;

        public DateOnClickListener(FragmentActivity activity, SimpleDateFormat format) {
            mActivity = activity;
            mFormat = format;
        }

        @Override
        public void onClick(View v) {
            TextView tv = (TextView) v;
            Log.v(TAG, String.format("Parsing date [%s]", tv.getText()));
            Calendar calendar = Calendar.getInstance();
            try {
                Date date = mFormat.parse(tv.getText().toString());
                calendar.setTime(date);
            } catch (Exception e) {
                Log.v(TAG, String.format("Error parsing date [%s]", tv.getText()), e);
                // let it be
            }
            Bundle args = new Bundle();
            args.putInt(DatePickerFragment.ARG_DAY, calendar.get(Calendar.DAY_OF_MONTH));
            args.putInt(DatePickerFragment.ARG_MONTH, calendar.get(Calendar.MONTH));
            args.putInt(DatePickerFragment.ARG_YEAR, calendar.get(Calendar.YEAR));
            DialogFragment fragment = new DatePickerFragment().setTargetView(tv)
                    .setDateFormat(mFormat);
            fragment.setArguments(args);
            fragment.show(mActivity.getSupportFragmentManager(), "datePicker");
        }
    }

    public static HashMap<Long, String> cursorToMap(Cursor cursor, boolean closeCursor) {
        Log.v(TAG, String.format("cursor has [%d] records", cursor.getCount()));
        HashMap<Long, String> toRet = new HashMap<Long, String>();
        if (cursor.moveToFirst()) {
            do {
                Log.v(TAG, String.format("putting [%s] = [%s]", cursor.getLong(0),
                        cursor.getString(1)));
                toRet.put(cursor.getLong(0), cursor.getString(1));
            } while (cursor.moveToNext());
        }
        Log.v(TAG, String.format("map has [%d] entries", toRet.size()));
        if (closeCursor) {
            cursor.close();
        }
        return toRet;
    }

    public static HashMap<String, String> cursorToMapString(Cursor cursor, boolean closeCursor) {
        Log.v(TAG, String.format("cursor has [%d] records", cursor.getCount()));
        HashMap<String, String> toRet = new HashMap<String, String>();
        if (cursor.moveToFirst()) {
            do {
                Log.v(TAG, String.format("putting [%s] = [%s]", cursor.getString(0),
                        cursor.getString(1)));
                toRet.put(cursor.getString(0), cursor.getString(1));
            } while (cursor.moveToNext());
        }
        Log.v(TAG, String.format("map has [%d] entries", toRet.size()));
        if (closeCursor) {
            cursor.close();
        }
        return toRet;
    }

    public static HashMap<String, Long> cursorToMapReverse(Cursor cursor, boolean closeCursor) {
        Log.v(TAG, String.format("cursor has [%d] records", cursor.getCount()));
        HashMap<String, Long> toRet = new HashMap<String, Long>();
        if (cursor.moveToFirst()) {
            do {
                if (!cursor.getString(0).equals(AppConstants.EMPTY)) {
                    Log.v(TAG, String.format("putting [%s] = [%s]", cursor.getString(0),
                            cursor.getString(1)));
                    toRet.put(cursor.getString(0), cursor.getLong(1));
                }
            } while (cursor.moveToNext());
        }
        Log.v(TAG, String.format("map has [%d] entries", toRet.size()));
        if (closeCursor) {
            cursor.close();
        }
        return toRet;
    }

    public static boolean stringValidator(String s) {
        if (null != s && !s.matches(AppConstants.EMPTY) && !s.matches("null"))
            return true;
        else
            return false;
    }

    public static boolean equalsIgnoreNull(String s1, String s2) {
        if (TextUtils.isEmpty(s1) && TextUtils.isEmpty(s2))
            return true;

        return TextUtils.equals(s1, s2);
    }

    public static boolean hasCamera(Context context) {
        if (sHasCamera == null) {
            sHasCamera = Boolean.valueOf(context.getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA));
        }
        return sHasCamera.booleanValue();
    }

    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    public static enum ScalingLogic {
        CROP, FIT
    }

    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;

            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int) (srcHeight * dstAspect);
                final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            } else {
                final int srcRectHeight = (int) (srcWidth / dstAspect);
                final int scrRectTop = (int) (srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        } else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }

    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;

            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int) (dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int) (dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }

    public static int[] convertIntegers(List<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }
}
