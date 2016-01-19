package com.kostas.onlineHelp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/**
 * Created by liakos on 10/10/2015.
 */
public class FrgSettings extends BaseFragment{


    EditText speechTextStart, speechTextFinish;
    CheckBox sound, vibration;
    TextView errorText;
    Button saveSettings;
    SharedPreferences app_preferences;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.frg_settings, container, false);

        setViews(v);



        return  v;
    }

    public void setViews(View v){
        speechTextStart = (EditText) v.findViewById(R.id.speechEditTextStart);
        speechTextFinish = (EditText) v.findViewById(R.id.speechEditTextFinish);
        saveSettings = (Button) v.findViewById(R.id.saveSettingsButton);
        errorText = (TextView) v.findViewById(R.id.settingsError);
        app_preferences = getActivity().getSharedPreferences(IntervalActivity.PREFS_NAME, Context.MODE_PRIVATE);

        sound = (CheckBox) v.findViewById(R.id.checkbox_sound);
        vibration = (CheckBox) v.findViewById(R.id.checkbox_vibration);

        sound.setChecked(!app_preferences.getBoolean("noSound", false));
        vibration.setChecked(!app_preferences.getBoolean("noVibration", false));

        speechTextStart.setText(app_preferences.getString("speechStart", ""));
        speechTextFinish.setText(app_preferences.getString("speechFinish", ""));

        saveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(saveSettings.getWindowToken(), 0);

                String start = speechTextStart.getText().toString().trim();
                String finish = speechTextFinish.getText().toString().trim();
                Boolean soundOn = sound.isChecked();
                Boolean vibrationOn = vibration.isChecked();


                if (finish.length()<21 && start.length()<21){
                    SharedPreferences.Editor editor =app_preferences.edit();
                    editor.putString("speechStart", start);
                    editor.putString("speechFinish", finish);
                    editor.putBoolean("noSound", !soundOn);
                    editor.putBoolean("noVibration", !vibrationOn);
                    editor.apply();
                    Toast.makeText(getActivity(), "Saved", Toast.LENGTH_SHORT).show();
                    errorText.setVisibility(View.INVISIBLE);
                }else{
                    errorText.setVisibility(View.VISIBLE);
                }
            }
        });

    }


    static FrgSettings init(int val) {
        FrgSettings truitonList = new FrgSettings();

        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);
        return truitonList;
    }

}
