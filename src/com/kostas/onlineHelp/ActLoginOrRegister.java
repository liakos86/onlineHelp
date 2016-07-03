package com.kostas.onlineHelp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.kostas.mongo.SyncHelper;

/**
 * Created by liakos on 3/7/2016.
 */
public class ActLoginOrRegister extends BaseFrgActivityWithBottomButtons {

    String username, password, email;

    EditText editUsername, editPassword, editEmail;

    TextView textLogin, textRegister, textForgot;

    Button buttonLogin, buttonRegister;

    int type = 0 ;//register


    SyncHelper sh;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_login_register);

        setViewsAndListeners();

        sh = new SyncHelper(this);

    }


    private void setViewsAndListeners(){


        buttonRegister = (Button) findViewById(R.id.buttonRegister);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        textLogin = (TextView) findViewById(R.id.textLogin);
        textRegister = (TextView) findViewById(R.id.textRegister);
        textForgot = (TextView) findViewById(R.id.textForgot);

        editUsername = (EditText) findViewById(R.id.user_name);
        editPassword = (EditText) findViewById(R.id.user_pass);
        editEmail = (EditText) findViewById(R.id.user_email);

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
                Toast.makeText(this, "Please fill in all fields!", Toast.LENGTH_LONG).show();
                return;
            }

        }else{//login
            if ((username.length()== 0 && email.length()== 0)||password.length()==0){
                Toast.makeText(this, "Please fill in username or email & password!", Toast.LENGTH_LONG).show();
                return;
            }


            if (username.length()> 0 && email.length()> 0){
                Toast.makeText(this, "Login only with username or email", Toast.LENGTH_LONG).show();
                return;
            }
        }


        if (email.length() > 0 && !isValidEmail(email)){
            Toast.makeText(this, "Please fill in a valid email!", Toast.LENGTH_LONG).show();
            return;
        }

        //if type==1 we get uer by shared prefs id
        if (((ExtApplication)getApplication()).isOnline()) {

            new insertOrGetUser((ExtApplication)getApplication(), type).execute();
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
            }else if (result==3){
              Toast.makeText(app, "User inserted", Toast.LENGTH_LONG).show();

          }

        }


    }



}
