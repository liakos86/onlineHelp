package com.kostas.dbObjects;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;

import java.util.List;

/**
 * Created by liakos on 22/4/2015.
 */
public class User {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();

    public static final String SHARED_RUNS_NUM = "sharedRunsNum";


    private String username;
    private ObjectId _id;
    private long user_id;
    private float totalDistance;
    private int totalIntervals;
    private int totalRuns;
    private long totalTime;
    private String friends;
    private String email;
    private String friendRequests;
    private int sharedRunsNum;
    private String mongoId;

    public User(){}


    public User(
            long user_id,
            String mongoId,
            String username,
            String email,
            String friends,
            String friendRequests,
            int sharedRunsNum,

            float totalDistance,
            int totalIntervals,
            int totalRuns,
            long totalTime


            ){
        this.user_id = user_id;
        this.mongoId = mongoId;
        this.username = username;
        this.email = email;
        this.friends = friends;
        this.friendRequests = friendRequests;
        this.sharedRunsNum = sharedRunsNum;
        this.totalDistance = totalDistance;
        this.totalIntervals = totalIntervals;
        this.totalRuns = totalRuns;
        this.totalTime = totalTime;

    }

    public User(SharedPreferences sp){
        this.mongoId = sp.getString("mongoId", null);
        this.username = sp.getString("username",null);
        this.email = sp.getString("email", null);
        this.friends = sp.getString("friends", null);
        this.friendRequests = sp.getString("friendRequests", null);
        this.sharedRunsNum = sp.getInt(SHARED_RUNS_NUM, 0);
        this.totalDistance = sp.getFloat("totalDistance", 0);
        this.totalIntervals = sp.getInt("totalIntervals", 0);
        this.totalRuns = sp.getInt("totalRuns", 0);
        this.totalTime = sp.getLong("totalTime", 0);
    }

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public int getSharedRunsNum() {
        return sharedRunsNum;
    }

    public void setSharedRunsNum(int sharedRunsNum) {
        this.sharedRunsNum = sharedRunsNum;
    }

    public int getTotalIntervals() {
        return totalIntervals;
    }

    public void setTotalIntervals(int totalIntervals) {
        this.totalIntervals = totalIntervals;
    }

    public String getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(String friendRequests) {
        this.friendRequests = friendRequests;
    }

    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }

    public ObjectId get_id() {
        return _id;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public String getFriends() {
        return friends;
    }

    public void setFriends(String friends) {
        this.friends = friends;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(int totalRuns) {
        this.totalRuns = totalRuns;
    }

    public static User getFromId(Context context, long id) {
        Log.v(TAG, String.format("Requesting item [%d]", id));
        synchronized (context) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(Uri.withAppendedPath(ContentDescriptor.User.CONTENT_URI,
                                String.valueOf(id)), null, null, null, null);
                cursor.moveToFirst();
                return createFromCursor(cursor);
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }
//
    public static ContentValues asContentValues(User item) {
        if (item == null)
            return null;
        synchronized (item) {
            ContentValues toRet = new ContentValues();

            toRet.put(ContentDescriptor.User.Cols.ID, item.user_id);
            toRet.put(ContentDescriptor.User.Cols.USERNAME, item.username);
            toRet.put(ContentDescriptor.User.Cols.EMAIL, item.email);
            toRet.put(ContentDescriptor.User.Cols.FRIENDS, item.friends);
            toRet.put(ContentDescriptor.User.Cols.FRIEND_REQUESTS, item.friendRequests);
            toRet.put(ContentDescriptor.User.Cols.MONGO_ID, item.mongoId);
            toRet.put(ContentDescriptor.User.Cols.TOTAL_DISTANCE, item.totalDistance);
            toRet.put(ContentDescriptor.User.Cols.TOTAL_INTERVALS, item.totalIntervals);
            toRet.put(ContentDescriptor.User.Cols.FRIEND_REQUESTS, item.friendRequests);
            toRet.put(ContentDescriptor.User.Cols.TOTAL_RUNS, item.totalRuns);
            toRet.put(ContentDescriptor.User.Cols.TOTAL_TIME, item.totalTime);
            toRet.put(ContentDescriptor.User.Cols.SHARED_RUNS_NUM, item.sharedRunsNum);



            return toRet;
        }
    }

    public static User createFromCursor(Cursor cursor) {
        synchronized (cursor) {
            if (cursor.isClosed() || cursor.isAfterLast() || cursor.isBeforeFirst()) {
                Log.v(TAG, String.format("Requesting entity but no valid cursor"));
                return null;
            }
            User toRet = new User();
            toRet.user_id = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.User.Cols.ID));
            toRet.username = cursor.getString(cursor.getColumnIndex(ContentDescriptor.User.Cols.USERNAME));
            toRet.mongoId = cursor.getString(cursor.getColumnIndex(ContentDescriptor.User.Cols.MONGO_ID));
            toRet.email = cursor.getString(cursor.getColumnIndex(ContentDescriptor.User.Cols.EMAIL));
            toRet.sharedRunsNum = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.User.Cols.SHARED_RUNS_NUM));
            toRet.friends = cursor.getString(cursor.getColumnIndex(ContentDescriptor.User.Cols.FRIENDS));
            toRet.friendRequests = cursor.getString(cursor.getColumnIndex(ContentDescriptor.User.Cols.FRIEND_REQUESTS));
            toRet.totalDistance = cursor.getFloat(cursor.getColumnIndex(ContentDescriptor.User.Cols.TOTAL_DISTANCE));
            toRet.totalTime = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.User.Cols.TOTAL_TIME));
            toRet.totalIntervals = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.User.Cols.TOTAL_INTERVALS));
            toRet.totalRuns = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.User.Cols.TOTAL_RUNS));


            return toRet;
        }
    }

    /**
     * let the id decide if we have an insert or an update
     *
     * @param resolver
     * @param item
     */
    public static void save(ContentResolver resolver, User item) {
        if (item.user_id == Database.INVALID_ID)
            resolver.insert(ContentDescriptor.User.CONTENT_URI, User.asContentValues(item));
        else
            resolver.update(ContentDescriptor.User.CONTENT_URI, User.asContentValues(item),
                    String.format("%s=?", ContentDescriptor.User.Cols.ID),
                    new String[]{
                            String.valueOf(item.user_id)
                    });
    }




}

