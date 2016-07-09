
package com.kostas.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Plan;
import com.kostas.dbObjects.Running;
import com.kostas.dbObjects.User;

import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {


    String[] USERS_FROM = {
            // ! beware. I mark the position of the fields
            ContentDescriptor.User.Cols.ID,
            ContentDescriptor.User.Cols.MONGO_ID,
            ContentDescriptor.User.Cols.USERNAME,
            ContentDescriptor.User.Cols.EMAIL,
            ContentDescriptor.User.Cols.FRIENDS,
            ContentDescriptor.User.Cols.FRIEND_REQUESTS,
            ContentDescriptor.User.Cols.SHARED_RUNS_NUM,
            ContentDescriptor.User.Cols.TOTAL_DISTANCE,
            ContentDescriptor.User.Cols.TOTAL_INTERVALS,
            ContentDescriptor.User.Cols.TOTAL_RUNS,
            ContentDescriptor.User.Cols.TOTAL_TIME


    };




    private static final String DATABASE_NAME = "interval_runner.db";
    private static final int DATABASE_VERSION = 2;
    // this is also considered as invalid id by the server
    public static final long INVALID_ID = -1;
    private Context mContext;

    private static final String DATABASE_ALTER_RUNNING_1 = "ALTER TABLE running ADD COLUMN IS_SHARED INTEGER default 0;";

    public Database(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(ContentDescriptor.Running.createTable());
        db.execSQL(ContentDescriptor.Interval.createTable());
        db.execSQL(ContentDescriptor.Plan.createTable());
        db.execSQL(ContentDescriptor.User.createTable());


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        if (oldVersion < 2) {
            db.execSQL(DATABASE_ALTER_RUNNING_1);
            db.execSQL(ContentDescriptor.User.createTable());
        }

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

    public void addUser(User user) {
        if (user.getFriends() == null){
            user.setFriends("");
        }
        if (user.getFriendRequests() == null){
            user.setFriendRequests("");
        }
        ContentResolver resolver = mContext.getContentResolver();
        resolver.insert(ContentDescriptor.User.CONTENT_URI, User.asContentValues(user));
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
                ContentDescriptor.Running.Cols.DISTANCE,
                ContentDescriptor.Running.Cols.IS_SHARED

        };
        int sDescPosition = 0;
        int sDatePosition = 1;
        int sIdPosition = 2;
        int sTimePosition = 3;
        int sPacePosition = 4;
        int sDistPosition = 5;
        int sIsSharedPosition = 6;


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
                        c.getFloat(sDistPosition),
                        c.getInt(sIsSharedPosition)==1
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

    public void setSharedFlagTrue(Long runningId){
        ContentResolver resolver = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ContentDescriptor.Running.Cols.IS_SHARED, 1);
        resolver.update(ContentDescriptor.Running.CONTENT_URI, values, ContentDescriptor.Running.Cols.ID + " = '"+runningId+"'", null);

    }

    public List<User> fetchUsersFromDb() {


        int sIdPosition = 0;
        int sMongoIdPosition = 1;
        int sUsernamePosition = 2;
        int sEmailPosition = 3;
        int sFriendsPosition = 4;
        int sFriendRequestsPosition = 5;
        int sSharedRunsNumPosition = 6;

        int sTotalDistancePosition = 7;
        int sTotalIntervalsPosition = 8;
        int sTotalRunsPosition = 9;
        int sTotalTimePosition = 10;


        Cursor c = mContext.getContentResolver().query(ContentDescriptor.User.CONTENT_URI, USERS_FROM,
                null,
                null, null);

        List<User> St = new ArrayList<User>();

        if (c.getCount() > 0) {

            while (c.moveToNext()) {


                User newUser = new User(c.getLong(sIdPosition),
                        c.getString(sMongoIdPosition),
                        c.getString(sUsernamePosition),

                        c.getString(sEmailPosition),
                        c.getString(sFriendsPosition),
                        c.getString(sFriendRequestsPosition),

                        c.getInt(sSharedRunsNumPosition),

                        c.getFloat(sTotalDistancePosition),
                        c.getInt(sTotalIntervalsPosition),
                        c.getInt(sTotalRunsPosition),
                        c.getLong(sTotalTimePosition)
                );



                St.add(newUser);
            }
        }
        c.close();
        c = null;

        return St;

    }

    public User fetchUser(String username) {


        int sIdPosition = 0;
        int sMongoIdPosition = 1;
        int sUsernamePosition = 2;
        int sEmailPosition = 3;
        int sFriendsPosition = 4;
        int sFriendRequestsPosition = 5;
        int sSharedRunsNumPosition = 6;

        int sTotalDistancePosition = 7;
        int sTotalIntervalsPosition = 8;
        int sTotalRunsPosition = 9;
        int sTotalTimePosition = 10;

        Cursor c = mContext.getContentResolver().query(ContentDescriptor.User.CONTENT_URI, USERS_FROM,
                ContentDescriptor.User.Cols.USERNAME + " = '" + username + "' ",
                null, null);

        User user = new User();

        if (c.getCount() > 0) {

            while (c.moveToNext()) {


                user = new User(c.getLong(sIdPosition),
                        c.getString(sMongoIdPosition),
                        c.getString(sUsernamePosition),

                        c.getString(sEmailPosition),
                        c.getString(sFriendsPosition),
                        c.getString(sFriendRequestsPosition),

                        c.getInt(sSharedRunsNumPosition),

                        c.getFloat(sTotalDistancePosition),
                        c.getInt(sTotalIntervalsPosition),
                        c.getInt(sTotalRunsPosition),
                        c.getLong(sTotalTimePosition)
                );
            }
            c.close();
            c = null;



        }
        return user;
    }


}
