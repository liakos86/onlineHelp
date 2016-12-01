package com.kostas.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.kostas.dbObjects.Running;
import com.kostas.dbObjects.User;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;
import com.kostas.mongo.SyncHelper;
import com.kostas.onlineHelp.*;

import java.util.ArrayList;
import java.util.List;

public class MongoUpdateService extends IntentService {

    public static final String NEW_FRIEND = "new_friend";
    public static final String NEW_FRIEND_REQUEST = "new_request";
    public static final String NEW_FRIEND_RUN_IN_DB = "new_run";

    private Handler mHandler = new Handler();
    SharedPreferences app_preferences;
    ExtApplication application;
    SyncHelper sh;
    Database db;
    User meFromDb;
    User meFromMongo;
    List<User> friendsWithRunsAndIntervalsFromDb = new ArrayList<User>();
    List<User> friendsFromMongo = new ArrayList<User>();
    List<Running> allFriendRunsFromMongo = new ArrayList<Running>();


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
        friendsWithRunsAndIntervalsFromDb = db.fetchUsersWithRunsAndIntervalsFromDb();
        meFromDb = application.getMe();
        mHandler.post(mUpdateFriendsFromMongoRunnable);
        startForeground(0, new Notification());
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
            resultIntent.putExtra(AppConstants.NEW_FRIEND_OR_RUN, true);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(application);
            stackBuilder.addParentStack(ActMain.class);
// Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            Notification notification = mBuilder.build();
            notification.ledARGB = getResources().getColor(R.color.interval_green);
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
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
            notice.flags |= Notification.FLAG_AUTO_CANCEL;


            notificationManager.notify(123, notice);
            //startForeground(myID, notice);
        }
    }


    private void checkForNewFriendRunsAndMyRequests() {

        for (User friendFromMongo : friendsFromMongo){
            if (friendFromMongo.get_id().get$oid().equals(meFromDb.getMongoId())) {
                meFromMongo = friendFromMongo;
                break;
            }
        }
        checkMyFriendsAndRequests();
        friendsFromMongo.remove(meFromMongo);

        for (User friendFromMongo : friendsFromMongo) {
            for (User friendFromDb : friendsWithRunsAndIntervalsFromDb) {
                if (friendFromMongo.get_id().get$oid().equals(friendFromDb.getMongoId())) {
                    if (friendFromMongo.getSharedRuns().size() > friendFromDb.getSharedRuns().size()){
                        createForegroundNotification(friendFromMongo.getUsername()+" has added a new run");
                    }
                }
            }
            allFriendRunsFromMongo.addAll(friendFromMongo.getSharedRuns());
        }
    }

    /**
     * Check if my friends have changed in mongoDb which means someone has accepted my request
     * Check if i have a new friend request in mongo
     */
    private void checkMyFriendsAndRequests() {

        String mongoFriends = meFromMongo.getFriends() != null ? meFromMongo.getFriends().trim() : AppConstants.EMPTY;
        String dbFriends = meFromDb.getFriends() != null ? meFromDb.getFriends().trim() : AppConstants.EMPTY;

        if ( dbFriends.length() < mongoFriends.length()) {
            createForegroundNotification("You have a new friend!!!");
            app_preferences.edit().putString("friends", mongoFriends).apply();
            application.getMe().setFriends(mongoFriends);
            Intent intent = new Intent(AppConstants.NOTIFICATION);
            intent.putExtra(NEW_FRIEND, true);
            sendBroadcast(intent);
        }

        String mongoRequests = meFromMongo.getFriendRequests() != null ? meFromMongo.getFriendRequests().trim() : AppConstants.EMPTY;
        String dbRequests = meFromDb.getFriendRequests() != null ? meFromDb.getFriendRequests().trim() : AppConstants.EMPTY;

        if (dbRequests.length() < mongoRequests.length()) {
            app_preferences.edit().putString("friendRequests", mongoRequests).apply();
            application.getMe().setFriendRequests(mongoRequests);
            createForegroundNotification("You have a new friend request!!!");
            Intent intent = new Intent(AppConstants.NOTIFICATION);
            intent.putExtra(NEW_FRIEND_REQUEST, true);
            sendBroadcast(intent);
        }
    }

    /**
     *
     * TODO maybe a more clever way
     */
    private void refreshFriends(){
        if (friendsFromMongo.size() > friendsWithRunsAndIntervalsFromDb.size()) {
            //todo: ???
            db.deleteAllFriends();
            friendsWithRunsAndIntervalsFromDb.clear();
            for (User fr : friendsFromMongo){
                fr.setMongoId(fr.get_id().get$oid());
                db.addUser(fr);
                friendsWithRunsAndIntervalsFromDb.add(fr);
            }
        }
    }

    @Override
    public void onDestroy() {
        try {
            mHandler.removeCallbacks(mUpdateFriendsFromMongoRunnable);
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


    /**
     * Starts an async task to fetch all friends.
     * First I set my user from the application.
     * Then, only if user is logged I start the async process.
     * The circle is repeated after selected time.
     */
    private Runnable mUpdateFriendsFromMongoRunnable = new Runnable() {

        public void run() {
            if (meFromDb == null && app_preferences.getString("username", null) != null) {
                meFromDb = application.getMe();
            }

            if (app_preferences.getString("username", null) != null) {
                new PerformAsyncTask().execute();
            }

            mHandler.postDelayed(mUpdateFriendsFromMongoRunnable, 50000);//todo: 30 minutes

        }
    };


    private class PerformAsyncTask extends AsyncTask<Void, Void, Void> {

        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... unused) {
            String friends = app_preferences.getString("friends", AppConstants.EMPTY);
            String[] friendsArray = friends.split(" ");
            String myUsername = meFromDb != null ? meFromDb.getUsername() : app_preferences.getString("username", AppConstants.EMPTY);

            ArrayList<String> userNames = new ArrayList<String>();
            for (String name : friendsArray) {
                userNames.add(name);
            }
            userNames.add(myUsername);
            userNames.remove(null);
            userNames.remove(AppConstants.EMPTY);

            friendsFromMongo = sh.getUsersWithRunsAndIntervalsByUsernameMongo(userNames);
            checkForNewFriendRunsAndMyRequests();
            refreshFriends();
            db.deleteAllFriendRuns();

            for (Running run : allFriendRunsFromMongo) {
                if (run.get_id() != null) {
                    run.setRunning_id(-1);
                    db.addRunningWithIntervals(run, ContentDescriptor.RunningFriend.CONTENT_URI, ContentDescriptor.IntervalFriend.CONTENT_URI);
                }

                Intent intent = new Intent(AppConstants.NOTIFICATION);
                intent.putExtra(NEW_FRIEND_RUN_IN_DB, true);
                sendBroadcast(intent);


            }
            allFriendRunsFromMongo.clear();
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
        }
    }
}
