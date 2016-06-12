package com.kostas.onlineHelp;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import com.kostas.custom.NonSwipeableViewPager;
import com.kostas.fragments.FrgShowRuns;
import com.kostas.service.RunningService;

import java.util.Map;

/**
 * Created by liakos on 9/6/2016.
 */

    public class ActSplash extends BaseFrgActivityWithBottomButtons {



        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.act_splash);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!((ExtApplication)getApplication()).isRunsLoaded()){
                        try {
                            Thread.sleep(100);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    startMain();
                }
            }).start();

        }

    void startMain(){
        Intent intent = new Intent(this, ActMain.class);
        startActivity(intent);
        finish();
    }


    }

