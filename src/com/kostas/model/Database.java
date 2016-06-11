
package com.kostas.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Plan;
import com.kostas.dbObjects.Running;

import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "interval_runner.db";
    private static final int DATABASE_VERSION = 1;
    // this is also considered as invalid id by the server
    public static final long INVALID_ID = -1;
    private Context mContext;

    public Database(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(ContentDescriptor.Running.createTable());
        db.execSQL(ContentDescriptor.Interval.createTable());
        db.execSQL(ContentDescriptor.Plan.createTable());


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("Database", "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
            db.execSQL("drop table if exists " + ContentDescriptor.Running.TABLE_NAME);
        db.execSQL("drop table if exists " + ContentDescriptor.Interval.TABLE_NAME);
        db.execSQL("drop table if exists " + ContentDescriptor.Plan.TABLE_NAME);

            onCreate(db); // run onCreate to get new database
    }
    
    
    public int addRunning(Running running) {
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = resolver.insert(ContentDescriptor.Running.CONTENT_URI, Running.asContentValues(running));

        int runId = Integer.valueOf(uri.getLastPathSegment());

        for (Interval interval : running.getIntervals())
        {
            interval.setRunning_id(runId);
            addInterval(interval);
        }

        return runId;

    }


    public void deleteRunning(Long id){
        ContentResolver resolver = mContext.getContentResolver();
        resolver.delete(ContentDescriptor.Running.CONTENT_URI, ContentDescriptor.Running.Cols.ID + "=" + String.valueOf(id), null);
        resolver.delete(ContentDescriptor.Interval.CONTENT_URI,ContentDescriptor.Interval.Cols.RUNNING_ID + "=" + String.valueOf(id), null);

    }

    public void addInterval(Interval interval) {
        ContentResolver resolver = mContext.getContentResolver();
        resolver.insert(ContentDescriptor.Interval.CONTENT_URI, Interval.asContentValues(interval));
    }


    public void deleteInterval(Long id){
        ContentResolver resolver = mContext.getContentResolver();
        resolver.delete(ContentDescriptor.Interval.CONTENT_URI, ContentDescriptor.Interval.Cols.ID + "=" + String.valueOf(id), null);
    }

    public void addPlan(Plan plan) {
        ContentResolver resolver = mContext.getContentResolver();
        resolver.insert(ContentDescriptor.Plan.CONTENT_URI, Plan.asContentValues(plan));
    }

    public void deletePlan(Long id){
        ContentResolver resolver = mContext.getContentResolver();
        resolver.delete(ContentDescriptor.Plan.CONTENT_URI, ContentDescriptor.Plan.Cols.ID + "=" + String.valueOf(id), null);
    }


    public int countRuns(){
        String[] proj = {ContentDescriptor.Running.Cols.ID};
        Cursor c = mContext.getContentResolver().query(ContentDescriptor.Running.CONTENT_URI, proj, null, null, null);
        int toRet = c.getCount();
        c.close();
        c=null;
        return toRet;
    }

    public List<Running> fetchRunsFromDb() {

        String[] FROM = {
                // ! beware. I mark the position of the fields
                ContentDescriptor.Running.Cols.DESCRIPTION,
                ContentDescriptor.Running.Cols.DATE,
                ContentDescriptor.Running.Cols.ID,
                ContentDescriptor.Running.Cols.TIME,
                ContentDescriptor.Running.Cols.AVGPACETEXT,
                ContentDescriptor.Running.Cols.DISTANCE

        };
        int sDescPosition = 0;
        int sDatePosition = 1;
        int sIdPosition = 2;
        int sTimePosition = 3;
        int sPacePosition = 4;
        int sDistPosition = 5;


        Cursor c = mContext.getContentResolver().query(ContentDescriptor.Running.CONTENT_URI, FROM,
                null,
                null, null);

        List<Running> St = new ArrayList<Running>();

        if (c.getCount() > 0) {

            while (c.moveToNext()) {


                Running newRun = new Running(c.getLong(sIdPosition),
                        c.getString(sDescPosition),
                        c.getLong(sTimePosition),
                        c.getString(sDatePosition),
                        c.getFloat(sDistPosition)
                );

                newRun.setAvgPaceText(c.getString(sPacePosition));


                St.add(newRun);
            }
        }
        c.close();
        c = null;

        return St;

    }

    public List<Interval> fetchIntervalsForRun(long id) {

        String[] FROM = {
                // ! beware. I mark the position of the fields
                ContentDescriptor.Interval.Cols.ID,
                ContentDescriptor.Interval.Cols.LATLONLIST,
                ContentDescriptor.Interval.Cols.MILLISECONDS,
                ContentDescriptor.Interval.Cols.DISTANCE,
                ContentDescriptor.Interval.Cols.PACETEXT,
                ContentDescriptor.Interval.Cols.FASTEST
        };

        int sIdPosition = 0;
        int sLatPosition = 1;
        int sMillisPosition = 2;
        int sDistancePosition = 3;
        int sPacePosition = 4;
        int sFastestPosition = 5;

        Cursor c = mContext.getContentResolver().query(ContentDescriptor.Interval.CONTENT_URI, FROM,
                ContentDescriptor.Interval.Cols.RUNNING_ID+" = "+String.valueOf(id),
                null, null);

        List<Interval> St = new ArrayList<Interval>();

        if (c.getCount() > 0) {

            while (c.moveToNext()) {
                Interval interval = new Interval(c.getLong(sIdPosition),c.getString(sLatPosition),
                        c.getLong(sMillisPosition), c.getFloat(sDistancePosition));
                interval.setFastest(c.getInt(sFastestPosition)==1);
                interval.setPaceText(c.getString(sPacePosition));
                St.add(interval);
            }
        }
        c.close();
        c = null;

        return St;

    }

    public List<Plan> fetchPlansFromDb() {

        String[] FROM = {
                // ! beware. I mark the position of the fields
                ContentDescriptor.Plan.Cols.ID,
                ContentDescriptor.Plan.Cols.DESCRIPTION,
                ContentDescriptor.Plan.Cols.METERS,
                ContentDescriptor.Plan.Cols.SECONDS,
                ContentDescriptor.Plan.Cols.ROUNDS,
                ContentDescriptor.Plan.Cols.START_REST

        };

        int sIdPosition = 0;
        int sDescPosition = 1;
        int sMetersPosition = 2;
        int sSecondsPosition = 3;
        int sRoundsPosition = 4;
        int sStartRestPosition = 5;

        Cursor c = mContext.getContentResolver().query(ContentDescriptor.Plan.CONTENT_URI, FROM,
                null,
                null, null);

        List<Plan> St = new ArrayList<Plan>();

        if (c.getCount() > 0) {

            while (c.moveToNext()) {



                St.add(new Plan(c.getLong(sIdPosition),
                        c.getString(sDescPosition),
                        c.getInt(sMetersPosition),
                        c.getInt(sSecondsPosition),
                        c.getInt(sRoundsPosition),
                        c.getInt(sStartRestPosition)
                ));
            }
        }
        c.close();
        c = null;

        return St;

    }



}
