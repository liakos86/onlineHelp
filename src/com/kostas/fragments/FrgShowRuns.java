package com.kostas.fragments;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.kostas.onlineHelp.*;
import com.kostas.custom.MapWrapperLayout;
import com.kostas.service.RunningService;

import java.util.*;


/**
 * Created by liakos on 11/4/2015.
 */
public class FrgShowRuns extends Fragment implements OnMapReadyCallback{

    List <Running> runs = new ArrayList<Running>();

    /**
     * All the intervals together
     */
    List<Interval> intervals = new ArrayList<Interval>();

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
    Running currentRun;
    List<Interval> currentIntervals;
    ArrayList<Marker> markers = new ArrayList<Marker>();
    ListView intervalListView;
    IntervalAdapterItem adapterInterval;

    /**
     * Parent view containing every layout
     */
    ViewFlipper viewFlipper;
    GoogleMap googleMap;
    MapWrapperLayout mapWrapperLayout;

    /**
     * The bounds of the visible map area
     */
    LatLngBounds bounds;

    /**
     * Close the map with the lines and return to intervals of run
     */
    ImageButton closeMapButton;

    /**
     * Open the map for the interval, close the current interval and go to runs list
     */
    Button openMapButton, closeIntervalsButton;

    Button buttonNewRun;

    /**
     * If the map is drawn we dont need to redraw
     */
    boolean alreadyDrawn;

    TextView runsCount ;
    TextView intervalsCount;
    TextView metersCount;
    TextView durationCount ;

