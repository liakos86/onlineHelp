package com.kostas.onlineHelp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.kostas.custom.ViewHolderRow;
import com.kostas.dbObjects.Running;
import com.kostas.service.RunningService;

import java.util.List;

public class RunningAdapterItem extends ArrayAdapter<Running> {

    int layoutResourceId;
    List<Running> data;
    Activity activity;
    boolean isMetricMiles;

    public RunningAdapterItem(Activity activity, int layoutResourceId,
                               List<Running> data, boolean isMetricMiles) {

        super(activity.getApplication(), layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.data = data;
        this.activity = activity;
        this.isMetricMiles = isMetricMiles;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolderRow holder =null;
        if (convertView == null || !(convertView.getTag() instanceof ViewHolderRow)) {
            convertView = activity.getLayoutInflater().inflate(R.layout.list_run_row, parent, false);
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

        final Running run = data.get(position);
        String[] paces = run.getAvgPaceText().split("-");
        if (isMetricMiles){

            holder.bottomText.setText(String.format("%1$,.2f", ((double) (run.getDistance() *0.000621371192))) + " miles with " + ((int) (run.getTime() / 1000)) + " secs rest");
            if (paces.length > 1) {
                holder.topRightText.setText("Avg Pace: "+paces[1]);
            }else{
                holder.topRightText.setText("n/a");
            }


        }else{
            holder.bottomText.setText((int) run.getDistance() + " meters with " + ((int) (run.getTime() / 1000)) + " secs rest");
            holder.topRightText.setText("Avg Pace: "+paces[0]);
        }

        holder.bottom2Text.setText( run.getDescription().length()>0? run.getDescription() : "No description" );

        holder.topText.setText(run.getDate());

        holder.rightText.setText(String.valueOf(run.getIntervals().size()) + " sessions");


        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent goToIntervals = new Intent(activity, ActViewIntervals.class);

                goToIntervals.putExtra("run", run.getRunning_id());
                goToIntervals.putExtra("myRun", false);
                activity.startActivity(goToIntervals);
            }
        });

//            convertView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    currentRun = child.get(childPosition);
//                    showIntervalsForRun(child.get(childPosition));
//                }
//            });
        return convertView;

    }
    private class runningViewHolder{
        TextView distance;
        TextView time;
        TextView fastest;
    }
}

