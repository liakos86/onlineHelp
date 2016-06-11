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
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.secondary_grey));
            holder.fastest.setVisibility(View.VISIBLE);
        }
        else {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.primary_grey));

            holder.fastest.setVisibility(View.GONE);

        }

        Interval current = data.get(position);


        // object item based on the position
        final long intervalTime = current.getMilliseconds();

        int hours = (int)(intervalTime/3600000);
        int mins = (int)((intervalTime - (hours*3600000))/60000);
        int secs = (int)((intervalTime - (hours*3600000) - (mins*60000))/1000);

        String timeText = String.format("%02d",hours)+":" + String.format("%02d",mins)+":"+String.format("%02d",secs);
        holder.distance.setText((int)current.getDistance()+"m completed  in  "+timeText);


        holder.time.setText(
                "Speed: "+  String.format("%1$,.2f", ((double) ((current.getDistance()/intervalTime)  *3600)))+"km/h " +
                "  Pace: "+current.getPaceText());

        return convertView;

    }
   private class intervalViewHolder{
        TextView distance;
        TextView time;
        TextView fastest;
    }
}

