
package com.kostas.mongo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Running;
import com.kostas.dbObjects.User;
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
        Uri uri=null;


            uri = new Uri.Builder()
                    .scheme("https")
                    .authority(authUrl)
                    .path(runner_collection)
                    .appendQueryParameter("apiKey", apiKey)
                    .build();

        DefaultHttpClient client = application.getHttpClient();
        client.setParams(getMyParams());


        try{

            HttpResponse response;

           // if (username.length()>0&&email.length()>0) {//insert new
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

          //  }

            Log.v(TAG, "user acquisition- response received");

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

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
            User user2 = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());

            if (user2==null){
                return 0; //no user found
            }

            user2.setMongoId(user2.get_id().get$oid());//it comes with object, I unpack to store in db
            dbHelper.addUser(user2);

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

    /**
     * Refresh my friends list and my friend requests
     */
    public int getMyMongoUser() {

        Log.v(TAG, "Fetching my user");


             String   query =  "{ 'username':'"+app_preferences.getString("username","")+"' }";


         Uri   uri = new Uri.Builder()
                    .scheme("https")
                    .authority(authUrl)
                    .path(runner_collection)
                    .appendQueryParameter("q", query)
                    .appendQueryParameter("apiKey", apiKey)
                    .appendQueryParameter("fo", "true")
                    .build();

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
            User user2 = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());


            SharedPreferences.Editor editor = app_preferences.edit();

            if (user2 != null) {
                editor.putString("friends", user2.getFriends());
                editor.putString("friendRequests", user2.getFriendRequests());
                editor.apply();
            }else{
                editor.putString("mongoId", null);
                editor.putString("friends", null);
                editor.putString("friendRequests", null);
                editor.apply();
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception fetching user", e);
            return -1;
        }

        Log.v(TAG, String.format("Fetching existing user - done"));
        return 0;
    }


    //fixme commit
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


//             url =  runnerCollection+ "?q= { 'email':'"+email+"' , 'password' :'"+password+"' }&apiKey=" +apiKey;
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
            User user2 = (User) gson.fromJson(resultString,
                    new TypeToken<User>() {
                    }.getType());



            if (user2==null){
                return -1; //no user found
            }


            if (dbHelper.fetchUser(user2.getUsername()).getMongoId() == null) {
                user2.setMongoId(user2.get_id().get$oid());//it comes with object, I unpack to store in db
                dbHelper.addUser(user2);
            }


               SharedPreferences.Editor editor = app_preferences.edit();
               editor.putString("mongoId", user2.get_id().get$oid());
                editor.putString("username", user2.getUsername());
                editor.putLong("totalTime", user2.getTotalTime());
                editor.putFloat("totalDistance", user2.getTotalDistance());
                editor.putString("friends", user2.getFriends());
                editor.putString("friendRequests", user2.getFriendRequests());
                editor.apply();
               returnCode = 2;

            //else if (email==null&&user2!=null){
//                SharedPreferences.Editor editor = app_preferences.edit();
//                editor.putInt("totalScore", user2.getTotalScore());
//                editor.putString("username", user2.getUsername());
//                editor.putInt("totalChallenges", user2.getTotalChallenges());
//                editor.putLong("totalTime", user2.getTotalTime());
//                editor.putFloat("totalDistance", user2.getTotalDistance());
//                editor.putInt("wonChallenges", user2.getwonChallenges());
//                editor.putString("friends", user2.getFriends());
//                editor.putString("friendRequests", user2.getFriendRequests());
//                editor.commit();
//
//
//
//                returnCode = 0;
//            }


        } catch (Exception e) {
            Log.e(TAG, "Exception fetching user", e);
            return -2;
        }

        Log.v(TAG, String.format("Fetching existing user - done"));

        return returnCode;

    }

