package com.kostas.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.kostas.onlineHelp.R;
import org.w3c.dom.Text;

/**
 * Created by liakos on 10/10/2015.
 */
public class FrgSettings extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.frg_settings, container, false);

        TextView icons8text = ((TextView) v.findViewById(R.id.icons8text));
        icons8text.setText((getResources().getText(R.string.icons8)));
        icons8text.setMovementMethod(LinkMovementMethod.getInstance());

        Button b1=(Button)v.findViewById(R.id.shareButton);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "www.google.com");
                startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.shareTitle)));
            }
        });

        Button b2=(Button)v.findViewById(R.id.loginButton);


        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), getResources().getString(R.string.soon), Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }


    public static FrgSettings init(int val) {
        FrgSettings truitonList = new FrgSettings();
        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);
        return truitonList;

    }
}
