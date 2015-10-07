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
            convertView.setTag(holder);
        } else {
            holder = (intervalViewHolder) convertView.getTag();

        }


        if (data.get(position).isFastest()) {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.white_back));
            holder.distance.setTextColor(mContext.getResources().getColor(R.color.drawer_black));
            holder.time.setTextColor(mContext.getResources().getColor(R.color.drawer_black));

        }
        else {
            if (position % 2 == 0){
                convertView.setBackgroundColor(mContext.getResources().getColor(R.color.drawer_grey));

            }else{
                convertView.setBackgroundColor(mContext.getResources().getColor(R.color.drawer_black));

            }
            holder.distance.setTextColor(mContext.getResources().getColor(R.color.white_back));
            holder.time.setTextColor(mContext.getResources().getColor(R.color.white_back));

        }


        // object item based on the position
        final long intervalTime = data.get(position).getMilliseconds();


        holder.distance.setText((int)data.get(position).getDistance()+"m completed");

        int hours = (int)(intervalTime/3600000);
        int mins = (int)((intervalTime - (hours*3600000))/60000);
        int secs = (int)((intervalTime - (hours*3600000) - (mins*60000))/1000);

        String timeText = hours+"hr " + mins+"min "+secs+"sec";

        holder.time.setText(timeText+"   Speed avg: "+  String.format("%1$,.2f", ((float) ((data.get(position).getDistance()/intervalTime)  *3600)))+"km/h");

        return convertView;

    }

}

 class intervalViewHolder{
    TextView distance;
    TextView time;
}