//    public List<Running> getMongoChallenges() {
//
//        Log.v(TAG, "Fetching challenges for me");
//
//        List<Running>challenges = new ArrayList<Running>();
//
//        List<Running>challengesExtClosed = new ArrayList<Running>();
//
//        List<Running>challengesDBClosed = new ArrayList<Running>();
//
//
//        String query = "  " +
//                "{   $or: [";
//
//        query += "{ 'user_name': '" + app_preferences.getString("username","") + "'},";
//
//        query += "{ 'opponent_name': '" + app_preferences.getString("username","") + "'}";
//
//        query += "]  } ";
////todo add status query
////        query += ", 'status': '0'     }   ";
//
//          Uri  uri = new Uri.Builder()
//                    .scheme("https")
//                    .authority(authUrl)
//                    .path(running_collection)
//                    .appendQueryParameter("q", query)
//                    .appendQueryParameter("apiKey", apiKey)
//                    .build();
//
//
//        DefaultHttpClient client = application.getHttpClient();
//        client.setParams(getMyParams());
//
//
//
//        try {
//
//            HttpResponse response;
//
//
//                HttpGet httpRequest = new HttpGet(uri.toString());
//                setDefaultGetHeaders(httpRequest);
//                Log.v(TAG, "Fetching challenges - requesting");
//                response = client.execute(httpRequest);
//
//
//
//            Log.v(TAG, "Fetching challenges - responce received");
//
//            HttpEntity entity = response.getEntity();
//
//            StatusLine statusLine = response.getStatusLine();
//
//            Log.v(TAG, String.format("Fetching challenges - status [%s]", statusLine));
//
//            if (statusLine.getStatusCode() >= 300) {
////                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
//                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
//            }
//
//            String resultString = null;
//
//            if (entity != null) {
//                InputStream instream = entity.getContent();
//                Header contentEncoding = response.getFirstHeader("Content-Encoding");
//                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
//                    instream = new GZIPInputStream(instream);
//                }
//                resultString = Utils.convertStreamToString(instream);
//
//                instream.close();
//            }
//
//            Log.v(TAG, String.format("Deserialising [%s]", resultString));
//
//            Gson gson = new Gson();
//            challenges = (List<Running>) gson.fromJson(resultString,
//                    new TypeToken<List<Running>>() {
//                    }.getType());
//
//            dbHelper.deleteAllOpenChallenges();
//
//
//
////            Log.v(TAG, String.format("Fetching parts - ready to insert [%d] parts", StoreList.size()));
//            int size = challenges.size();
//            for (int i = 0; i < size; i++) {
//
//
//                //if closed challenge and user has responded to it (not created!)
//                if ( challenges.get(i).getStatus()==1 ){
//                    challengesExtClosed.add(challenges.get(i));
//                    challenges.remove(i);
//                    --i;
//                    --size;
//                }else if ( challenges.get(i).getStatus()==0)  {
//                    challenges.get(i).setType(1);
//                    challenges.get(i).setRunning_id(-1);
//                    dbHelper.addRunning(challenges.get(i));
//                }
////                ll_rows++;
//            }
//
//
//            challengesDBClosed = dbHelper.fetchClosedRunsFromDb();
//            int sizeClosed = challengesDBClosed.size();
//
//            size = challengesExtClosed.size();
//            for (int j=0; j<size; j++){
//
//                boolean exists=false;
//
//                for (int k=0; k<sizeClosed; k++){
//
//                    if (challengesExtClosed.get(j).get_id().get$oid().equals(challengesDBClosed.get(k).getMongoId())){
//                        exists=true;
//                        continue;
//                    }
//
//                }
//
//                if (!exists) {
//
//                    challengesExtClosed.get(j).setMongoId(challengesExtClosed.get(j).get_id().get$oid());
//                    challengesExtClosed.get(j).setRunning_id(-1);
//                    challengesExtClosed.get(j).setType(1);
//                    challengesDBClosed.add(challengesExtClosed.get(j));
//                    dbHelper.addRunning(challengesExtClosed.get(j));
//                }
//
//
//
//            }
//
//
//
//            for (Running run:challengesDBClosed){
//                challenges.add(run);
//                if (run.getDeleted()!=1 && run.getWinner()!=null && (run.getUser_name().equals(app_preferences.getString("username","")))){
//                    deleteChallenge(run.getMongoId());
//                    dbHelper.setDeletedFlag(run.getMongoId());
//                }
//            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "Exception fetching challenges", e);
//            return challenges;
//        }
//
//        Log.v(TAG, String.format("Fetching challenges - done"));
//
//
//
//        return challenges;
//
//    }
//
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

            JSONObject workout = new JSONObject();
            workout.put("username", app_preferences.getString("username",""));
            workout.put("time", runToShare.getTime());
            workout.put("date", runToShare.getDate());
            workout.put("distance", runToShare.getDistance());
            workout.put("avgPaceText", runToShare.getAvgPaceText());
            workout.put("description", runToShare.getDescription());



            StringEntity se = new StringEntity( workout.toString());
            setDefaultPostHeaders(httpPost);
            httpPost.setEntity(se);

            Log.v(TAG, "uploading run - requesting");
            HttpResponse response = client.execute(httpPost);
            Log.v(TAG, "uploading run - response received");

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            Log.v(TAG, String.format(" status [%s]", statusLine));

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

            Gson gson = new Gson();


            Running run = (Running) gson.fromJson(resultString,
                    new TypeToken<Running>() {
                    }.getType());


            if (run == null || run.get_id() == null){
                return 0;
            }

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

