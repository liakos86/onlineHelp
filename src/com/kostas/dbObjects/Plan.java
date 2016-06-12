package com.kostas.dbObjects;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;

/**
 * Created by liakos on 10/10/2015.
 */
public class Plan {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();


    private long id;
    private String description;
    private int meters;
    private int seconds;
    private int rounds;
    private int startRest;

    public Plan(){}

    public Plan(String description){
        this.description = description;
    }

    public Plan(long id, String description, int meters, int seconds, int rounds, int startRest){
        this.id = id;
        this.description = description;
        this.meters = meters;
        this.seconds = seconds;
        this.rounds = rounds;
        this.startRest = startRest;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMeters() {
        return meters;
    }

    public void setMeters(int meters) {
        this.meters = meters;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public int getStartRest() {
        return startRest;
    }

    public void setStartRest(int startRest) {
        this.startRest = startRest;
    }

    public static Plan getFromId(Context context, long id) {
        //Log.v(TAG, String.format("Requesting item [%d]", id));
        synchronized (context) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(Uri.withAppendedPath(ContentDescriptor.Plan.CONTENT_URI,
                                String.valueOf(id)), null, null, null, null);
                cursor.moveToFirst();
                return createFromCursor(cursor);
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public static ContentValues asContentValues(Plan item) {
        if (item == null)
            return null;
        synchronized (item) {
            ContentValues toRet = new ContentValues();

            toRet.put(ContentDescriptor.Plan.Cols.ID, item.id);
            toRet.put(ContentDescriptor.Plan.Cols.DESCRIPTION, item.description);
            toRet.put(ContentDescriptor.Plan.Cols.METERS, item.meters);
            toRet.put(ContentDescriptor.Plan.Cols.SECONDS, item.seconds);
            toRet.put(ContentDescriptor.Plan.Cols.ROUNDS, item.rounds);
            toRet.put(ContentDescriptor.Plan.Cols.START_REST, item.startRest);

            return toRet;
        }
    }

    public static Plan createFromCursor(Cursor cursor) {
        synchronized (cursor) {
            if (cursor.isClosed() || cursor.isAfterLast() || cursor.isBeforeFirst()) {
                //Log.v(TAG, String.format("Requesting entity but no valid cursor"));
                return null;
            }
            Plan toRet = new Plan();
            toRet.id = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.Plan.Cols.ID));
            toRet.description = cursor.getString(cursor.getColumnIndex(ContentDescriptor.Plan.Cols.DESCRIPTION));
            toRet.meters = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.Plan.Cols.METERS));
            toRet.seconds = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.Plan.Cols.SECONDS));
            toRet.rounds = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.Plan.Cols.ROUNDS));
            return toRet;
        }
    }

    /**
     * let the id decide if we have an insert or an update
     *
     * @param resolver
     * @param item
     */
    public static void save(ContentResolver resolver, Plan item) {
        if (item.id == Database.INVALID_ID)
            resolver.insert(ContentDescriptor.Plan.CONTENT_URI, Plan.asContentValues(item));
        else
            resolver.update(ContentDescriptor.Plan.CONTENT_URI, Plan.asContentValues(item),
                    String.format("%s=?", ContentDescriptor.Plan.Cols.ID),
                    new String[]{
                            String.valueOf(item.id)
                    });
    }



}
