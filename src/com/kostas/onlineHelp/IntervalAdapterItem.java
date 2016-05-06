package com.kostas.onlineHelp;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.kostas.dbObjects.Interval;

import java.util.List;

public class IntervalAdapterItem extends ArrayAdapter<Interval> {

    Context mContext;
    int layoutResourceId;
    List<Interval> data;
    Activity activity;

    public IntervalAdapterItem(Activity activity, Context mContext, int layoutResourceId,
                               List<Interval> data) {

        super(mContext, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;
        this.activity = activity;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        intervalViewHolder holder =null;
        if (convertView == null || !(convertView.getTag() instanceof intervalViewHolder)) {
            convertView = activity.getLayoutInflater().inflate(R.layout.list_interval_row, parent, false);

            holder = new intervalViewHolder();

            holder.distance = (TextView) convertView
                    .findViewById(R.id.distanceIntervalText);
            holder.time =  (TextView) convertView
                    .findViewById(R.id.timeIntervalText);
            holder.fastest = (TextView) convertView
                    .findViewById(R.id.fastestIntervalText);
            convertView.setTag(holder);
        } else {
            holder = (intervalViewHolder) convertView.getTag();

        }


        if (data.get(position).isFastest()) {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.white_back));
            holder.distance.setTextColor(mContext.getResources().getColor(R.color.primary_grey));
            holder.time.setTextColor(mContext.getResources().getColor(R.color.primary_grey));
            holder.fastest.setVisibility(View.VISIBLE);

        }
        else {
            if (position % 2 == 0){
                convertView.setBackgroundColor(mContext.getResources().getColor(R.color.secondary_grey));

            }else{
                convertView.setBackgroundColor(mContext.getResources().getColor(R.color.primary_grey));

            }
            holder.distance.setTextColor(mContext.getResources().getColor(R.color.white_back));
            holder.time.setTextColor(mContext.getResources().getColor(R.color.white_back));
            holder.fastest.setVisibility(View.GONE);

        }


        // object item based on the position
        final long intervalTime = data.get(position).getMilliseconds();

        int hours = (int)(intervalTime/3600000);
        int mins = (int)((intervalTime - (hours*3600000))/60000);
        int secs = (int)((intervalTime - (hours*3600000) - (mins*60000))/1000);

        String timeText = String.format("%02d",hours)+":" + String.format("%02d",mins)+":"+String.format("%02d",secs);
        holder.distance.setText((int)data.get(position).getDistance()+"m completed  in  "+timeText);

        float pace =  intervalTime / data.get(position).getDistance();

        int paceMinutes = (int)(pace/60);
        int paceSeconds = (int)(pace - (paceMinutes*60));

        String paceText = paceMinutes<60 ?   String.format("%02d", paceMinutes)+"m "+String.format("%02d", paceSeconds)+"s" : "over 1 hour";

        holder.time.setText(
                "  Speed: "+  String.format("%1$,.2f", ((double) ((data.get(position).getDistance()/intervalTime)  *3600)))+"km/h " +
                "  Pace: "+paceText);

        return convertView;

    }
   private class intervalViewHolder{
        TextView distance;
        TextView time;
        TextView fastest;
    }
}

