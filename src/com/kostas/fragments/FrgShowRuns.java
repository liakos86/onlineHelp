package com.kostas.fragments;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.*;
import com.kostas.custom.ViewHolderRow;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Running;
import com.kostas.model.Database;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.kostas.mongo.SyncHelper;
import com.kostas.onlineHelp.*;
import com.kostas.custom.MapWrapperLayout;

import java.util.*;


/**
 * Created by liakos on 11/4/2015.
 */
public class FrgShowRuns extends Fragment {

    List <Running> runs = new ArrayList<Running>();



    /**
     * The months grouped
     */
    private ArrayList<String> parentItems = new ArrayList<String>();

    /**
     * The runs per month grouped
     */
    private ArrayList<Object> childItems = new ArrayList<Object>();
    private ExpandableListView runsExpListView;
    MyExpandableAdapter adapterExp;


    /**
     * Parent view containing every layout
     */
    ViewFlipper viewFlipper;




    Button buttonNewRun;



    TextView runsCount ;
    TextView intervalsCount;
    TextView metersCount;
    TextView durationCount ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.frg_show_runs, container, false);
        initializeViews(v);
        setList();
        return  v;
    }





    private void initializeViews(View v){


         runsCount = (TextView) v.findViewById(R.id.runsCount);
         intervalsCount =(TextView) v.findViewById(R.id.intervalsCount);
         metersCount =(TextView) v.findViewById(R.id.metersCount);
         durationCount =(TextView) v.findViewById(R.id.durationCount);
        viewFlipper = (ViewFlipper) v.findViewById(R.id.viewFlipperRuns);


        runsExpListView = (ExpandableListView) v.findViewById(R.id.listExpRunning);
        buttonNewRun = (Button) v.findViewById(R.id.buttonNewRun);


    }






    
    private void setList(){
        runs = ((ExtApplication)getActivity().getApplication()).getRuns();
        computeParentAndChildRuns();
        computeInfoTexts();

        runsExpListView.setDividerHeight(2);
        adapterExp = new MyExpandableAdapter(parentItems, childItems, getActivity());
        runsExpListView.setAdapter(adapterExp);

        runsExpListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            int previousItem = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                if (groupPosition != previousItem)
                    runsExpListView.collapseGroup(previousItem);
                previousItem = groupPosition;
            }
        });

        runsExpListView.expandGroup(0);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int right = (int) (getResources().getDisplayMetrics().widthPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));

        //placing the indicator 'right' pixels from the right of the view
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            runsExpListView.setIndicatorBounds(right - getResources().getDrawable(R.drawable.arrow_down).getIntrinsicWidth(), right);
        } else {
         runsExpListView.setIndicatorBoundsRelative(right - getResources().getDrawable(R.drawable.arrow_down).getIntrinsicWidth(), right);
        }


        buttonNewRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((BaseFrgActivityWithBottomButtons) getActivity()).startIntervalAct();
            }
        });
    }

    public void computeParentAndChildRuns(){

        if (runs.size()>0) {
            parentItems.clear();
            childItems.clear();
            for (Running running : runs) {
                String month = running.getDate().substring(3,10);
                if (!parentItems.contains(month)){
                    parentItems.add(month);
                }
            }

            for (String monthNumber : parentItems){
                ArrayList<Running> monthRuns = new ArrayList<Running>();
                for (Running running : runs) {
                    String month = running.getDate().substring(3,10);
                    if (month.equals(monthNumber)){
                        monthRuns.add(running);
                    }
                }
                childItems.add(monthRuns);
            }

            if (adapterExp!=null) {
                adapterExp.notifyDataSetChanged();
                runsExpListView.expandGroup(0);
            }
        }else{
            viewFlipper.setDisplayedChild(1);
        }
    }

    public void computeInfoTexts(){


        int runsNum=0, intervalsNum=0;
        float metersNum = 0f;
        long millisecsNum = 0l;

        for (Running run : runs){
            ++runsNum;
            for (Interval interval : run.getIntervals()){
                ++intervalsNum;
                metersNum+=interval.getDistance();
                millisecsNum+=interval.getMilliseconds();
            }

        }

        runsCount.setText("RUNS\r\n"+runsNum);
        intervalsCount.setText("INTERVALS\r\n"+intervalsNum);
        metersCount.setText("KM\r\n"+String.format("%1$,.1f",metersNum/1000));
        durationCount.setText(("HRS\r\n"+(int)(millisecsNum/3600000)));
    }

    private void confirmDelete(final Long trId,final int groupPosition, final int position){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        alertDialogBuilder
                .setMessage("Delete Running ?")
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteRunning(trId, groupPosition, position);
                    }

                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void deleteRunning(Long trId, int groupPosition, int position){
        Database db = new Database(getActivity().getBaseContext());
        db.deleteRunning(trId);
        List<Running> runnings =  ((ExtApplication)getActivity().getApplication()).getRuns();
        for(Running running : runnings){
            if (running.getRunning_id() == trId){
                runnings.remove(running);
                break;
            }
        }


        ((ArrayList<Running>)childItems.get(groupPosition)).remove(position);
        adapterExp.notifyDataSetChanged();
        computeInfoTexts();
        showTextNoRuns();
    }

    public void refreshAfterAdd(){
        runs = ((ExtApplication)getActivity().getApplication()).getRuns();

        viewFlipper.setDisplayedChild(0);

        computeParentAndChildRuns();
        adapterExp.notifyDataSetChanged();
        computeInfoTexts();
    }




    public class MyExpandableAdapter extends BaseExpandableListAdapter {
        private Activity activity;
        private ArrayList<Object> childtems;
        private LayoutInflater inflater;
        private ArrayList<String> parentItems;
        private ArrayList<Running>child;

        public MyExpandableAdapter(ArrayList<String> parents, ArrayList<Object> childern, Activity activity) {
            this.parentItems = parents;
            this.childtems = childern;
            this.activity = activity;
            this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private class headerViewHolder{
            TextView month;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            child = (ArrayList<Running>) childtems.get(groupPosition);
            ViewHolderRow holder =null;
            if (convertView == null || !(convertView.getTag() instanceof ViewHolderRow)) {
                convertView = inflater.inflate(R.layout.list_run_row, parent, false);
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

            Running run = child.get(childPosition);

            holder.bottomText.setText((int) run.getDistance() + " meters with " + ((int) (run.getTime() / 1000)) + " secs rest");
            holder.bottom2Text.setText( run.getDescription().length()>0? run.getDescription() : "No description" );

            holder.topText.setText(run.getDate());
            holder.topRightText.setText("Avg Pace: "+run.getAvgPaceText());
            holder.rightText.setText(String.valueOf(run.getIntervals().size()) + " sessions");

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        confirmDelete(child.get(childPosition).getRunning_id(), groupPosition, childPosition);
                        return false;
                    }
                });

            final Activity act = getActivity();
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent goToIntervals = new Intent(act, ActViewIntervals.class);

                        goToIntervals.putExtra("run", child.get(childPosition).getRunning_id());
                        goToIntervals.putExtra("myRun", true);
                        startActivity(goToIntervals);


//                        currentRun = child.get(childPosition);
//                        showIntervalsForRun(child.get(childPosition));
//                        if (currentRun.isShared()){
//                            shareFriendsButton.setClickable(false);
//                            shareFriendsButton.setText("Run already shared");
//                        }
                    }
                });
            return convertView;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

            headerViewHolder holder =null;
            if (convertView == null || !(convertView.getTag() instanceof headerViewHolder)) {
                convertView = inflater.inflate(R.layout.group_header, null);
                holder = new headerViewHolder();
                holder.month = (TextView) convertView.findViewById(R.id.textView1);
                convertView.setTag(holder);
            } else {
                holder = (headerViewHolder) convertView.getTag();
            }

            int month=1;
            try {
                 month = Integer.valueOf(parentItems.get(groupPosition).split("/")[0]);
            }catch (Exception e){
                e.printStackTrace();
            }
            String[] months = getResources().getStringArray(R.array.months);
            String monthName= months[month-1];
            holder.month.setText(monthName+" "+parentItems.get(groupPosition).split("/")[1] + " - " + ((ArrayList<Running>) childtems.get(groupPosition)).size() + " workouts");
            return convertView;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (childtems.size()>0) {
                return ((ArrayList<String>) childtems.get(groupPosition)).size();
            }
            return 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public int getGroupCount() {
            return parentItems.size();
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
            super.onGroupCollapsed(groupPosition);
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
            super.onGroupExpanded(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

    }

    private void showTextNoRuns(){
        boolean empty= true;
        for (Object child : childItems){
            if (((ArrayList)child).size()>0){
                empty = false;
                break;
            }
        }

        if (empty){
           viewFlipper.setDisplayedChild(1);
        }else{
            viewFlipper.setDisplayedChild(0);
        }
    }





    public static FrgShowRuns init(int val) {
        FrgShowRuns truitonList = new FrgShowRuns();
        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);
        return truitonList;
    }

}
