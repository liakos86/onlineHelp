package com.kostas.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.kostas.custom.NumberPickerKostas;
import com.kostas.custom.ViewHolderRow;
import com.kostas.dbObjects.Plan;
import com.kostas.model.Database;
import com.kostas.mongo.SyncHelper;
import com.kostas.onlineHelp.ActMain;
import com.kostas.onlineHelp.ExtApplication;
import com.kostas.onlineHelp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liakos on 10/10/2015.
 */
public class FrgFriends extends Fragment {

    String username, password, email;

    EditText editUsername, editPassword, editEmail;

    TextView textLogin, textRegister, textForgot;

    Button buttonLogin, buttonRegister;

    int type = 0 ;//register


    SyncHelper sh;

    ViewFlipper friendsFlipper;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.frg_friends, container, false);


        setViewsAndListeners(v);

        sh = new SyncHelper(getActivity());

        setTitleActionBar();
        
        return  v;
    }




    private void setViewsAndListeners(View v){

        friendsFlipper = (ViewFlipper) v.findViewById(R.id.friends_flipper);

        buttonRegister = (Button) v.findViewById(R.id.buttonRegister);
        buttonLogin = (Button) v.findViewById(R.id.buttonLogin);
        textLogin = (TextView) v.findViewById(R.id.textLogin);
        textRegister = (TextView) v.findViewById(R.id.textRegister);
        textForgot = (TextView) v.findViewById(R.id.textForgot);

        editUsername = (EditText) v.findViewById(R.id.user_name);
        editPassword = (EditText) v.findViewById(R.id.user_pass);
        editEmail = (EditText) v.findViewById(R.id.user_email);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAndCallAsync();
            }
        });

        textLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //hide and show
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


    public final static boolean isValidEmail(CharSequence target) {
        if (target == null)
            return false;

        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();

    }


    private class insertOrGetUser extends AsyncTask<Void, Void, Integer> {
        private ExtApplication app;
        private int type;

        public insertOrGetUser(ExtApplication app, int type) {
            this.app = app;
            this.type = type;
        }

        protected void onPreExecute() {

        }

        @Override
        protected Integer doInBackground(Void... unused) {


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
                setTitleActionBar();
                friendsFlipper.setDisplayedChild(1);
            }else if (result==3){
                Toast.makeText(app, "User inserted", Toast.LENGTH_LONG).show();
                setTitleActionBar();
                friendsFlipper.setDisplayedChild(1);
            }

        }


    }

    public void setTitleActionBar(){
        SharedPreferences app_preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);

        if (app_preferences.getString("mongoId", null) != null){
            getActivity().setTitle(app_preferences.getString("username", null));
            friendsFlipper.setDisplayedChild(1);
        }

//        SharedPreferences.Editor editor = app_preferences.edit();
//        editor.remove("mongoId").apply();
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
