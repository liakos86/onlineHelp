package com.kostas.onlineHelp;


import android.content.Intent;
import android.os.Bundle;


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

