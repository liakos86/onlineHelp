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
import com.kostas.model.Database;
import com.kostas.onlineHelp.ActMain;
import com.kostas.onlineHelp.AppConstants;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.R;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

//todo sendme
public class SyncHelper {

    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName()
            .substring(0, 23);
    private ExtApplication application;
    String running_collection, interval_collection, runner_collection, authUrl, apiKey;
    private SharedPreferences app_preferences;

    public SyncHelper(ExtApplication application) {
        this.application = application;
        running_collection = application.getResources().getString(R.string.running_collection_mongo_path);
        interval_collection = application.getResources().getString(R.string.interval_collection_mongo_path);
        authUrl = application.getResources().getString(R.string.auth_url);
        apiKey = application.getResources().getString(R.string.apiKey);
        runner_collection = application.getResources().getString(R.string.runner_collection_mongo_path);
        app_preferences = application.getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String getResultStringFromResponse(HttpResponse response) throws IOException{
        HttpEntity entity = response.getEntity();
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
        return resultString;
    }

    private void setMyUser(User user2){
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
    }

    private <T> List<T> unmarshallingFromResultString(String result, boolean isList, T input){
        Gson gson = new Gson();
        return  gson.fromJson(result,
                new TypeToken<List<T>>() {
                }.getType());
    }

    private String getStringUri(String collectionPath, Map<String, String> queryParams){
        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme("https")
                .authority(authUrl)
                .path(collectionPath)
                .appendQueryParameter("apiKey", apiKey);
        for (Map.Entry<String, String> entry : queryParams.entrySet()){
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }
       return uriBuilder.build().toString();
    }

    public int insertMongoUser(String email, String username, String password) {
        Log.v(TAG, "Inserting user");
        int returnCode = -2;//server error default ?

        String uriString  = getStringUri(runner_collection, new HashMap<String, String>());

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {

            JSONObject runner = new JSONObject();
            runner.put("username", username);
            runner.put("password", password);
            runner.put("email", email);
            StringEntity se = new StringEntity(runner.toString());

            HttpPost httpPost = new HttpPost(uriString);
            httpPost.setEntity(se);
            setDefaultHttpHeaders(httpPost);
            HttpResponse response = client.execute(httpPost);

            String resultString = getResultStringFromResponse(response);

            Gson gson = new Gson();
            User user2 = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());

            if (user2 == null) {
                return 0; //no user found
            }

            setMyUser(user2);


            returnCode = 3;
        } catch (Exception e) {
            Log.e(TAG, "Exception inserting user", e);
            return -2;
        }
        Log.v(TAG, "Successfully inserted user");

        return returnCode;
    }

    public int getMongoUser(String email, String username, String password) {
        Log.v(TAG, "Fetching user");
        int returnCode = -2;//server error default ?
        String uri = null;

        if (username.length() > 0 || email.length() > 0) {//try get existing user by email or username

            String query = "{ 'email':'" + email + "' , 'password' :'" + password + "' }";
            if (username.length() > 0) {
                query = "{ 'username':'" + username + "' , 'password' :'" + password + "' }";
            }

            Map<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("q", query);
            queryParams.put("fo", "true");

            uri = getStringUri(runner_collection, queryParams);
        }

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            HttpGet httpRequest = new HttpGet(uri);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);

            String resultString = getResultStringFromResponse(response);

            Gson gson = new Gson();
            User user2 = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());

            if (user2 == null) {
                return -1; //no user found
            }
            setMyUser(user2);
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

        String uri = getStringUri(running_collection, new HashMap<String, String>());

