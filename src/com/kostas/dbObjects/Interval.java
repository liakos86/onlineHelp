package com.kostas.dbObjects;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
//import android.util.Log;
import android.os.Parcel;
import android.os.Parcelable;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liakos on 19/9/2015.
 */
public class Interval{

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    private long interval_id;
    private long running_id;
    private String latLonList;
    private long milliseconds;
    private float distance;
    private boolean isFastest;
    private String paceText;

    public Interval(){}

    public Interval (long interval_id, String latLonList, long milliseconds, float distance){
        this.interval_id = interval_id;
        this.milliseconds = milliseconds;
        this.latLonList = latLonList;
        this.distance = distance;
    }

    public Interval (long interval_id, List<Location> locationList, long milliseconds, float distance){
        this.interval_id = interval_id;
        this.milliseconds = milliseconds;
        this.distance = distance;

        StringBuilder sb = new StringBuilder(locationList.get(0).getLatitude()+","+locationList.get(0).getLongitude());
        locationList.remove(0);
        for (Location l : locationList){
            sb.append("," +l.getLatitude()+","+l.getLongitude()+","+l.getLatitude()+","+l.getLongitude());
        }

        this.latLonList = sb.toString();

    }


    public boolean isFastest() {
        return isFastest;
    }

    public void setFastest(boolean isFastest) {
        this.isFastest = isFastest;
    }

    public long getRunning_id() {
        return running_id;
    }

    public void setRunning_id(long running_id) {
        this.running_id = running_id;
    }

    public long getInterval_id() {
        return interval_id;
    }

    public void setInterval_id(long interval_id) {
        this.interval_id = interval_id;
    }

    public String getLatLonList() {
        return latLonList;
    }

    public void setLatLonList(String latLonList) {
        this.latLonList = latLonList;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public String getPaceText() {
        return paceText;
    }

    public void setPaceText(String paceText) {
        this.paceText = paceText;
    }

    public static Interval getFromId(Context context, long id) {
        //Log.v(TAG, String.format("Requesting item [%d]", id));
        synchronized (context) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(Uri.withAppendedPath(ContentDescriptor.Interval.CONTENT_URI,
                                String.valueOf(id)), null, null, null, null);
                cursor.moveToFirst();
                return createFromCursor(cursor);
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public static ContentValues asContentValues(Interval item) {
        if (item == null)
            return null;
        synchronized (item) {
            ContentValues toRet = new ContentValues();

            toRet.put(ContentDescriptor.Interval.Cols.ID, item.interval_id);
            toRet.put(ContentDescriptor.Interval.Cols.RUNNING_ID, item.running_id);
            toRet.put(ContentDescriptor.Interval.Cols.MILLISECONDS, item.milliseconds);
            toRet.put(ContentDescriptor.Interval.Cols.LATLONLIST, item.latLonList);
            toRet.put(ContentDescriptor.Interval.Cols.DISTANCE, item.distance);
            toRet.put(ContentDescriptor.Interval.Cols.PACETEXT, item.paceText);
            toRet.put(ContentDescriptor.Interval.Cols.FASTEST, item.isFastest ? 1 : 0);

            return toRet;
        }
    }

    public static Interval createFromCursor(Cursor cursor) {
        synchronized (cursor) {
            if (cursor.isClosed() || cursor.isAfterLast() || cursor.isBeforeFirst()) {
                //Log.v(TAG, String.format("Requesting entity but no valid cursor"));
                return null;
            }
            Interval toRet = new Interval();
            toRet.interval_id = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.Interval.Cols.ID));
            toRet.running_id = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.Interval.Cols.RUNNING_ID));
            toRet.latLonList = cursor.getString(cursor.getColumnIndex(ContentDescriptor.Interval.Cols.LATLONLIST));
            toRet.milliseconds = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.Interval.Cols.MILLISECONDS));
            toRet.distance = cursor.getFloat(cursor.getColumnIndex(ContentDescriptor.Interval.Cols.DISTANCE));
            toRet.paceText = cursor.getString(cursor.getColumnIndex(ContentDescriptor.Interval.Cols.PACETEXT));
            toRet.isFastest = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.Interval.Cols.FASTEST))==1;
            return toRet;
        }
    }

    /**
     * let the id decide if we have an insert or an update
     *
     * @param resolver
     * @param item
     */
    public static void save(ContentResolver resolver, Interval item) {
        if (item.interval_id == Database.INVALID_ID)
            resolver.insert(ContentDescriptor.Interval.CONTENT_URI, Interval.asContentValues(item));
        else
            resolver.update(ContentDescriptor.Interval.CONTENT_URI, Interval.asContentValues(item),
                    String.format("%s=?", ContentDescriptor.Interval.Cols.ID),
                    new String[]{
                            String.valueOf(item.interval_id)
                    });
    }

}
