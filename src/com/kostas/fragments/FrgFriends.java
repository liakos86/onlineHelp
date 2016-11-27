package com.kostas.fragments;

import android.app.Activity;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.kostas.custom.ViewHolderRow;
import com.kostas.dbObjects.Running;
import com.kostas.dbObjects.User;
import com.kostas.model.Database;
import com.kostas.mongo.SyncHelper;
import com.kostas.onlineHelp.*;
import com.kostas.service.MongoUpdateService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liakos on 10/10/2015.
 */
public class FrgFriends extends Fragment {
    String username, password, email;
    EditText editUsername, editPassword, editEmail, friendName;
    TextView textLogin, textRegister, textForgot;
    Button buttonLogin, buttonRegister;
    int type = 0 ;//register
    ListView friendRequestsList;
    ListView friendsWithRunsList;
    ListView runsOfFriendList;
    ViewFlipper friendsFlipper;
    Button addFriend;
    TextView infoText;
    RequestsAdapterItem requestsAdapter;
    UsersAdapterItem friendsWithRunsAdapter;
    RunningAdapterItem friendRunsAdapter;
    List<User> friendsWithRuns = new ArrayList<User>();
    List<Running> runsOfFriend = new ArrayList<Running>();
    List<String> friendRequests = new ArrayList<String>();
    private BroadcastReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.frg_friends, container, false);
        getFriendRequestsFromSP();
        getFriendsWithRunsFromDB();
        setViewsAndListeners(v);
        setBroadcastReceiver();
        setCorrectFlipperChild();
        return  v;
    }

    /**
     * A receiver that listens for broadcast messages
     * If the message contains an interval_distance entry I update the current interval info
     * Else it means the interval is over and i prepare for the next one
     */
    private void setBroadcastReceiver() {
        if (receiver != null) {
            return;
        }
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    if (bundle.getBoolean(MongoUpdateService.NEW_FRIEND)){
                        /**
                         * Should i do smth?
                         */
                    }
                    else if (bundle.getBoolean(MongoUpdateService.NEW_FRIEND_REQUEST)) {

                        getFriendRequestsFromSP();
                        requestsAdapter.notifyDataSetChanged();
                        friendsFlipper.setDisplayedChild(2);

                    }
                    else if (bundle.getBoolean(MongoUpdateService.NEW_FRIEND_RUN_IN_DB)) {
                        getFriendsWithRunsFromDB();
                        friendsWithRunsAdapter.notifyDataSetChanged();
                    }
                }
            }
        };
    }


    private void setViewsAndListeners(View v){
        friendRequestsList = ((ListView) v.findViewById(R.id.listFriendRequests));
        friendsWithRunsList = ((ListView) v.findViewById(R.id.listFriendsWithRuns));
        runsOfFriendList = ((ListView) v.findViewById(R.id.listFriendRuns));
        infoText = (TextView) v.findViewById(R.id.infoText);
        friendsFlipper = (ViewFlipper) v.findViewById(R.id.friends_flipper);
        buttonRegister = (Button) v.findViewById(R.id.buttonRegister);
        addFriend = (Button)v.findViewById(R.id.buttonAddFriend);
        buttonLogin = (Button) v.findViewById(R.id.buttonLogin);
        textLogin = (TextView) v.findViewById(R.id.textLogin);
        textRegister = (TextView) v.findViewById(R.id.textRegister);
        textForgot = (TextView) v.findViewById(R.id.textForgot);
        editUsername = (EditText) v.findViewById(R.id.user_name);
        editPassword = (EditText) v.findViewById(R.id.user_pass);
        editEmail = (EditText) v.findViewById(R.id.user_email);
        friendName = (EditText)v.findViewById(R.id.editNewFriend);
        requestsAdapter = new RequestsAdapterItem(getActivity().getApplicationContext(), R.layout.list_plan_row, friendRequests);
        friendRequestsList.setAdapter(requestsAdapter);
        friendsWithRunsAdapter = new UsersAdapterItem(getActivity().getApplicationContext(), R.layout.list_run_row, friendsWithRuns);
        friendsWithRunsList.setAdapter(friendsWithRunsAdapter);
//        SharedPreferences app_preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
//        friendRunsAdapter = new RunningAdapterItem(getActivity(), R.layout.list_run_row, runsOfFriend, app_preferences.getBoolean(AppConstants.METRIC_MILES, false));
//        runsOfFriendList.setAdapter(friendRunsAdapter);
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAndCallAsync();
            }
        });
        textLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonRegister.setVisibility(View.GONE);
                buttonLogin.setVisibility(View.VISIBLE);
                textLogin.setVisibility(View.GONE);
                textRegister.setVisibility(View.VISIBLE);
                textForgot.setVisibility(View.VISIBLE);
                type = 1;//login
            }
        });

        textRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //hide and show
                buttonRegister.setVisibility(View.VISIBLE);
                buttonLogin.setVisibility(View.GONE);
                textLogin.setVisibility(View.VISIBLE);
                textRegister.setVisibility(View.GONE);
                textForgot.setVisibility(View.GONE);

                type = 0;//register
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAndCallAsync();
            }
        });
        addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchFriend();
            }
        });
    }

    void getFriendsWithRunsFromDB(){
        Database db = new Database(getActivity().getApplication());
        friendsWithRuns.clear();
        List<User> allFriends = db.fetchUsersFromDb();
        friendsWithRuns.addAll(allFriends);
    }

    void getFriendRequestsFromSP(){
        SharedPreferences app_preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        String friends = app_preferences.getString("friendRequests", AppConstants.EMPTY);
        Toast.makeText(getActivity(), "REQUESTS: "+friends, Toast.LENGTH_SHORT).show();
        friends = friends.replace("null ", AppConstants.EMPTY);
        String[]friendsArray  = friends.split(" ");
        //}
        friendRequests.clear();
        for (String friend : friendsArray){
            friendRequests.add(friend);
        }
        friendRequests.remove(AppConstants.EMPTY);
    }

    private void fetchFriend() {
        if (friendName.getText().length() > 0) {
            if (!alreadyFriend(friendName.getText().toString())) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(addFriend.getWindowToken(), 0);

                if (((ExtApplication)getActivity().getApplication()).isOnline()) {
                    new sentFriendRequestAsync(getActivity(), friendName.getText().toString()).execute();
                }else{
                    Toast.makeText(getActivity(), "Please connect to the internet", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(getActivity(), "Insert valid name", Toast.LENGTH_LONG).show();
        }
    }

    private boolean alreadyFriend(String friendName){

        SharedPreferences app_preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);

        String friends = app_preferences.getString("friends", null);
        String sentRequests = app_preferences.getString("sentRequests",null);
        if (friends != null && !friends.equals(AppConstants.EMPTY)) {
            String[] friendsList = friends.split(" ");
            for (String friendEmail : friendsList) {
                if (friendEmail.equals(friendName)) {
                    Toast.makeText(getActivity(), "Already a friend", Toast.LENGTH_LONG).show();
                    return true;
                }
            }

        }
        if (sentRequests != null && !sentRequests.equals(AppConstants.EMPTY)) {
            String[] sentList = sentRequests.split(" ");
            for (String friendEmail : sentList) {
                if (friendEmail.equals(friendName)) {
                    infoText.setText("Already sent request to " + friendEmail);
                    infoText.setTextColor(getResources().getColor(R.color.interval_red));
                    return true;
                }
            }

        }
        return false;
    }


    public void validateAndCallAsync(){

        username = editUsername.getText().toString().trim();
        password = editPassword.getText().toString().trim();
        email = editEmail.getText().toString().trim();


        if (type==0){//register

            if (username.length()==0||password.length()==0||email.length()==0){
                Toast.makeText(getActivity(), "Please fill in all fields!", Toast.LENGTH_LONG).show();
                return;
            }

        }else{//login
            if ((username.length()== 0 && email.length()== 0)||password.length()==0){
                Toast.makeText(getActivity(), "Please fill in username or email & password!", Toast.LENGTH_LONG).show();
                return;
            }


            if (username.length()> 0 && email.length()> 0){
                Toast.makeText(getActivity(), "Login only with username or email", Toast.LENGTH_LONG).show();
                return;
            }
        }


        if (email.length() > 0 && !isValidEmail(email)){
            Toast.makeText(getActivity(), "Please fill in a valid email!", Toast.LENGTH_LONG).show();
            return;
        }

        //if type==1 we get uer by shared prefs id
        if (((ExtApplication)getActivity().getApplication()).isOnline()) {

            new insertOrGetUser((ExtApplication)getActivity().getApplication(), type).execute();
        }
    }


    private boolean isValidEmail(CharSequence target) {
        return   ((target != null) &&  (android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()));
    }

    private void startMongoService(){
        if (!((ExtApplication)getActivity().getApplication()).isMyServiceRunning(MongoUpdateService.class)) {
            Intent intent = new Intent(getActivity().getBaseContext(), MongoUpdateService.class);
            getActivity().startService(intent);
            getActivity().registerReceiver(receiver, new IntentFilter(AppConstants.NOTIFICATION));
        }
    }


    private class insertOrGetUser extends AsyncTask<Void, Void, Integer> {
        private ExtApplication app;
        private int type;

        public insertOrGetUser(ExtApplication app, int type) {
            this.app = app;
            this.type = type;
        }

        protected void onPreExecute() {}

        @Override
        protected Integer doInBackground(Void... unused) {
            SyncHelper sh = new SyncHelper(app);
            if (type == 1)
                return sh.getMongoUser(email, username, password);
            else
                return sh.insertMongoUser(email, username, password);

        }

        @Override
        protected void onPostExecute(Integer result) {

//            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(getWindow().getWindowToken(), 0);

            if (result==0) {
                Toast.makeText(app, "Insert failed", Toast.LENGTH_LONG).show();
            }
            else if (result==-1){
                Toast.makeText(app, "Invalid credentials", Toast.LENGTH_LONG).show();
            }else if (result==-2){
                Toast.makeText(app, "Server Error", Toast.LENGTH_LONG).show();
            }else if (result==2){
                //((ActMainTest)getActivity()).getFirstLeaderboard();
                Toast.makeText(app, "User found", Toast.LENGTH_LONG).show();
//                setTitleActionBar();
                friendsFlipper.setDisplayedChild(1);
            }else if (result==3){
                Toast.makeText(app, "User inserted", Toast.LENGTH_LONG).show();
//                setTitleActionBar();
                friendsFlipper.setDisplayedChild(1);
            }

            startMongoService();
        }




    }

    private class sentFriendRequestAsync extends AsyncTask<Void, Void, List<User>> {
        private Activity activity;
        String friend;
        int type;

        public sentFriendRequestAsync(Activity activity, String friend) {
            this.activity = activity;
            this.friend = friend;
        }

        protected void onPreExecute() {
            addFriend.setClickable(false);
            infoText.setText(AppConstants.EMPTY);
        }

        @Override
        protected List<User> doInBackground(Void... unused) {

            SyncHelper sh = new SyncHelper((ExtApplication)activity.getApplication());


            return sh.sentFriendRequest(friend);


        }

        @Override
        protected void onPostExecute(List<User> users) {

            addFriend.setClickable(true);
            friendName.setText(AppConstants.EMPTY);


                if (users.size() == 1) {
                    infoText.setText("Friend request sent to " + friend + " !");
                    infoText.setTextColor(getResources().getColor(R.color.interval_green));
                }
                else if (users.size() == 0) {
                    infoText.setText("User does not exist!");
                    infoText.setTextColor(getResources().getColor(R.color.interval_red));
                }


        }

    }

    private class acceptOrRejectRequest extends AsyncTask<Void, Void, User> {
        private Activity activity;
        String friend;
        int type;


        public acceptOrRejectRequest(Activity activity, String friend, int type) {
            this.activity = activity;
            this.friend = friend;
            this.type = type;
        }

        protected void onPreExecute() {
            addFriend.setClickable(false);
            friendRequestsList.setClickable(false);
        }

        @Override
        protected User doInBackground(Void... unused) {

            SyncHelper sh = new SyncHelper(((ExtApplication) activity.getApplication()));

            return sh.acceptOrRejectFriend(friend, type);



        }

        @Override
        protected void onPostExecute(User user) {

            addFriend.setClickable(true);
            friendRequestsList.setClickable(true);

            friendRequests.clear();
            for (String fr : friendRequests){
                if (!fr.equals(friend)){
                    friendRequests.add(fr);
                }
            }


            requestsAdapter.notifyDataSetChanged();
            if (friendRequests.size() == 0){
                friendsFlipper.setDisplayedChild(1);
            }


        }


    }

    public void setCorrectFlipperChild(){
        SharedPreferences app_preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);

        if (app_preferences.getString("mongoId", null) != null){
            ((ExtApplication) getActivity().getApplication()).setMe(new User(app_preferences));
            friendsFlipper.setDisplayedChild(1);
            startMongoService();

            if (friendRequests.size() > 0){
                friendsFlipper.setDisplayedChild(2);
            }
        }else{
            friendsFlipper.setDisplayedChild(0);
            prepareForLogin();
        }

    }

    private void prepareForLogin(){
        Database db = new Database(getContext());
        db.deleteAllFriendRuns();
        db.deleteAllFriends();
    }

    public class RequestsAdapterItem extends ArrayAdapter<String> {

        Context mContext;
        int layoutResourceId;
        List<String> data;

        public RequestsAdapterItem(Context mContext, int layoutResourceId,
                                List<String> data) {

            super(mContext, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.mContext = mContext;
            this.data = data;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolderRequestRow holder =null;
            if (convertView == null || !(convertView.getTag() instanceof ViewHolderRow)) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_request_row, parent, false);

                holder = new ViewHolderRequestRow();


                holder.topText = (TextView) convertView
                        .findViewById(R.id.topText);

                holder.acceptButton = ((Button) convertView.findViewById(R.id.accept_request_button));
                holder.rejectButton = ((Button) convertView.findViewById(R.id.reject_request_button));

            } else {
                holder = (ViewHolderRequestRow) convertView.getTag();

            }

            final String friend = data.get(position);
            holder.topText.setText(friend+" wants to add you as a friend");

            holder.acceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new acceptOrRejectRequest(getActivity(), friend, 0).execute();
                }
            });

            holder.rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new acceptOrRejectRequest(getActivity(), friend, 2).execute();
                }
            });

            return convertView;

        }

    }

    public class UsersAdapterItem extends ArrayAdapter<User> {

        Context mContext;
        int layoutResourceId;
        List<User> data;
        LayoutInflater inflater;

        public UsersAdapterItem(Context mContext, int layoutResourceId,
                                List<User> data) {

            super(mContext, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.mContext = mContext;
            this.data = data;
            this.inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolderRow holder =null;
            if (convertView == null || !(convertView.getTag() instanceof ViewHolderRow)) {
                convertView = inflater.inflate(R.layout.list_run_row, parent, false);
                holder = new ViewHolderRow();
                holder.rightText = (TextView) convertView
                        .findViewById(R.id.rightText);
                holder.bottomText = (TextView) convertView
                        .findViewById(R.id.bottomText);
                holder.bottom2Text = (TextView) convertView
                        .findViewById(R.id.bottom2Text);
                holder.topText =  (TextView) convertView
                        .findViewById(R.id.topText);
                holder.topRightText =  (TextView) convertView
                        .findViewById(R.id.topRightText);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolderRow) convertView.getTag();
            }

            final User user = data.get(position);

            holder.topText.setText(user.getUsername());
            holder.bottomText.setText(String.valueOf(user.getSharedRunsNum())+" public runs");

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    SharedPreferences app_preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);

                    runsOfFriend = user.getSharedRuns();
                    friendRunsAdapter = new RunningAdapterItem(getActivity(), R.layout.list_run_row, runsOfFriend, app_preferences.getBoolean(AppConstants.METRIC_MILES, false));
                    runsOfFriendList.setAdapter(friendRunsAdapter);
                    friendsFlipper.setDisplayedChild(3);
                }
            });

            return convertView;

        }

    }

    private class ViewHolderRequestRow{
        TextView topText;
        Button acceptButton, rejectButton;
    }
    

    public static FrgFriends init(int val) {
        FrgFriends truitonList = new FrgFriends();

        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);
        return truitonList;
    }

}