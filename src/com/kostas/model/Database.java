
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
import com.kostas.onlineHelp.ExtApplication;

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
    private static final int DATABASE_VERSION = 3;
    // this is also considered as invalid id by the server
    public static final long INVALID_ID = -1;
    private Context mApp;

    private static final String DATABASE_ALTER_RUNNING_1 = "ALTER TABLE running ADD COLUMN IS_SHARED INTEGER default 0;";
    private static final String DATABASE_ALTER_RUNNING_2 = "ALTER TABLE running ADD COLUMN username TEXT;";


    public Database(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        mApp = ctx;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(ContentDescriptor.Running.createTable());
        db.execSQL(ContentDescriptor.Interval.createTable());
        db.execSQL(ContentDescriptor.Plan.createTable());
        db.execSQL(ContentDescriptor.User.createTable());
        db.execSQL(ContentDescriptor.RunningFriend.createTable());
        db.execSQL(ContentDescriptor.IntervalFriend.createTable());


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        if (oldVersion < 2) {
            db.execSQL(DATABASE_ALTER_RUNNING_1);
            db.execSQL(DATABASE_ALTER_RUNNING_2);
            db.execSQL(ContentDescriptor.User.createTable());
            db.execSQL(ContentDescriptor.RunningFriend.createTable());
            db.execSQL(ContentDescriptor.IntervalFriend.createTable());
        }

    }
    
    
    public int addRunning(Running running, Uri runUri, Uri intervalUri) {
        ContentResolver resolver = mApp.getContentResolver();
        Uri uri = resolver.insert(runUri, Running.asContentValues(running));

        int runId = Integer.valueOf(uri.getLastPathSegment());

        for (Interval interval : running.getIntervals())
        {
            interval.setInterval_id(-1);
            interval.setRunning_id(runId);
            addInterval(interval, intervalUri);
        }

        return runId;

    }


    public void deleteRunning(Long id){
        ContentResolver resolver = mApp.getContentResolver();
        resolver.delete(ContentDescriptor.Running.CONTENT_URI, ContentDescriptor.RunningCols.ID + "=" + String.valueOf(id), null);
        resolver.delete(ContentDescriptor.Interval.CONTENT_URI,ContentDescriptor.IntervalCols.RUNNING_ID + "=" + String.valueOf(id), null);

    }

    public void addInterval(Interval interval, Uri intervalUri) {
        ContentResolver resolver = mApp.getContentResolver();
        resolver.insert(intervalUri, Interval.asContentValues(interval));
    }

    public void addPlan(Plan plan) {
        ContentResolver resolver = mApp.getContentResolver();
        resolver.insert(ContentDescriptor.Plan.CONTENT_URI, Plan.asContentValues(plan));
    }

    public void deletePlan(Long id){
        ContentResolver resolver = mApp.getContentResolver();
        resolver.delete(ContentDescriptor.Plan.CONTENT_URI, ContentDescriptor.Plan.Cols.ID + "=" + String.valueOf(id), null);
    }

    public void addUser(User user) {
        if (user.getFriends() == null){
            user.setFriends("");
        }
        if (user.getFriendRequests() == null){
            user.setFriendRequests("");
        }
        ContentResolver resolver = mApp.getContentResolver();
        resolver.insert(ContentDescriptor.User.CONTENT_URI, User.asContentValues(user));
    }


    public int countRuns(){
        String[] proj = {ContentDescriptor.RunningCols.ID};
        Cursor c = mApp.getContentResolver().query(ContentDescriptor.Running.CONTENT_URI, proj, null, null, null);
        int toRet = c.getCount();
        c.close();
        c=null;
        return toRet;
    }

    public List<Running> fetchRunsFromDb(Uri runUri, Uri intervalUri) {

       
        String[] FROM = {
                // ! beware. I mark the position of the fields
                ContentDescriptor.RunningCols.DESCRIPTION,
                ContentDescriptor.RunningCols.DATE,
                ContentDescriptor.RunningCols.ID,
                ContentDescriptor.RunningCols.TIME,
                ContentDescriptor.RunningCols.AVGPACETEXT,
                ContentDescriptor.RunningCols.DISTANCE,
                ContentDescriptor.RunningCols.IS_SHARED,
                ContentDescriptor.RunningCols.USERNAME

        };
        int sDescPosition = 0;
        int sDatePosition = 1;
        int sIdPosition = 2;
        int sTimePosition = 3;
        int sPacePosition = 4;
        int sDistPosition = 5;
        int sIsSharedPosition = 6;
        int sUsernamePosition = 7;



        Cursor c = mApp.getContentResolver().query(runUri, FROM,
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
                        c.getInt(sIsSharedPosition)==1,
                        c.getString(sUsernamePosition)
                );

                newRun.setAvgPaceText(c.getString(sPacePosition));


                St.add(newRun);
            }
        }
        c.close();
        c = null;



        for (Running run : St){
            run.setIntervals(fetchIntervalsForRun(run.getRunning_id(), intervalUri));
        }

        return St;

    }

    public List<Interval> fetchIntervalsForRun(long id, Uri tableUri) {

        String[] FROM = {
                // ! beware. I mark the position of the fields
                ContentDescriptor.IntervalCols.ID,
                ContentDescriptor.IntervalCols.LATLONLIST,
                ContentDescriptor.IntervalCols.MILLISECONDS,
                ContentDescriptor.IntervalCols.DISTANCE,
                ContentDescriptor.IntervalCols.PACETEXT,
                ContentDescriptor.IntervalCols.FASTEST
        };

        int sIdPosition = 0;
        int sLatPosition = 1;
        int sMillisPosition = 2;
        int sDistancePosition = 3;
        int sPacePosition = 4;
        int sFastestPosition = 5;

        Cursor c = mApp.getContentResolver().query(tableUri, FROM,
                ContentDescriptor.IntervalCols.RUNNING_ID+" = "+String.valueOf(id),
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

        Cursor c = mApp.getContentResolver().query(ContentDescriptor.Plan.CONTENT_URI, FROM,
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
        ContentResolver resolver = mApp.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ContentDescriptor.RunningCols.IS_SHARED, 1);
        resolver.update(ContentDescriptor.Running.CONTENT_URI, values, ContentDescriptor.RunningCols.ID + " = '"+runningId+"'", null);

    }

    public void updateUserRuns(String mongoId, int newruns){
        ContentResolver resolver = mApp.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ContentDescriptor.User.Cols.SHARED_RUNS_NUM, newruns);
        resolver.update(ContentDescriptor.User.CONTENT_URI, values, ContentDescriptor.User.Cols.MONGO_ID + " = '"+mongoId+"'", null);

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


        Cursor c = mApp.getContentResolver().query(ContentDescriptor.User.CONTENT_URI, USERS_FROM,
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

        Cursor c = mApp.getContentResolver().query(ContentDescriptor.User.CONTENT_URI, USERS_FROM,
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


    public void deleteAllFriends(){
        ContentResolver resolver = mApp.getContentResolver();
        resolver.delete(ContentDescriptor.User.CONTENT_URI, null, null);
    }

    public void deleteAllFriendRuns(){
        ContentResolver resolver = mApp.getContentResolver();
        resolver.delete(ContentDescriptor.RunningFriend.CONTENT_URI, null, null);
        resolver.delete(ContentDescriptor.IntervalFriend.CONTENT_URI, null, null);


    }


}