//        HashMap  params = new HashMap();
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            User me = application.getMe();
            JSONObject workout = new JSONObject();
            workout.put("username", app_preferences.getString("username", AppConstants.EMPTY));
            workout.put("time", runToShare.getTime());
            workout.put("date", runToShare.getDate());
            workout.put("distance", runToShare.getDistance());
            workout.put("avgPaceText", runToShare.getAvgPaceText());
            workout.put("description", runToShare.getDescription());
            List<Running> runs = application.getRuns();
            int newSharedRunId = 1;
            for (Running run : runs){
                if (run.isShared()){
                  ++newSharedRunId;
                }
            }


            workout.put("sharedId", newSharedRunId);
            Log.v("SHAREDID", (newSharedRunId) + AppConstants.EMPTY);
            StringEntity se = new StringEntity(workout.toString());

            HttpPost httpPost = new HttpPost(uri);
            setDefaultHttpHeaders(httpPost);
            httpPost.setEntity(se);
            HttpResponse response = client.execute(httpPost);

            String resultString = getResultStringFromResponse(response);
            Gson gson = new Gson();

            Running run = (Running) gson.fromJson(resultString,
                    new TypeToken<Running>() {
                    }.getType());

            if (run == null || run.get_id() == null) {
                return 0;
            }

            updateMyMongoSharedRunsNum();

            for (Interval interval : runToShare.getIntervals()) {
                interval.setRunning_mongo_id(run.get_id().get$oid());
                int code = shareIntervalForRunMongo(interval);
                if (code == 0) {
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
        String uri = getStringUri(interval_collection, new HashMap<String, String>());
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            JSONObject intervalSession = new JSONObject();
            intervalSession.put("paceText", intervalToShare.getPaceText());
            intervalSession.put("milliseconds", intervalToShare.getMilliseconds());
            intervalSession.put("isFastest", intervalToShare.isFastest());
            intervalSession.put("distance", intervalToShare.getDistance());
            intervalSession.put("latLonList", intervalToShare.getLatLonList());
            intervalSession.put("running_mongo_id", intervalToShare.getRunning_mongo_id());
            StringEntity se = new StringEntity(intervalSession.toString());

            HttpPost httpPost = new HttpPost(uri);
            setDefaultHttpHeaders(httpPost);
            httpPost.setEntity(se);
            HttpResponse response = client.execute(httpPost);
            String resultString = getResultStringFromResponse(response);
            Gson gson = new Gson();

            Interval intervalSaved = gson.fromJson(resultString,
                    new TypeToken<Interval>() {
                    }.getType());

            if (intervalSaved == null) {
                return 0;
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception uploading interval", e);
            return 0;
        }

        Log.v(TAG, String.format("uploaded interval - done"));
        return 1;
    }

    public List<User> sentFriendRequest(String friend) {

        List<User> users = new ArrayList<User>();
        String query;

        if (friend == null || friend.equals(AppConstants.EMPTY)) {
            return users;
        } else {
            query = "{ 'username' : '" + friend + "'}";
        }

        Map<String, String>queryMap = new HashMap<String, String>();
        queryMap.put("q", query);
        queryMap.put("s", "{'totalScore': -1}");//sorting not used here
        String uri = getStringUri(runner_collection, queryMap);

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            HttpGet httpRequest = new HttpGet(uri);
            setDefaultHttpHeaders(httpRequest);

            HttpResponse response = client.execute(httpRequest);

            String resultString = getResultStringFromResponse(response);
            Gson gson = new Gson();
            users = gson.fromJson(resultString,
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
        return users;
    }

    public User getMongoUserByUsernameForFriend(User user) {// 1 send request, 0 accept, 2 reject, else just get user

        String myUsername = app_preferences.getString("username", AppConstants.EMPTY);
        String currentRequests = user.getFriendRequests() != null ? user.getFriendRequests() + " " : AppConstants.EMPTY;
        uploadNewFriendOrRequest(currentRequests + myUsername, user.getUsername());
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putString("sentRequests", app_preferences.getString("sentRequests", AppConstants.EMPTY) + user.getUsername());
        editor.apply();

        Log.v(TAG, String.format("Fetching user - done"));
        return user;
    }

    public boolean uploadNewFriendOrRequest(String friends, String username) {
        Log.v(TAG, "Uploading new friend");
        String query = "{'username': '" + username + "'}";

        Map<String, String>queryMap = new HashMap<String, String>();
        queryMap.put("q", query);
        String uri = getStringUri(runner_collection, queryMap);

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            JSONObject obj = new JSONObject();
            obj.put("friendRequests", friends);
            JSONObject lastObj = new JSONObject();
            lastObj.put("$set", obj);
            StringEntity se = new StringEntity(lastObj.toString());
            HttpPut httpRequest = new HttpPut(uri);
            httpRequest.setEntity(se);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);
            String resultString = getResultStringFromResponse(response);
            Log.v(TAG, String.format("Deserialising [%s]", resultString));
        } catch (Exception e) {
            Log.e(TAG, "Exception inserting friend", e);
            return false;
        }
        return true;
    }

    /**
     * refers to mongo
     */
    public void updateMyMongoSharedRunsNum() {
        String query = "{'username': '" + application.getMe().getUsername() + "'}";
        Map<String, String>queryMap = new HashMap<String, String>();
        queryMap.put("q", query);
        String uri = getStringUri(runner_collection, queryMap);
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            JSONObject obj = new JSONObject();
            obj.put("sharedRunsNum", application.getMe().getSharedRuns().size());
            JSONObject lastObj = new JSONObject();
            lastObj.put("$set", obj);
            StringEntity se = new StringEntity(lastObj.toString());
            HttpPut httpRequest = new HttpPut(uri);
            httpRequest.setEntity(se);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);
            String resultString = getResultStringFromResponse(response);
        } catch (Exception e) {
            Log.e(TAG, "Exception updating sharedRunsNum", e);
        }
    }

    public User acceptOrRejectFriend(String username, int type) {// 1 send request, 0 accept, 2 reject, else just get user
        String myUsername = app_preferences.getString("username", AppConstants.EMPTY);
        User user = null;

        Map<String, String>queryMap = new HashMap<String, String>();
        queryMap.put("q", "{ 'username':'" + username + "' }");
        queryMap.put("fo", "true");
        String uri = getStringUri(runner_collection, queryMap);
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            HttpGet httpRequest = new HttpGet(uri);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);
            String resultString = getResultStringFromResponse(response);
            Gson gson = new Gson();
            user = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());

            if (type == 0 || type == 2) {// refresh other user's requests
                //add friend to both users list
                if ((type == 0) && ((user.getFriends() == null || !user.getFriends().contains(myUsername)) && !app_preferences.getString("friends", AppConstants.EMPTY).contains(user.getUsername()))) {
                    fixFriendsListForUser(user.getFriends() + " " + myUsername, user.getUsername(), type);
                    fixFriendsListForUser(app_preferences.getString("friends", AppConstants.EMPTY) + " " + user.getUsername(), myUsername, type);
                    Database dbHelper = new Database(application);
                    dbHelper.addUser(user);
                }

                //remove his name
                String newFriendRequests = app_preferences.getString("friendRequests", AppConstants.EMPTY).replace(" " + user.getUsername() + " ", " ");
                newFriendRequests = newFriendRequests.replace(user.getUsername() + " ", " ");
                newFriendRequests = newFriendRequests.replace(" " + user.getUsername(), " ");
                newFriendRequests = newFriendRequests.replace(user.getUsername(), AppConstants.EMPTY);
                fixFriendsListForUser(newFriendRequests, myUsername, 1);

                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putString("friendRequests", newFriendRequests);
                if (type == 0) {
                    editor.putString("friends", app_preferences.getString("friends", AppConstants.EMPTY) + " " + user.getUsername());
                }
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception fetching user", e);
            return user;
        }
        return user;
    }

    public boolean fixFriendsListForUser(String friends, String username, int type) {
        String query = "{'username': '" + username + "'}";
        Map<String, String>queryMap = new HashMap<String, String>();
        queryMap.put("q", query);
        String uri = getStringUri(runner_collection, queryMap);
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            JSONObject obj = new JSONObject();
            if (type == 0)
                obj.put("friends", friends);
            else if (type == 1)
                obj.put("friendRequests", friends);

            JSONObject lastObj = new JSONObject();
            lastObj.put("$set", obj);
            StringEntity se = new StringEntity(lastObj.toString());
            HttpPut httpRequest = new HttpPut(uri);
            httpRequest.setEntity(se);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);
            String resultString = getResultStringFromResponse(response);
        } catch (Exception e) {
            Log.e(TAG, "Exception inserting friend", e);
            return false;
        }
        return true;
    }

    public List<User> getUsersWithRunsAndIntervalsByUsernameMongo(ArrayList<String> usernames) {

        List<User> users = new ArrayList<User>();
        if (usernames.size() == 0) {
            return users;
        }

        int size = usernames.size();
        String query = "{ $or: [";
        for (int i = 0; i < size - 1; i++) {
            query += "{ 'username': '" + usernames.get(i).trim() + "'},";
        }
        query += "{ 'username': '" + usernames.get(size - 1).trim() + "'}";
        query += "] }";

        Map<String, String>queryMap = new HashMap<String, String>();
        queryMap.put("q", query);
        // queryMap.put("s", "{'totalRuns': -1}");
        String uri = getStringUri(runner_collection, queryMap);

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            HttpGet httpRequest = new HttpGet(uri);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);
            String resultString = getResultStringFromResponse(response);
            Gson gson = new Gson();
            users = gson.fromJson(resultString,
                    new TypeToken<List<User>>() {
                    }.getType());


            for (User user: users){

                List<User> userlist = new ArrayList<User>();
                userlist.add(user);
                List<Running> newRuns = getRunsWithIntervalsForUsersMongo(userlist);
                user.setSharedRuns(newRuns);
            }


        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard or friend", e);
            return users;
        }
        return users;
    }

    public List<Running> getRunsWithIntervalsForUsersMongo(List<User> users) {
        List<Running> newRuns = new ArrayList<Running>();
        for (User user : users) {
            if (user.get_id() == null) {
                users.remove(user);
            }
        }

        int size = users.size();
        if (size == 0) {
            return newRuns;
        }

        String query = "{$or:[";
        for (int i = 0; i < size - 1; i++) {
            query += "{ $and:[{ 'username': '" + users.get(i).getUsername() + "' }]},";


//            query += "{ $and:[{ 'username': '" + users.get(i).getUsername() + "' ,";
//            query += " 'sharedId': " + users.get(i).getSharedRunsNum() + "}]} ,";
        }

        query += "{ $and:[{ 'username': '" + users.get(size - 1).getUsername() + "' }]}";

//        query += "{ $and:[{ 'username': '" + users.get(size - 1).getUsername() + "' ,";
//        query += " 'sharedId': " + users.get(size - 1).getSharedRunsNum() + "}]} ";


        query += "]}";//close or

        Map<String, String>queryMap = new HashMap<String, String>();
        queryMap.put("q", query);
        String uri = getStringUri(running_collection, queryMap);

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            HttpGet httpRequest = new HttpGet(uri);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);
            String resultString = getResultStringFromResponse(response);
            Gson gson = new Gson();
            newRuns = gson.fromJson(resultString,
                    new TypeToken<List<Running>>() {
                    }.getType());

            for (Running run : newRuns) {
                run.setIntervals(fetchIntervalsForMongoRun(run));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard or friend", e);
            return newRuns;
        }
        return newRuns;
    }

    public List<Interval> fetchIntervalsForMongoRun(Running run) {//0 leaderboard
        List<Interval> intervals = new ArrayList<Interval>();

        String query = "{ $or: [";
        query += "{ 'running_mongo_id': '" + run.get_id().get$oid() + "'}";
        query += "] }";
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("q", query);

        String uri = getStringUri(interval_collection, queryParams);
        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());

        try {
            HttpGet httpRequest = new HttpGet(uri);
            setDefaultHttpHeaders(httpRequest);
            HttpResponse response = client.execute(httpRequest);

            String resultString = getResultStringFromResponse(response);
            Gson gson = new Gson();
            intervals = gson.fromJson(resultString,
                    new TypeToken<List<Interval>>() {
                    }.getType());

            for (Interval interval : intervals) {
                interval.setInterval_id(-1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard or friend", e);
            return intervals;
        }
        return intervals;
    }

    private void setDefaultHttpHeaders(HttpRequest httpReq) throws UnsupportedEncodingException {
        httpReq.setHeader("Accept", "application/json");
        httpReq.setHeader("Content-type", "application/json");
    }

    private HttpParams getMyParams() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10 * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, 5 * 60 * 1000);
        return httpParams;
    }
}