//
//    public void updateTimeAndDistance(String username){
//
//
//        Log.v(TAG, "Saving time and dist");
//
//        String query = "{'username': '"+username+"'}";
//
//
//        Uri uri = new Uri.Builder()
//                .scheme("https")
//                .authority(authUrl)
//                .path(runner_collection)
//                .appendQueryParameter("q", query)
//                .appendQueryParameter("apiKey", apiKey)
//                .build();
//
//
//        DefaultHttpClient client = application.getHttpClient();
//        client.setParams(getMyParams());
//
//
//        try {
//            JSONObject obj = new JSONObject();
//
//
//            obj.put("totalDistance" , app_preferences.getFloat("totalDistance",0));
//
//            obj.put("totalTime" , app_preferences.getLong("totalTime", 0));
//
//
//            JSONObject lastObj = new JSONObject();
//            lastObj.put("$set", obj);
//
//            StringEntity se = new StringEntity(lastObj.toString());
//
//
//
//            HttpPut httpRequest = new HttpPut(uri.toString());
//
//            httpRequest.setEntity(se);
//
//
//            setDefaultPutHeaders(httpRequest);
//
//            Log.v(TAG, "setting user stats - requesting");
//            HttpResponse response = client.execute(httpRequest);
//            Log.v(TAG, "setting finished - responce received");
//
//            HttpEntity entity = response.getEntity();
//
//            StatusLine statusLine = response.getStatusLine();
//
//            Log.v(TAG, String.format("Setting time & dist - status [%s]", statusLine));
//
//            if (statusLine.getStatusCode() >= 300) {
////                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
//                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
//                return;
//            }
//
//            String resultString = null;
//
//            if (entity != null) {
//                InputStream instream = entity.getContent();
//                Header contentEncoding = response.getFirstHeader("Content-Encoding");
//                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
//                    instream = new GZIPInputStream(instream);
//                }
//                resultString = Utils.convertStreamToString(instream);
//
//                instream.close();
//            }
//
//            Log.v(TAG, String.format("Deserialising [%s]", resultString));
//
//
//
////            dbHelper.deleteAllStores();
//
//            Log.v(TAG, String.format("Uploaded user time and dist"));
//
//        } catch (Exception e) {
//            Log.e(TAG, "Exception uploading user stats", e);
//        }
//    }
//
    public List<User> sentFriendRequest(String friend){

        List<User> users = new ArrayList<User>();


            Log.v(TAG, "Fetching user "+friend);


            String[] friendsArray;
            String query;

            if (friend == null || friend.equals("")) {
                return users;
            } else {



                    query = "{ 'username' : '"+friend+"'}";



            }

//        https://api.mongolab.com/api/1/databases/auction/collections/runner?
//        q={ $or: [ { "email": "liak@liak.gr" }, { "email": "liak2@liak.gr" } ] }
//        &apiKey=fft3Q2J8bB2l-meOoBHZK_z3E_b5cuBz


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


                //add a new friend request to the other user
                if (users.size() == 1) {

                    getMongoUserByUsernameForFriend(users.get(0).getUsername());

                }
//            dbHelper.deleteAllStores();

                Log.v(TAG, String.format("Fetching parts - retrieved [%d] users", users.size()));

//            for (int i = 0; i < StoreList.size(); i++) {
//                dbHelper.addSite(StoreList.get(i));
//                ll_rows++;
//            }

           } catch (Exception e) {
                Log.e(TAG, "Exception fetching leaderboard or friend", e);
                return users;
            }

            Log.v(TAG, String.format("Fetching leader or friend - done"));

            return users;
    }

    public User getMongoUserByUsernameForFriend(String username) {// 1 send request, 0 accept, 2 reject, else just get user

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

            String currentRequests = user.getFriendRequests() != null ? user.getFriendRequests()+" " : "";
                uploadNewFriendOrRequest(currentRequests  + myUsername, user.getUsername());
                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putString("sentRequests",  app_preferences.getString("sentRequests","")+user.getUsername());
                editor.commit();
                //refresh other users friends


//            Log.v(TAG, String.format("Fetching parts - ready to insert 1 user", 1));

        } catch (Exception e) {
            Log.e(TAG, "Exception fetching user", e);
            return user;
        }

        Log.v(TAG, String.format("Fetching user - done"));



        return user;

    }
