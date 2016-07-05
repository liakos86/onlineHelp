package com.kostas.dbObjects;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;

import java.util.List;


/**
 * Created by liakos on 11/4/2015.
 */
public class Running {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    private ObjectId _id;
    private long running_id;
    private String date;
    private float distance;
    private long time;
    private String description;
    private String avgPaceText;
    private List<Interval> intervals;
    private boolean isShared;

    public Running(){}


    public Running(long running_id, String description, long time, String date, float distance, List<Interval>intervals){
        this.running_id = running_id;
        this.time = time;
        this.date = date;
        this.description = description;
        this.distance = distance;
        this.intervals = intervals;
    }


    public Running(long running_id, String description, long time, String date, float distance){
        this.running_id = running_id;
        this.time = time;
        this.date = date;
        this.description = description;
        this.distance = distance;
    }

    public ObjectId get_id() {
        return _id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getRunning_id() {
        return running_id;
    }

    public void setRunning_id(long running_id) {
        this.running_id = running_id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public String getAvgPaceText() {
        return avgPaceText;
    }

    public void setAvgPaceText(String avgPaceText) {
        this.avgPaceText = avgPaceText;
    }

    public List<Interval> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<Interval> intervals) {
        this.intervals = intervals;
    }

    public static Running getFromId(Context context, long id) {
        //Log.v(TAG, String.format("Requesting item [%d]", id));
        synchronized (context) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(Uri.withAppendedPath(ContentDescriptor.Running.CONTENT_URI,
                                String.valueOf(id)), null, null, null, null);
                cursor.moveToFirst();
                return createFromCursor(cursor);
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public static ContentValues asContentValues(Running item) {
        if (item == null)
            return null;
        synchronized (item) {
            ContentValues toRet = new ContentValues();

            toRet.put(ContentDescriptor.Running.Cols.ID, item.running_id);
            toRet.put(ContentDescriptor.Running.Cols.DATE, item.date);
            toRet.put(ContentDescriptor.Running.Cols.DESCRIPTION, item.description);
            toRet.put(ContentDescriptor.Running.Cols.TIME, item.time);
            toRet.put(ContentDescriptor.Running.Cols.AVGPACETEXT, item.avgPaceText);
            toRet.put(ContentDescriptor.Running.Cols.DISTANCE, item.distance);

            return toRet;
        }
    }

    public static Running createFromCursor(Cursor cursor) {
        synchronized (cursor) {
            if (cursor.isClosed() || cursor.isAfterLast() || cursor.isBeforeFirst()) {
                //Log.v(TAG, String.format("Requesting entity but no valid cursor"));
                return null;
            }
            Running toRet = new Running();
            toRet.running_id = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.Running.Cols.ID));
            toRet.date = cursor.getString(cursor.getColumnIndex(ContentDescriptor.Running.Cols.DATE));
            toRet.description = cursor.getString(cursor.getColumnIndex(ContentDescriptor.Running.Cols.DESCRIPTION));
            toRet.time = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.Running.Cols.TIME));
            toRet.avgPaceText = cursor.getString(cursor.getColumnIndex(ContentDescriptor.Running.Cols.AVGPACETEXT));
            toRet.distance = cursor.getFloat(cursor.getColumnIndex(ContentDescriptor.Running.Cols.DISTANCE));

            return toRet;
        }
    }

    /**
     * let the id decide if we have an insert or an update
     *
     * @param resolver
     * @param item
     */
    public static void save(ContentResolver resolver, Running item) {
        if (item.running_id == Database.INVALID_ID)
            resolver.insert(ContentDescriptor.Running.CONTENT_URI, Running.asContentValues(item));
        else
            resolver.update(ContentDescriptor.Running.CONTENT_URI, Running.asContentValues(item),
                    String.format("%s=?", ContentDescriptor.Running.Cols.ID),
                    new String[]{
                            String.valueOf(item.running_id)
                    });
    }




}
