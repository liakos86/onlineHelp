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
import android.widget.Toast;
import com.kostas.dbObjects.Running;
import com.kostas.dbObjects.User;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;
import com.kostas.mongo.SyncHelper;
import com.kostas.onlineHelp.ActIntervalNew;
import com.kostas.onlineHelp.ActMain;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.R;

import java.util.ArrayList;
import java.util.List;


public class MongoUpdateService extends IntentService {

    public static final String NEW_FRIEND = "new_friend";
    public static final String NEW_FRIEND_REQUEST = "new_request";
    public static final String NEW_FRIEND_RUN = "new_run";


    public static final String NOTIFICATION = "com.kostas.onlineHelp";


    private Handler mHandler = new Handler();
    SharedPreferences app_preferences;
    ExtApplication application;
    SyncHelper sh;
    Database db;


    User meFromDb;

    User meFromMongo;

    List<User> friendsFromDb = new ArrayList<User>();

    List<User> friendsFromMongo = new ArrayList<User>();

    List<User> friendsWithNewRuns = new ArrayList<User>();


    private enum CallTypes {
        FETCH_FRIENDS(0), FETCH_FRIEND_RUNS(1);

        private int value;

        public int getValue() {
            return value;
        }

        private CallTypes(int value) {
            this.value = value;
        }

    }

    ;


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

        friendsFromDb = db.fetchUsersFromDb();

        meFromDb = application.getMe();

        mHandler.post(mFriendsListRunnable);

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


    private void checkForChanges() {



        for (User friendFromMongo : friendsFromMongo) {

            if (friendFromMongo.get_id().get$oid().equals(meFromDb.getMongoId())) {
                meFromMongo = friendFromMongo;

                checkMyFriendsAndRequests();

            }

            for (User friendFromDb : friendsFromDb) {

                if (friendFromMongo.get_id().get$oid().equals(friendFromDb.getMongoId())) {
                    checkRunsForUser(friendFromDb, friendFromMongo);
                }

            }

        }

        /**
         * todo
         * what if i uninstalled and then reinstalled? i have lost my sharednum
         *
         */
        app_preferences.edit().putInt("sharedRunsNum", meFromMongo.getSharedRunsNum()).apply();
        application.getMe().setSharedRunsNum(meFromMongo.getSharedRunsNum());
        friendsFromMongo.remove(meFromMongo);
        getNewFriendRuns();





    }

    /**
     * Check if my friends have changed in mongoDb which means someone has accepted my request
     * Check if i have a new friend request in mongo
     */
    private void checkMyFriendsAndRequests() {

        String mongoFriends = meFromMongo.getFriends() != null ? meFromMongo.getFriends().trim() : "";
        String dbFriends = meFromDb.getFriends() != null ? meFromDb.getFriends().trim() : "";

        if ( dbFriends.length() < mongoFriends.length()) {
            createForegroundNotification("You have a new friend!!!");

            app_preferences.edit().putString("friends", mongoFriends).apply();
            application.getMe().setFriends(mongoFriends);

            Intent intent = new Intent(NOTIFICATION);
            intent.putExtra(NEW_FRIEND, true);
            sendBroadcast(intent);
        }

        String mongoRequests = meFromMongo.getFriendRequests() != null ? meFromMongo.getFriendRequests().trim() : "";
        String dbRequests = meFromDb.getFriendRequests() != null ? meFromDb.getFriendRequests().trim() : "";

        if (dbRequests.length() < mongoRequests.length()) {
            app_preferences.edit().putString("friendRequests", mongoRequests).apply();
            application.getMe().setFriendRequests(mongoRequests);
            createForegroundNotification("You have a new friend request!!!");

            Intent intent = new Intent(NOTIFICATION);
            intent.putExtra(NEW_FRIEND_REQUEST, true);
            sendBroadcast(intent);

        }

    }

    /**
     * For every user I have in db i check his runs against the ones i got from mongo
     * If the number is larger his means he has shared a new run
     * @param userDb
     * @param userMongo
     */
    private void checkRunsForUser(User userDb, User userMongo) {

        if (userMongo.getSharedRunsNum() > userDb.getSharedRunsNum()) {
            friendsWithNewRuns.add(userMongo);

            userDb.setSharedRunsNum(userMongo.getSharedRunsNum());

        }

    }

    /**
     * create an async call for every user in the list of the friends with new runs
     */
    private void getNewFriendRuns(){//todo turn friendsWithNewRuns to HashMap in order to get the new N runs. Now i get only the last

        for (User friend : friendsWithNewRuns){
            db.updateUserRuns(friend.get_id().get$oid(), friend.getSharedRunsNum());
        }

        if (friendsWithNewRuns.size() > 0) {
            new PerformAsyncTask(CallTypes.FETCH_FRIEND_RUNS).execute();
        } else{
            refreshFriends();
        }

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

    /**
     *
     * TODO maybe a more clever way
     */
    private void refreshFriends(){
        if (friendsFromMongo.size() > friendsFromDb.size()) {
            //todo: ???
            db.deleteAllFriends();
            friendsFromDb.clear();
            for (User fr : friendsFromMongo){

                fr.setMongoId(fr.get_id().get$oid());
                db.addUser(fr);
                friendsFromDb.add(fr);
            }



        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private Runnable mFriendsListRunnable = new Runnable() {

        public void run() {

            if (meFromDb == null && app_preferences.getString("username", null) != null) {
                meFromDb = application.getMe();
            }


            if (app_preferences.getString("username", null) != null) {
                new PerformAsyncTask(CallTypes.FETCH_FRIENDS).execute();
            }

            mHandler.postDelayed(mFriendsListRunnable, 30000);//todo: 30 minutes

        }
    };


    private class PerformAsyncTask extends AsyncTask<Void, Void, Void> {


        CallTypes type;

        PerformAsyncTask(CallTypes type) {
            this.type = type;
        }


        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... unused) {
            // SharedPreferences.Editor editor = app_preferences.edit();


            if (type == CallTypes.FETCH_FRIENDS) {

                String friends = app_preferences.getString("friends", "");
                String[] friendsArray = friends.split(" ");
                String myUsername = meFromDb != null ? meFromDb.getUsername() : app_preferences.getString("username", "");

                ArrayList<String> usernames = new ArrayList<String>();
                for (String name : friendsArray) {
                    usernames.add(name);
                }

                usernames.add(myUsername);

                usernames.remove(null);
                usernames.remove("");

                friendsFromMongo = sh.getUsersByUsernamesList(usernames);

                checkForChanges();

            }else{

                List<Running> newRuns = sh.getNewRunsForUsers(friendsWithNewRuns);


                for (Running run : newRuns){


                    if (run.get_id()!=null) {
                        run.setRunning_id(-1);
                        db.addRunning(run, ContentDescriptor.RunningFriend.CONTENT_URI, ContentDescriptor.IntervalFriend.CONTENT_URI);
                    }
                    createForegroundNotification("A friend added a run");

                    Intent intent = new Intent(NOTIFICATION);
                    intent.putExtra(NEW_FRIEND_RUN, true);
                    sendBroadcast(intent);
                }

                friendsWithNewRuns.clear();

               refreshFriends();

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }
}