//
//
    public boolean uploadNewFriendOrRequest(String friends,String username){


        Log.v(TAG, "Uploading new friend");

        String query = "{'username': '"+username+"'}";


//        https://api.mongolab.com/api/1/databases/auction/collections/runner?
//        q={ $or: [ { "email": "liak@liak.gr" }, { "email": "liak2@liak.gr" } ] }
//        &apiKey=fft3Q2J8bB2l-meOoBHZK_z3E_b5cuBz


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



//            dbHelper.deleteAllStores();

            Log.v(TAG, String.format("Fetching parts - new friend added"));

//            for (int i = 0; i < StoreList.size(); i++) {
//                dbHelper.addSite(StoreList.get(i));
//                ll_rows++;
//            }

        } catch (Exception e) {
            Log.e(TAG, "Exception inserting friend", e);
            return false;
        }

        Log.v(TAG, String.format("uploaded friend - done"));
        return true;

    }

    public User acceptOrRejectFriend(String username, int type) {// 1 send request, 0 accept, 2 reject, else just get user

        Log.v(TAG, "Fetching user");

        String myUsername = app_preferences.getString("username", "");

        User user = null;

        Uri uri=null;

        uri = new Uri.Builder()
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
                editor.commit();

//                Toast.makeText(getApplication(), "Friend added!", Toast.LENGTH_LONG).show();



            }

//            Log.v(TAG, String.format("Fetching parts - ready to insert 1 user", 1));

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







//        https://api.mongolab.com/api/1/databases/auction/collections/runner?
//        q={ $or: [ { "email": "liak@liak.gr" }, { "email": "liak2@liak.gr" } ] }
//        &apiKey=fft3Q2J8bB2l-meOoBHZK_z3E_b5cuBz


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



//            dbHelper.deleteAllStores();

            Log.v(TAG, String.format("Fetching parts - new friend added"));

//            for (int i = 0; i < StoreList.size(); i++) {
//                dbHelper.addSite(StoreList.get(i));
//                ll_rows++;
//            }

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


