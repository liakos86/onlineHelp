package com.kostas.mongo;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Running;
import com.kostas.dbObjects.User;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;
import com.kostas.onlineHelp.ActMain;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.R;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

//todo sendme
public class SyncHelper {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName()
            .substring(0, 23);
    private ExtApplication application;
    Database dbHelper;
    String running_collection, interval_collection, runner_collection, authUrl, apiKey ;
    DefaultHttpClient client;
    private SharedPreferences app_preferences;

    public SyncHelper(ExtApplication application) {
        this.application = application;
        running_collection = application.getResources().getString(R.string.running_collection_mongo_path);
        interval_collection = application.getResources().getString(R.string.interval_collection_mongo_path);
        authUrl = application.getResources().getString(R.string.auth_url);
        apiKey = application.getResources().getString(R.string.apiKey);
        runner_collection = application.getResources().getString(R.string.runner_collection_mongo_path);
        app_preferences = application.getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        dbHelper = new Database(application);
        client = application.getHttpClient();
    }

    public int insertMongoUser(String email, String username, String password){
        Log.v(TAG, "Inserting user");
        int returnCode = -2;//server error default ?
        Uri uri = new Uri.Builder()
                    .scheme("https")
                    .authority(authUrl)
                    .path(runner_collection)
                    .appendQueryParameter("apiKey", apiKey)
                    .build();

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try{
            HttpResponse response;
                JSONObject runner = new JSONObject();
                runner.put("username", username);
                runner.put("password", password);
                runner.put("email", email);
                StringEntity se = new StringEntity( runner.toString());

                HttpPost httpPost = new HttpPost(uri.toString());
                setDefaultPostHeaders(httpPost);
                httpPost.setEntity(se);
                Log.v(TAG, "inserting new user");
                response = client.execute(httpPost);

            Log.v(TAG, "user acquisition- response received");
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() >= 300) {
                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
            }

            String resultString = null;
            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);
                instream.close();
            }
            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Gson gson = new Gson();
            User user2 = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());

            if (user2==null){
                return 0; //no user found
            }

            user2.setMongoId(user2.get_id().get$oid());//it comes with object, I unpack to store in db
            application.setMe(user2);

            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putString("mongoId", user2.get_id().get$oid());
            editor.putString("username", user2.getUsername());
            editor.putLong("totalTime", user2.getTotalTime());
            editor.putFloat("totalDistance", user2.getTotalDistance());
            editor.putString("friends", user2.getFriends());
            editor.putString("friendRequests", user2.getFriendRequests());
            editor.apply();
            returnCode = 3;
        }catch (Exception e){
            Log.e(TAG, "Exception inserting user", e);
            return -2;
        }
        Log.v(TAG, "Successfully inserted user");

        return returnCode;
    }

    public int getMongoUser(String email, String username, String password) {
        Log.v(TAG, "Fetching user");
        int returnCode = -2;//server error default ?
        Uri uri=null;

         if (username.length()>0 || email.length()>0){//try get existing user by email or username

            String query = "{ 'email':'"+email+"' , 'password' :'"+password+"' }";
            if (username.length()>0){
               query =  "{ 'username':'"+username+"' , 'password' :'"+password+"' }";
            }

             uri = new Uri.Builder()
                    .scheme("https")
                    .authority(authUrl)
                    .path(runner_collection)
                    .appendQueryParameter("q", query)
                    .appendQueryParameter("apiKey", apiKey)
                     .appendQueryParameter("fo", "true")
                    .build();
        }

            DefaultHttpClient client = application.getHttpClient();
            client.setParams(getMyParams());

        try {

            HttpResponse response;

                HttpGet httpRequest = new HttpGet(uri.toString());
                setDefaultGetHeaders(httpRequest);
                Log.v(TAG, "fetching existing user");
                response = client.execute(httpRequest);
            Log.v(TAG, "user acquisition- response received");

            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);
                instream.close();
            }
            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Gson gson = new Gson();
            User user2 = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());

            if (user2==null){
                return -1; //no user found
            }

                user2.setMongoId(user2.get_id().get$oid());//it comes with object, I unpack to store in db
                application.setMe(user2);

               SharedPreferences.Editor editor = app_preferences.edit();
               editor.putString("mongoId", user2.get_id().get$oid());
                editor.putString("username", user2.getUsername());
                editor.putLong("totalTime", user2.getTotalTime());
                editor.putFloat("totalDistance", user2.getTotalDistance());
                editor.putString("friends", user2.getFriends());
                editor.putString("friendRequests", user2.getFriendRequests());
                editor.apply();
               returnCode = 2;

        } catch (Exception e) {
            Log.e(TAG, "Exception fetching user", e);
            return -2;
        }

        Log.v(TAG, String.format("Fetching existing user - done"));

        return returnCode;

    }

    public int shareRunToMongo(Running runToShare) {

        Log.v(TAG, "sharing run");

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(running_collection)
                .appendQueryParameter("apiKey", apiKey)
                .build();

//        HashMap  params = new HashMap();
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());
        HttpPost httpPost = new HttpPost(uri.toString());

        try {

            User me  = application.getMe();

            JSONObject workout = new JSONObject();
            workout.put("username", app_preferences.getString("username",""));
            workout.put("time", runToShare.getTime());
            workout.put("date", runToShare.getDate());
            workout.put("distance", runToShare.getDistance());
            workout.put("avgPaceText", runToShare.getAvgPaceText());
            workout.put("description", runToShare.getDescription());
            workout.put("sharedId", me.getSharedRunsNum()+1);

            Log.v("SHAREDID", (me.getSharedRunsNum() + 1) + "");

                    StringEntity se = new StringEntity(workout.toString());
            setDefaultPostHeaders(httpPost);
            httpPost.setEntity(se);
            Log.v(TAG, "uploading run - requesting");
            HttpResponse response = client.execute(httpPost);
            Log.v(TAG, "uploading run - response received");
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            Log.v(TAG, String.format(" status [%s]", statusLine));
            if (statusLine.getStatusCode() >= 300) {
                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Gson gson = new Gson();


            Running run = (Running) gson.fromJson(resultString,
                    new TypeToken<Running>() {
                    }.getType());



            if (run == null || run.get_id() == null){
                return 0;
            }

            me.setSharedRunsNum(me.getSharedRunsNum() + 1);
            app_preferences.edit().putInt(User.SHARED_RUNS_NUM, me.getSharedRunsNum()).apply();

            updateMySharedRunsNum();

            for (Interval interval : runToShare.getIntervals()){
                interval.setRunning_mongo_id(run.get_id().get$oid());
                int code = shareIntervalForRunMongo(interval);
                if (code == 0){
                    return 0;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception uploading run", e);
            return 0;
        }

        Log.v(TAG, String.format("uploaded run - done"));
        return 1;
    }

    public int shareIntervalForRunMongo(Interval intervalToShare) {

        Log.v(TAG, "sharing interval");

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(interval_collection)
                .appendQueryParameter("apiKey", apiKey)
                .build();

//        HashMap  params = new HashMap();
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());
        HttpPost httpPost = new HttpPost(uri.toString());


        try {

            JSONObject intervalSession = new JSONObject();
            intervalSession.put("paceText", intervalToShare.getPaceText());
            intervalSession.put("milliseconds", intervalToShare.getMilliseconds());
            intervalSession.put("isFastest", intervalToShare.isFastest());
            intervalSession.put("distance", intervalToShare.getDistance());
            intervalSession.put("latLonList", intervalToShare.getLatLonList());
            intervalSession.put("running_mongo_id", intervalToShare.getRunning_mongo_id());

            StringEntity se = new StringEntity( intervalSession.toString());
            setDefaultPostHeaders(httpPost);
            httpPost.setEntity(se);
            Log.v(TAG, "uploading run - requesting");
            HttpResponse response = client.execute(httpPost);
            Log.v(TAG, "uploading run - response received");
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            Log.v(TAG, String.format(" status [%s]", statusLine));
            if (statusLine.getStatusCode() >= 300) {
                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Gson gson = new Gson();

            Interval intervalSaved = (Interval) gson.fromJson(resultString,
                    new TypeToken<Interval>() {
                    }.getType());

            if (intervalSaved == null ){
                return 0;
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception uploading interval", e);
            return 0;
        }

        Log.v(TAG, String.format("uploaded interval - done"));
        return 1;
    }

    public List<User> sentFriendRequest(String friend){
        List<User> users = new ArrayList<User>();
            Log.v(TAG, "Fetching user "+friend);
            String query;

            if (friend == null || friend.equals("")) {
                return users;
            } else {
                    query = "{ 'username' : '"+friend+"'}";
            }

            Uri uri = new Uri.Builder()
                    .scheme("https")
                    .authority(authUrl)
                    .path(runner_collection)
                    .appendQueryParameter("q", query)
                    .appendQueryParameter("s", "{'totalScore': -1}")
                    .appendQueryParameter("apiKey", apiKey)
                    .build();

            DefaultHttpClient client = application.getHttpClient();
            client.setParams(getMyParams());

            try {
                HttpGet httpRequest = new HttpGet(uri.toString());
                setDefaultGetHeaders(httpRequest);

                Log.v(TAG, "Fetching runs - requesting");
                HttpResponse response = client.execute(httpRequest);
                Log.v(TAG, "Fetching runs - responce received");

                HttpEntity entity = response.getEntity();

                StatusLine statusLine = response.getStatusLine();

                Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));

                if (statusLine.getStatusCode() >= 300) {
                    Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
                }

                String resultString = null;

                if (entity != null) {
                    InputStream instream = entity.getContent();
                    Header contentEncoding = response.getFirstHeader("Content-Encoding");
                    if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                        instream = new GZIPInputStream(instream);
                    }
                    resultString = Utils.convertStreamToString(instream);

                    instream.close();
                }

                Log.v(TAG, String.format("Deserialising [%s]", resultString));

                Gson gson = new Gson();
                users = (List<User>) gson.fromJson(resultString,
                        new TypeToken<List<User>>() {
                        }.getType());

                //add a new friend request to the other user
                if (users.size() == 1) {
                    getMongoUserByUsernameForFriend(users.get(0));
                }
                Log.v(TAG, String.format("Fetching parts - retrieved [%d] users", users.size()));

           } catch (Exception e) {
                Log.e(TAG, "Exception fetching leaderboard or friend", e);
                return users;
            }

            Log.v(TAG, String.format("Fetching leader or friend - done"));

            return users;
    }

    public User getMongoUserByUsernameForFriend(User user) {// 1 send request, 0 accept, 2 reject, else just get user

        String myUsername = app_preferences.getString("username", "");
            String currentRequests = user.getFriendRequests() != null ? user.getFriendRequests()+" " : "";
                uploadNewFriendOrRequest(currentRequests  + myUsername, user.getUsername());
                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putString("sentRequests",  app_preferences.getString("sentRequests","")+user.getUsername());
                editor.apply();

        Log.v(TAG, String.format("Fetching user - done"));
        return user;
    }

    public boolean uploadNewFriendOrRequest(String friends,String username){

        Log.v(TAG, "Uploading new friend");
        String query = "{'username': '"+username+"'}";

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(runner_collection)
                .appendQueryParameter("q", query)
                .appendQueryParameter("apiKey", apiKey)
                .build();

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            JSONObject obj = new JSONObject();


            obj.put("friendRequests" , friends);

            JSONObject lastObj = new JSONObject();
            lastObj.put("$set", obj);

            StringEntity se = new StringEntity(lastObj.toString());

            HttpPut httpRequest = new HttpPut(uri.toString());

            httpRequest.setEntity(se);


            setDefaultPutHeaders(httpRequest);

            Log.v(TAG, "new friend - requesting");
            HttpResponse response = client.execute(httpRequest);
            Log.v(TAG, "new friend - responce received");

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));

            if (statusLine.getStatusCode() >= 300) {
//                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
                return false;
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Log.v(TAG, String.format("Fetching parts - new friend added"));
        } catch (Exception e) {
            Log.e(TAG, "Exception inserting friend", e);
            return false;
        }

        Log.v(TAG, String.format("uploaded friend - done"));
        return true;

    }

    public void updateMySharedRunsNum(){

        String query = "{'username': '"+application.getMe().getUsername()+"'}";

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(runner_collection)
                .appendQueryParameter("q", query)
                .appendQueryParameter("apiKey", apiKey)
                .build();


        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());


        try {
            JSONObject obj = new JSONObject();
            obj.put(User.SHARED_RUNS_NUM , application.getMe().getSharedRunsNum());
            JSONObject lastObj = new JSONObject();
            lastObj.put("$set", obj);

            StringEntity se = new StringEntity(lastObj.toString());
            HttpPut httpRequest = new HttpPut(uri.toString());
            httpRequest.setEntity(se);
            setDefaultPutHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() >= 300) {
                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
                return;
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);
                instream.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception updating sharedRunsNum", e);
            return;
        }

        Log.v(TAG, String.format("updated sharedRunsNum - done"));
        return;

    }

    public User acceptOrRejectFriend(String username, int type) {// 1 send request, 0 accept, 2 reject, else just get user

        Log.v(TAG, "Fetching user");

        String myUsername = app_preferences.getString("username", "");

        User user = null;

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(runner_collection)
                .appendQueryParameter("q", "{ 'username':'"+username+"' }")
                .appendQueryParameter("fo", "true")
                .appendQueryParameter("apiKey", apiKey)
                .build();

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {

            HttpResponse response;


            HttpGet httpRequest = new HttpGet(uri.toString());
            setDefaultGetHeaders(httpRequest);
            Log.v(TAG, "Fetching user - requesting");
            response = client.execute(httpRequest);



            Log.v(TAG, "Fetching user - responce received");

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            Log.v(TAG, String.format("Fetching user - status [%s]", statusLine));

            if (statusLine.getStatusCode() >= 300) {
//                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Gson gson = new Gson();
            user = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());

            // refresh other users requests
           if (type==0|| type==2) {
                //add friend to both users list
                if ((type==0)&&((user.getFriends()==null || !user.getFriends().contains(myUsername) )&& !app_preferences.getString("friends", "").contains(user.getUsername()))){
                    fixFriendsListForUser(user.getFriends() + " " + myUsername, user.getUsername(), type);
                    fixFriendsListForUser(app_preferences.getString("friends", "") + " " + user.getUsername(), myUsername, type);
                    dbHelper.addUser(user);
                }

                //remove his name
                String  newFriendRequests = app_preferences.getString("friendRequests","").replace(" " + user.getUsername() + " ", " ");
                newFriendRequests = newFriendRequests.replace(user.getUsername() + " ", " ");
                newFriendRequests = newFriendRequests.replace(" "+user.getUsername(), " ");
                newFriendRequests = newFriendRequests.replace(user.getUsername(), "");
               fixFriendsListForUser(newFriendRequests, myUsername, 1);

                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putString("friendRequests", newFriendRequests);
                if (type==0) {
                    editor.putString("friends", app_preferences.getString("friends", "") + " " + user.getUsername());
                }
                editor.apply();

            }

        } catch (Exception e) {
            Log.e(TAG, "Exception fetching user", e);
            return user;
        }

        Log.v(TAG, String.format("Fetching user - done"));
        return user;

    }

    public boolean fixFriendsListForUser(String friends,String username, int type){


        Log.v(TAG, "Uploading new friend");

        String query = "{'username': '"+username+"'}";
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(runner_collection)
                .appendQueryParameter("q", query)
                .appendQueryParameter("apiKey", apiKey)
                .build();


        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());


        try {
            JSONObject obj = new JSONObject();

            if (type==0)
                obj.put("friends" , friends);
            else if (type==1)
                obj.put("friendRequests" , friends);

            JSONObject lastObj = new JSONObject();
            lastObj.put("$set", obj);

            StringEntity se = new StringEntity(lastObj.toString());
            HttpPut httpRequest = new HttpPut(uri.toString());
            httpRequest.setEntity(se);
            setDefaultPutHeaders(httpRequest);

            Log.v(TAG, "new friend - requesting");
            HttpResponse response = client.execute(httpRequest);
            Log.v(TAG, "new friend - responce received");

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));

            if (statusLine.getStatusCode() >= 300) {
//                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
                return false;
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Log.v(TAG, String.format("Fetching parts - new friend added"));
        } catch (Exception e) {
            Log.e(TAG, "Exception inserting friend", e);
            return false;
        }

        Log.v(TAG, String.format("uploaded friend - done"));
        return true;

    }

    public List<User> getUsersByUsernamesList(ArrayList<String>usernames){//0 leaderboard

        List<User>users = new ArrayList<User>();
        if (usernames.size() == 0){
            return users;
        }

        Log.v(TAG, "Fetching users");
        String query;

        int size = usernames.size();

        query = "{ $or: [";
        for (int i = 0; i < size - 1; i++) {
            query += "{ 'username': '" + usernames.get(i).trim() + "'},";
        }
        query += "{ 'username': '" + usernames.get(size-1).trim() + "'}";
        query += "] }";

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(runner_collection)
                .appendQueryParameter("q", query)
                .appendQueryParameter("s", "{'totalRuns': -1}")
                .appendQueryParameter("apiKey", apiKey)
                .build();


        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());


        try {

            HttpGet httpRequest = new HttpGet(uri.toString());


            setDefaultGetHeaders(httpRequest);

            Log.v(TAG, "Fetching friends - requesting");
            HttpResponse response = client.execute(httpRequest);
            Log.v(TAG, "Fetching friends - responce received");

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));

            if (statusLine.getStatusCode() >= 300) {
//                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Gson gson = new Gson();


            users = (List<User>) gson.fromJson(resultString,
                    new TypeToken<List<User>>() {
                    }.getType());


            Log.v(TAG, String.format("Fetching parts - retrieved [%d] users", users.size()));

        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard or friend", e);
            return users;
        }

        Log.v(TAG, String.format("Fetching leader or friend - done"));


        return users;
    }

    public List<Running> getNewRunsForUsers(List<User> users) {

        List<Running> newRuns = new ArrayList<Running>();

        for (User user : users){
            if (user.get_id() == null){
                users.remove(user);
            }
        }


   //     {$or:[    {$and:[{'username':'user1','sharedId':1}]} , {$and:[{'username':'user2','sharedId':1}]}   ]}

        int size = users.size();

        if(size == 0){
            return newRuns;
        }

        String query = "{$or:[";

        Log.v("SIZE", size+"");
        Log.v("SIZE", users.toString());

        for (int i=0; i<size-1; i++) {

            query += "{ $and:[{ 'username': '" + users.get(i).getUsername() + "' ,";
            query += " 'sharedId': " + users.get(i).getSharedRunsNum() + "}]} ,";

        }
        query += "{ $and:[{ 'username': '" + users.get(size-1).getUsername() + "' ,";
        query += " 'sharedId': " + users.get(size-1).getSharedRunsNum() + "}]} ";

        query+= "]}";

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(running_collection)
                .appendQueryParameter("q", query)
                .appendQueryParameter("apiKey", apiKey)
                .build();


        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());


        try {

            HttpGet httpRequest = new HttpGet(uri.toString());


            setDefaultGetHeaders(httpRequest);

            HttpResponse response = client.execute(httpRequest);

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));

            if (statusLine.getStatusCode() >= 300) {
//                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Gson gson = new Gson();


            newRuns = (List<Running>) gson.fromJson(resultString,
                    new TypeToken<List<Running>>() {
                    }.getType());

            for (Running run : newRuns){
                run.setIntervals(fetchIntervalsForRun(run));
            }



        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard or friend", e);
            return newRuns;
        }
        return newRuns;
    }

    public List<Interval> fetchIntervalsForRun(Running run){//0 leaderboard

        List<Interval> intervals = new ArrayList<Interval>();

        String query;


        query = "{ $or: [";
            query += "{ 'running_mongo_id': '" + run.get_id().get$oid() + "'}";

        query += "] }";

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(interval_collection)
                .appendQueryParameter("q", query)
                .appendQueryParameter("apiKey", apiKey)
                .build();


        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());


        try {

            HttpGet httpRequest = new HttpGet(uri.toString());


            setDefaultGetHeaders(httpRequest);

            HttpResponse response = client.execute(httpRequest);

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));

            if (statusLine.getStatusCode() >= 300) {
//                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
            }

            String resultString = null;

            if (entity != null) {
                InputStream instream = entity.getContent();
                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    instream = new GZIPInputStream(instream);
                }
                resultString = Utils.convertStreamToString(instream);

                instream.close();
            }

            Log.v(TAG, String.format("Deserialising [%s]", resultString));

            Gson gson = new Gson();


            intervals = (List<Interval>) gson.fromJson(resultString,
                    new TypeToken<List<Interval>>() {
                    }.getType());

            for (Interval interval : intervals){
                interval.setInterval_id(-1);
            }




        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard or friend", e);
            return intervals;
        }

        Log.v(TAG, String.format("Fetching leader or friend - done"));


        return intervals;
    }

    private void setDefaultDeleteHeaders(HttpDelete httpRequest) throws UnsupportedEncodingException {
        httpRequest.setHeader("Accept", "application/json");
        httpRequest.setHeader("Content-type", "application/json");
    }

    private void setDefaultGetHeaders(HttpGet httpRequest) throws UnsupportedEncodingException {
        httpRequest.setHeader("Accept", "application/json");
        httpRequest.setHeader("Content-type", "application/json");
    }

    private void setDefaultPostHeaders(HttpPost httpPost) throws UnsupportedEncodingException {
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
    }

    private void setDefaultPutHeaders(HttpPut httpPut) throws UnsupportedEncodingException {
        httpPut.setHeader("Accept", "application/json");
        httpPut.setHeader("Content-type", "application/json");
    }

    private HttpParams getMyParams() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10 * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, 5 * 60 * 1000);
        return httpParams;
    }
}
