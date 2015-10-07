package com.kostas.onlineHelp;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class BaseFragmentActivity extends FragmentActivity {
    ActionBar actionBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getActionBar();
    }

//    protected AppContent getAppContent() {
//        if(null==appContent){
//            SharedPreferences sharedPreferences = getSharedPreferences("AppContent",MODE_PRIVATE);
//            String resultString = sharedPreferences.getString("data", "");
//            Gson gson = new Gson();
//
//            appContent = (AppContent) gson.fromJson(resultString,
//                    new TypeToken<AppContent>() {
//                    }.getType());
//
//            if (null==appContent){
//                appContent = new AppContent();
//            }
//        }
//        return appContent;
//    }

}