//
//    public void replyToChallenge(String opponentName, boolean won, float distance) {
//
//        updateTimeAndDistance(app_preferences.getString("username",""));
//
//        Log.v(TAG, "replying to challenge");
//
////        Toast.makeText(getApplication(), opponentName+"  "+won, Toast.LENGTH_LONG).show();
//
//
//        String query = "{ $and: [";
//        query = "{ 'user_name': '" + opponentName + "' ,";
//        query += " 'opponent_name': '" + app_preferences.getString("username","") + "'}";
////        query += "] }";
//
//
//        Uri uri = new Uri.Builder()
//                .scheme("https")
//                .authority(authUrl)
//                .path(running_collection)
//                .appendQueryParameter("q", query)
//                .appendQueryParameter("apiKey", apiKey)
//                .build();
//
////        HashMap  params = new HashMap();
//        DefaultHttpClient client = application.getHttpClient();
//        client.setParams(getMyParams());
//        HttpPut httpPut = new HttpPut(uri.toString());
//
//
//        try {
//
//            JSONObject obj = new JSONObject();
//            obj.put("winner" , won?app_preferences.getString("username",""):opponentName);
//            obj.put("status" , 1);
//            JSONObject lastObj = new JSONObject();
//            lastObj.put("$set", obj);
//
//            StringEntity se = new StringEntity(lastObj.toString());
//
//
//            setDefaultPutHeaders(httpPut);
//            httpPut.setEntity(se);
//
//            Log.v(TAG, "Fetching runs - requesting");
//            HttpResponse response = client.execute(httpPut);
//            Log.v(TAG, "Fetching runs - responce received");
//
//            HttpEntity entity = response.getEntity();
//
//            StatusLine statusLine = response.getStatusLine();
//
//            Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));
//
//            if (statusLine.getStatusCode() >= 300) {
////                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
//                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
//            }
//
//            String resultString = null;
//
//            if (entity != null) {
//                InputStream instream = entity.getContent();
//                Header contentEncoding = response.getFirstHeader("Content-Encoding");
//                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
//                    instream = new GZIPInputStream(instream);
//                }
//                resultString = Utils.convertStreamToString(instream);
//
//
//                fixScoreAndChallenges(app_preferences.getString("username",""), won, distance);
//                fixScoreAndChallenges(opponentName, !won, distance);
//
//
//
//
//
//                instream.close();
//            }
//
////            Toast.makeText(getApplication(), "Challenged updated", Toast.LENGTH_LONG).show();
//
//
//        } catch (Exception e) {
//            Log.e(TAG, "Exception replying to challenge", e);
//        }
//
//        Log.v(TAG, String.format("Fetching stores - done"));
//
//
//    }
//
//    private void fixScoreAndChallenges(String username, boolean won, float distance){
//
//        User user = getMongoUserByUsernameForFriend(username, -1);
//
//
//        if (user!=null) {
//
//            user.setTotalChallenges(user.getTotalChallenges() + 1);
//            if (won) {
//                user.setWonChallenges(user.getwonChallenges() + 1);
//                user.setTotalScore(user.getTotalScore() + (int)Math.ceil(distance));
//            } else {
////                if (user.getTotalScore() - 500 > 0)
////                    user.setTotalScore(user.getTotalScore() - 500);
////                else
////                    user.setTotalScore(0);
//
//            }
//
//            setScoreAndChallenges(user);
//
//        }
//
//    }
//
//    private void setScoreAndChallenges(User user){
//
//        Log.v(TAG, "Saving new score");
//
//        String query = "{'username': '"+user.getUsername()+"'}";
//
//
//        Uri uri = new Uri.Builder()
//                .scheme("https")
//                .authority(authUrl)
//                .path(runner_collection)
//                .appendQueryParameter("q", query)
//                .appendQueryParameter("apiKey", apiKey)
//                .build();
//
//
//        DefaultHttpClient client = application.getHttpClient();
//        client.setParams(getMyParams());
//
//
//        try {
//            JSONObject obj = new JSONObject();
//
//
//            obj.put("totalScore" , user.getTotalScore());
//
//            obj.put("totalChallenges" , user.getTotalChallenges());
//
//            obj.put("wonChallenges" , user.getWonChallenges());
//
//            JSONObject lastObj = new JSONObject();
//            lastObj.put("$set", obj);
//
//            StringEntity se = new StringEntity(lastObj.toString());
//
//
//
//            HttpPut httpRequest = new HttpPut(uri.toString());
//
//            httpRequest.setEntity(se);
//
//
//            setDefaultPutHeaders(httpRequest);
//
//            Log.v(TAG, "setting user stats - requesting");
//            HttpResponse response = client.execute(httpRequest);
//            Log.v(TAG, "setting finished - responce received");
//
//            HttpEntity entity = response.getEntity();
//
//            StatusLine statusLine = response.getStatusLine();
//
//            Log.v(TAG, String.format("Fetching stores - status [%s]", statusLine));
//
//            if (statusLine.getStatusCode() >= 300) {
////                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
//                Log.e(TAG, statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
//                return;
//            }
//
//            String resultString = null;
//
//            if (entity != null) {
//                InputStream instream = entity.getContent();
//                Header contentEncoding = response.getFirstHeader("Content-Encoding");
//                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
//                    instream = new GZIPInputStream(instream);
//                }
//                resultString = Utils.convertStreamToString(instream);
//
//                instream.close();
//            }
//
//            Log.v(TAG, String.format("Deserialising [%s]", resultString));
//
//
//
////            dbHelper.deleteAllStores();
//
//            Log.v(TAG, String.format("Uploaded user score and challenges"));
//
//        } catch (Exception e) {
//            Log.e(TAG, "Exception uploading user stats", e);
//        }
//
//    }
//
//    public void deleteChallenge(String id){
//
//
////
////       String query =  "{_'id': '{ '$oid': '"+id+"'}";
////         query =  "{'description' : 'xaxa'}";
//
//        Uri uri = new Uri.Builder()
//                .scheme("https")
//                .authority(authUrl)
//                .path(running_collection+"/"+id)
//                .appendQueryParameter("apiKey", apiKey)
//                .build();
//
//
//        DefaultHttpClient client = application.getHttpClient();
//        client.setParams(getMyParams());
//
//        try{
//            HttpDelete httpDelete = new HttpDelete(uri.toString());
//            HttpResponse response;
//
//
//            setDefaultDeleteHeaders(httpDelete);
//            Log.v(TAG, "deleting run - requesting");
//            response = client.execute(httpDelete);
//
//
//
//            Log.v(TAG, "deleting run - responce received");
//
//            HttpEntity entity = response.getEntity();
//
//            StatusLine statusLine = response.getStatusLine();
//
//            Log.v(TAG, String.format("deleting run [%s]", statusLine));
//
//            if (statusLine.getStatusCode() >= 300) {
////                Toast.makeText(activity,R.string.server_error,Toast.LENGTH_LONG).show();
//                Log.e(TAG,statusLine.getStatusCode()+" - "+statusLine.getReasonPhrase());
//            }
//
//            String resultString = null;
//
//            if (entity != null) {
//                InputStream instream = entity.getContent();
//                Header contentEncoding = response.getFirstHeader("Content-Encoding");
//                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
//                    instream = new GZIPInputStream(instream);
//                }
//                resultString = Utils.convertStreamToString(instream);
//
//                instream.close();
//            }
//
//            Log.v(TAG, String.format("Deserialising [%s]", resultString));
//        }catch (Exception e) {
//            Log.e(TAG, "Exception deleting run", e);
//
//        }
//
//
//    }

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

    public ExtApplication getApplication() {
        return application;
    }

}
