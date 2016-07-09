package com.kostas.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.kostas.dbObjects.User;
import com.kostas.model.Database;
import com.kostas.mongo.SyncHelper;
import com.kostas.onlineHelp.ActIntervalNew;
import com.kostas.onlineHelp.ActMain;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.R;

import java.util.ArrayList;
import java.util.List;


public class MongoUpdateService extends IntentService
         {


             private Handler mHandler = new Handler();
             SharedPreferences app_preferences;
             ExtApplication application;
             SyncHelper sh;
             Database db;



             User meFromDb;

             User meFromMongo;

             List<User> friendsFromDb;

             List<User> friendsFromMongo;


             private enum CallTypes{
                 FETCH_FRIENDS(0), FETCH_FRIEND_RUNS(1);

                 private int value;

                 public int getValue() {
                     return value;
                 }

                 private CallTypes(int value){
                     this.value = value;
                 }

             };


    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        application = (ExtApplication) getApplication();
        app_preferences = getApplication().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        sh = new SyncHelper(application);
        db = new Database(application);


    }




    public MongoUpdateService() {
        super("MongoUpdateService");
    }

    /**
     * Can be called multiple times. Basically called every time startService()
     * is called from an activity
     *
     * @param intent
     * @param startId
     */
    @Override
    public void onStart(Intent intent, int startId) {

       // SharedPreferences.Editor editor = app_preferences.edit();
       // if (startId == 1) {//first
            //editor.putBoolean(INTERVAL_IN_PROGRESS, false).apply();


        String myUsername = app_preferences.getString("username", "");

        friendsFromDb = db.fetchUsersFromDb();

        for (User user : friendsFromDb){

            if (myUsername.equals(user.getUsername())){
                meFromDb = user;
            }

        }



        mHandler.post(mFriendsListRunnable);





        //    return;
       // }


    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public boolean stopService(Intent name) {


        return super.stopService(name);
    }

    /**
     * Create a notification with a title and a message
     * that leads to my activity_main activity when pressed
     * <p/>
     * If the sdk is >15 then I set flags like the
     * color of the light to blink
     */
    private void createForegroundNotification(String message) {

        NotificationManager notificationManager =
                (NotificationManager) application.getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        final int myID = 1234;
        if (Build.VERSION.SDK_INT > 15) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(application);
            mBuilder.setSmallIcon(R.drawable.ic_notification_icon);
            int color = getResources().getColor(R.color.interval_green);
            mBuilder.setColor(color);
            mBuilder.setContentTitle(message);
            mBuilder.setContentText("Click for info");
//            mBuilder.setOngoing(true);
            Intent resultIntent = new Intent(application, ActMain.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(application);
            stackBuilder.addParentStack(ActMain.class);
// Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            Notification notification = mBuilder.build();
            notification.ledARGB = getResources().getColor(R.color.interval_green);
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
           // notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.ledOffMS = 1000;
            notification.ledOnMS = 1500;

            // notificationID allows you to update the notification later on.
            //startForeground(myID, notification);

            notificationManager.notify(123, notification);
        } else {
//The intent to launch when the user clicks the expanded notification
            Intent intent2 = new Intent(this, ActIntervalNew.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent2, 0);
//This constructor is deprecated. Use Notification.Builder instead
            Notification notice = new Notification(R.drawable.ic_notification_icon, message, System.currentTimeMillis());
//This method is deprecated. Use Notification.Builder instead.
            notice.setLatestEventInfo(this, "Click for info", "Running", pendIntent);
            //notice.flags |= Notification.FLAG_NO_CLEAR;


            notificationManager.notify(123, notice);
            //startForeground(myID, notice);
        }
    }


    private void checkForChanges(){

        if (friendsFromMongo.size() > friendsFromDb.size()){
            //todo: ???
        }

        for (User friendFromMongo : friendsFromMongo){

            if (friendFromMongo.get_id().get$oid().equals(meFromDb.getMongoId())){
                meFromMongo = friendFromMongo;
                checkMyFriendsAndRequests();

            }

            for (User friendFromDb : friendsFromDb){

                if (friendFromMongo.get_id().get$oid().equals(friendFromDb.getMongoId())){
                    checkRunsForUser(friendFromDb, friendFromMongo);
                }

            }

        }


    }

    private void checkMyFriendsAndRequests(){
        if (meFromMongo.getFriends() != null && meFromDb.getFriends().trim().length() < meFromMongo.getFriends().trim().length()){
            createForegroundNotification("You have a new friend!!!");
        }

        if (meFromMongo.getFriendRequests() != null && meFromDb.getFriendRequests().trim().length() < meFromMongo.getFriendRequests().trim().length()){
            createForegroundNotification("You have a new friend request!!!");
        }

    }

    private void checkRunsForUser(User userDb, User userMongo){


    }



    @Override
    public void onDestroy() {
        try {

            mHandler.removeCallbacks(mFriendsListRunnable);


        } catch (Exception e) {
            //Log.v("LATLNG", "Crash");

        } finally {
            super.onDestroy();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private Runnable mFriendsListRunnable = new Runnable() {

        public void run() {

            if (meFromDb == null && app_preferences.getString("username", null) != null){
                meFromDb = db.fetchUser(app_preferences.getString("username",""));
            }


            if (app_preferences.getString("username", null) != null) {
                new PerformAsyncTask(CallTypes.FETCH_FRIENDS).execute();
            }

            mHandler.postDelayed(mFriendsListRunnable, 30000);//todo: 30 minutes

        }
    };




             private class PerformAsyncTask extends AsyncTask<Void, Void, Void> {


                 CallTypes type;

                 PerformAsyncTask(CallTypes type){
                     this.type = type;
                 }


        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... unused) {
           // SharedPreferences.Editor editor = app_preferences.edit();


           if (type == CallTypes.FETCH_FRIENDS){

               String friends = app_preferences.getString("friends", "");
               String[]friendsArray = friends.split(" ");
               String myUsername = meFromDb != null ? meFromDb.getUsername() : app_preferences.getString("username", "");

               ArrayList<String> usernames = new ArrayList<String>();
               for (String name : friendsArray){
                   usernames.add(name);
               }

               usernames.add(myUsername);

               usernames.remove(null);
               usernames.remove("");

               friendsFromMongo =  sh.getUsersByUsernamesList(usernames);

               checkForChanges();

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }
}