    boolean isMetricMiles;

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
        initializeMap();
        return  v;
    }





    private void initializeViews(View v){

        runsCount = (TextView) v.findViewById(R.id.runsCount);
        intervalsCount =(TextView) v.findViewById(R.id.intervalsCount);
        metersCount =(TextView) v.findViewById(R.id.metersCount);
        durationCount =(TextView) v.findViewById(R.id.durationCount);
        viewFlipper = (ViewFlipper) v.findViewById(R.id.viewFlipperRuns);

        openMapButton = (Button) v.findViewById(R.id.buttonShowMap);
        closeMapButton = (ImageButton) v.findViewById(R.id.buttonCloseMap);
        closeIntervalsButton = (Button) v.findViewById(R.id.buttonCloseIntervals);
        runsExpListView = (ExpandableListView) v.findViewById(R.id.listExpRunning);
        intervalListView = (ListView) v.findViewById(R.id.listIntervals);
        buttonNewRun = (Button) v.findViewById(R.id.buttonNewRun);
    }

    public void initializeMap(){
        ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapListKostas)).getMapAsync(this);

    }

    private void showIntervalsForRun(Running running){
        intervals.clear();
        // DO NOT USE: intervals = running.getIntervals() !!!! IT WILL CHANGE THE OBJECT REFERENCED
        for (Interval interval : running.getIntervals()){
            intervals.add(interval);
        }
        viewFlipper.setDisplayedChild(1);
        adapterInterval.notifyDataSetChanged();
        if (googleMap!=null) {
            drawMap();
        }else{
            Toast.makeText(getActivity(), "Google maps not present...", Toast.LENGTH_SHORT).show();
        }
    }

    private void setList(){
        intervalListView.setDivider(null);
        runs = ((ExtApplication)getActivity().getApplication()).getRuns();
        computeParentAndChildRuns();
        SharedPreferences preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        isMetricMiles = preferences.getBoolean(RunningService.METRIC_MILES, false);
        computeInfoTexts();
        createExpandableList();
        setButtonListeners();
    }

    public void createExpandableList(){
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


        adapterInterval = new IntervalAdapterItem(getActivity(), getActivity().getApplicationContext(),
                R.layout.list_interval_row, intervals, isMetricMiles);
        intervalListView.setAdapter(adapterInterval);
    }

    public void setButtonListeners(){
        closeIntervalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleMap != null) {
                    googleMap.clear();
                    alreadyDrawn = false;
                }
                viewFlipper.setDisplayedChild(0);
            }
        });

        closeMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.setDisplayedChild(1);
            }
        });

        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.setDisplayedChild(2);
            }
        });

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
            viewFlipper.setDisplayedChild(3);
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
        if (isMetricMiles){
            metersCount.setText("MI\r\n" + String.format("%1$,.1f", metersNum / RunningService.MILE_TO_METERS_CONST));
        }else {
            metersCount.setText("KM\r\n" + String.format("%1$,.1f", metersNum / 1000));
        }
        durationCount.setText(("HRS\r\n"+(int)(millisecsNum/3600000)));
    }

    public void drawMap(){
        alreadyDrawn = true;
        currentIntervals = currentRun.getIntervals();
        markers.clear();
        List<LatLng> locationList = new ArrayList<LatLng>();
        double northPoint=-85.05115 , southPoint=85.05115 , eastPoint=-180, westPoint=180;
        LatLng top = new LatLng(0,0), bottom=new LatLng(0,0), left=new LatLng(0,0), right=new LatLng(0,0);
        int number = currentIntervals.size();
        double latPoint, lonPoint;

        for (int i=0; i<number; i++) {//for each interval
            Interval current = currentIntervals.get(i);
            int color = i % 2 == 0 ? getResources().getColor(R.color.interval_red) : getResources().getColor(R.color.interval_green);
            locationList.clear();
            String[] latStringList = current.getLatLonList().split(",");
            int listLength = latStringList.length - 1;

            if (latStringList[0].equals("null")) {
                Toast.makeText(getActivity(),(i+1)+" : "+ current.getLatLonList(), Toast.LENGTH_SHORT).show();
            } else {
                for (int j = 0; j < listLength; j += 2) {
                    latPoint = Double.valueOf(latStringList[j]);
                    lonPoint = Double.parseDouble(latStringList[j + 1]);

                    if (latPoint > northPoint) {
                        northPoint = latPoint;
                        top = new LatLng(latPoint, lonPoint);
                    }
                    if (latPoint < southPoint) {
                        southPoint = latPoint;
                        bottom = new LatLng(latPoint, lonPoint);
                    }

                    if (lonPoint > eastPoint) {
                        eastPoint = lonPoint;
                        right = new LatLng(latPoint, lonPoint);
                    }
                    if (lonPoint < westPoint) {
                        westPoint = lonPoint;
                        left = new LatLng(latPoint, lonPoint);
                    }
                    locationList.add(new LatLng(latPoint, lonPoint));
                }
                int currSize = locationList.size() - 1;
                for (int k = 0; k < currSize; k++) {
                    googleMap.addPolyline(new PolylineOptions().add(locationList.get(k), locationList.get(k + 1)).width(7).color(color));
                }

                if (currSize > 0) {
                    markers.add(googleMap.addMarker(new MarkerOptions()
                                            .position(locationList.get(0))
                                            .title(String.valueOf(current.getInterval_id()))
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_start2)

                                            )
                            )
                    );

                    googleMap.addMarker(new MarkerOptions()
                                    .position(locationList.get(currSize))
                                    .snippet("Speed: " + String.format("%1$,.2f", ((double) ((current.getDistance() / current.getMilliseconds()) * 3600))) + " km/h")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_stop2))
                    );
                }
            }

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(top);
            builder.include(bottom);
            builder.include(left);
            builder.include(right);
            bounds = builder.build();
            try {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    @Override
    public void onMapReady(GoogleMap gMap) {
        googleMap = gMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.setIndoorEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
            }
        });

        mapWrapperLayout = (MapWrapperLayout) getView().findViewById(R.id.mapWrapperRuns);
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });
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
            viewFlipper.setDisplayedChild(3);
        }else{
            viewFlipper.setDisplayedChild(0);
        }
    }


    public void refreshPaceTextsIfNeeded(){
        SharedPreferences preferences = getActivity().getSharedPreferences(ActMain.PREFS_NAME, Context.MODE_PRIVATE);
        boolean newIsMetricMiles = preferences.getBoolean(RunningService.METRIC_MILES, false);
        if (newIsMetricMiles != isMetricMiles){
            isMetricMiles = newIsMetricMiles;
            createExpandableList();
            computeInfoTexts();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
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
