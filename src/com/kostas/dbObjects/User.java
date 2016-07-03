package com.kostas.dbObjects;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.List;

/**
 * Created by liakos on 22/4/2015.
 */
public class User {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName();


    private String username;
    private ObjectId _id;
    private long user_id;
    private float totalDistance;
    private long totalTime;
    private int totalScore;
    private String friends; //email list as it is unique
    private String email;
    private String friendRequests;
    private String sentRequests;

    public User(){}

    public User(
            long user_id,
            String username,
             int totalChallenges,
            int wonChallenges,
            int totalScore){
        this.user_id = user_id;
        this.username = username;
        this.totalScore = totalScore;

    }

    public User(SharedPreferences prefs){
        this._id = new ObjectId(prefs.getString("mongoId",null));
        this.username = prefs.getString("username", "");
        this.totalDistance = prefs.getFloat("totalDistance", 0);
        this.totalScore = prefs.getInt("totalScore",0);
        this.totalTime = prefs.getLong("totalTime",0);
        this.friends = prefs.getString("friends","");
        this.friendRequests = prefs.getString("friendRequests","");
    }

    public String getSentRequests() {
        return sentRequests;
    }

    public void setSentRequests(String sentRequests) {
        this.sentRequests = sentRequests;
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

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
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


//    public static User getFromId(Context context, long id) {
//        Log.v(TAG, String.format("Requesting item [%d]", id));
//        synchronized (context) {
//            Cursor cursor = null;
//            try {
//                cursor = context.getContentResolver()
//                        .query(Uri.withAppendedPath(ContentDescriptor.User.CONTENT_URI,
//                                String.valueOf(id)), null, null, null, null);
//                cursor.moveToFirst();
//                return createFromCursor(cursor);
//            } finally {
//                if (cursor != null)
//                    cursor.close();
//            }
//        }
//    }
//
//    public static ContentValues asContentValues(User item) {
//        if (item == null)
//            return null;
//        synchronized (item) {
//            ContentValues toRet = new ContentValues();
//
//            toRet.put(ContentDescriptor.User.Cols.ID, item.user_id);
//            toRet.put(ContentDescriptor.User.Cols.USERNAME, item.username);
//            toRet.put(ContentDescriptor.User.Cols.TOTAL_CHALLENGES, item.totalChallenges);
//            toRet.put(ContentDescriptor.User.Cols.WON_CHALLENGES, item.wonChallenges);
//            toRet.put(ContentDescriptor.User.Cols.TOTAL_SCORE, item.totalScore);
//
//
//            return toRet;
//        }
//    }
//
//    public static User createFromCursor(Cursor cursor) {
//        synchronized (cursor) {
//            if (cursor.isClosed() || cursor.isAfterLast() || cursor.isBeforeFirst()) {
//                Log.v(TAG, String.format("Requesting entity but no valid cursor"));
//                return null;
//            }
//            User toRet = new User();
//            toRet.user_id = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.User.Cols.ID));
//            toRet.username = cursor.getString(cursor.getColumnIndex(ContentDescriptor.User.Cols.USERNAME));
//            toRet.totalChallenges = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.User.Cols.TOTAL_CHALLENGES));
//            toRet.wonChallenges = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.User.Cols.WON_CHALLENGES));
//            toRet.totalScore = cursor.getInt(cursor.getColumnIndex(ContentDescriptor.User.Cols.TOTAL_SCORE));
//
//            return toRet;
//        }
//    }

    /**
     * let the id decide if we have an insert or an update
     *
     * @param resolver
     * @param item
     */
//    public static void save(ContentResolver resolver, User item) {
//        if (item.user_id == Database.INVALID_ID)
//            resolver.insert(ContentDescriptor.User.CONTENT_URI, User.asContentValues(item));
//        else
//            resolver.update(ContentDescriptor.User.CONTENT_URI, User.asContentValues(item),
//                    String.format("%s=?", ContentDescriptor.User.Cols.ID),
//                    new String[]{
//                            String.valueOf(item.user_id)
//                    });
//    }




}

