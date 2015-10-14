package com.kostas.onlineHelp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.*;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Running;
import com.kostas.model.Database;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.*;


/**
 * Created by liakos on 11/4/2015.
 */
public class FrgShowRuns extends BaseFragment {

    List<Running> runs;
    List<Interval> intervals;

    private ArrayList<String> parentItems = new ArrayList<String>();
    private String[]parents;
    private ArrayList<Object> childItems = new ArrayList<Object>();

    private ExpandableListView runsExpListView;
//    private ExpandableListAdapter adapterExp;

    MyExpandableAdapter adapterExp;

    TextView noRunsText;



//    List<Polyline> mapLines;

    Marker markerStart, markerFinish;

    Running currentRun;

    ListView runningListView, intervalListView;
//    RunningAdapterItem adapterRunning;
    IntervalAdapterItem adapterInterval;

    ViewFlipper viewFlipper;
    GoogleMap googleMap;
    LatLngBounds bounds;
    ImageButton closeMapButton;
    RelativeLayout openMapButton, closeIntervalsButton;

    //if he goes back and forth in map <-> intervals
    boolean alreadyDrawn;

    LatLng zoomPoint;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frg_show_runs, container, false);

        viewFlipper = (ViewFlipper) v.findViewById(R.id.viewFlipper);

        openMapButton = (RelativeLayout) v.findViewById(R.id.buttonShowMap);
        closeMapButton = (ImageButton) v.findViewById(R.id.buttonCloseMap);
        closeIntervalsButton = (RelativeLayout) v.findViewById(R.id.buttonCloseIntervals);

        noRunsText = (TextView) v.findViewById(R.id.noRunsText);

        setList(v);
        initializeMap();


        return  v;
    }

    public void initializeMap(){
        SupportMapFragment fm = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapListKostas);
        googleMap = fm.getMap();
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.setIndoorEnabled(false);



        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 60));
            }
        });

//        mapLines = new ArrayList<Polyline>();

//        viewFlipper.setDisplayedChild(2);
//        viewFlipper.setDisplayedChild(0);
    }

    private void showIntervalsForRun(Running running){

        intervals.clear();

        // DO NOT USE: intervals = running.getIntervals() !!!! IT WILL CHANGE THE OBJECT REFERENCED
        for (Interval interval : running.getIntervals()){
            interval.setDistance(running.getDistance());
            intervals.add(interval);
        }
        viewFlipper.setDisplayedChild(1);

        adapterInterval.notifyDataSetChanged();

        drawMap();



    }
    
    private void setList(View v){

//        runs = new ArrayList<Running>();
        intervals = new ArrayList<Interval>();

        runsExpListView = (ExpandableListView) v.findViewById(R.id.listExpRunning);

//        runningListView = (ListView) v.findViewById(R.id.listRunning);
//        runningListView.setDivider(null);
        intervalListView = (ListView) v.findViewById(R.id.listIntervals);
        intervalListView.setDivider(null);

//        adapterRunning = new RunningAdapterItem(getActivity().getApplicationContext(),
//                R.layout.list_running_row, runs);
//        runningListView.setAdapter(adapterRunning);

        getRunsFromDb();
        runsExpListView.setDividerHeight(2);
        runsExpListView.setClickable(true);
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

        int right = (int) (getResources().getDisplayMetrics().widthPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));


        //placing the indicator 'right' pixels from the right of the view
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            runsExpListView.setIndicatorBounds(right - getResources().getDrawable(R.drawable.arrow_down).getIntrinsicWidth(), right);
        } else {
            runsExpListView.setIndicatorBoundsRelative(right - getResources().getDrawable(R.drawable.arrow_down).getIntrinsicWidth(), right);
        }


        adapterInterval = new IntervalAdapterItem(getActivity(), getActivity().getApplicationContext(),
                R.layout.list_interval_row, intervals);
        intervalListView.setAdapter(adapterInterval);

        closeIntervalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleMap != null) {
                    googleMap.clear();
//                    mapLines.clear();
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

//                if (!alreadyDrawn)
//                    drawMap();
//                else if (zoomPoint!=null)
//                    zoomMap();

                viewFlipper.setDisplayedChild(2);
            }
        });
        
    }

    public void getRunsFromDb(){
        Database db = new Database(getActivity());
        List <Running> newRuns = db.fetchRunsFromDb();

        Collections.reverse(newRuns);
        if (newRuns.size()>0) {
//            runs.clear();
            parentItems.clear();
            childItems.clear();
            for (Running running : newRuns) {

                List<Interval>intervalsList =db.fetchIntervalsForRun(running.getRunning_id());

                int fastest=0, rounds=intervalsList.size();
                long millis = Long.MAX_VALUE;
                for (int i=0; i<rounds; i++){
                    if (intervalsList.get(i).getMilliseconds()< millis) {
                        fastest=i;
                        millis = intervalsList.get(i).getMilliseconds();
                    }
                }

                intervalsList.get(fastest).setFastest(true);

                running.setIntervals(intervalsList);
//                runs.add(running);

                String month = running.getDate().substring(3,10);

                if (!parentItems.contains(month)){
                    parentItems.add(month);


                }

            }


            for (String monthNumber : parentItems){
                ArrayList<Running> monthRuns = new ArrayList<Running>();
                for (Running running : newRuns) {
                    String month = running.getDate().substring(3,10);
                    if (month.equals(monthNumber)){
                        monthRuns.add(running);
                    }
                }
                childItems.add(monthRuns);
            }

            if (adapterExp!=null) adapterExp.notifyDataSetChanged();
//            adapterRunning.notifyDataSetChanged();

        }
    }


    public void drawMap(){


        alreadyDrawn = true;

        List<Interval> currentIntervals = currentRun.getIntervals();

        //merging the list of the points of the intervals
//        String latLonList = currentIntervals.get(0).getLatLonList();
//        int number = currentIntervals.size();
//        for (int i=1; i<number; i++){
//            latLonList +=","+ currentIntervals.get(i).getLatLonList();
//        }


//        String[] pointsList = latLonList.split(",");

        List<LatLng> locationList = new ArrayList<LatLng>();

        double northPoint=-85.05115 , southPoint=85.05115 , eastPoint=-180, westPoint=180;
        LatLng top = new LatLng(0,0), bottom=new LatLng(0,0), left=new LatLng(0,0), right=new LatLng(0,0);


        int number = currentIntervals.size();
        double latPoint, lonPoint;
        //for each interval
        for (int i=0; i<number; i++){

            int color = i%2==0 ? getResources().getColor(R.color.interval_red) : getResources().getColor(R.color.interval_green);

            locationList.clear();

            String [] latStringList =  currentIntervals.get(i).getLatLonList().split(",");
            int listLength = latStringList.length-1;
            for (int j=0; j< listLength; j+=2){

                latPoint = Double.valueOf(latStringList[j]);
                lonPoint = Double.parseDouble(latStringList[j + 1]);



                if (latPoint>northPoint) {
                    northPoint = latPoint;
                    top = new LatLng(latPoint, lonPoint);
                }
                if (latPoint< southPoint) {
                    southPoint = latPoint;
                    bottom = new LatLng(latPoint, lonPoint);
                }

                if (lonPoint>eastPoint) {
                    eastPoint = lonPoint;
                    right = new LatLng(latPoint, lonPoint);
                }
                if (lonPoint< westPoint) {
                    westPoint = lonPoint;
                    left = new LatLng(latPoint, lonPoint);
                }



//               if (j%2==0)
                   locationList.add(new LatLng(latPoint, lonPoint));
            }

            int currSize = locationList.size()-1;

            for (int k=0; k<currSize; k++){
                        googleMap.addPolyline(new PolylineOptions().add(locationList.get(k), locationList.get(k + 1)).width(6).color(color));
            }

        }

//        int zoom = 20;
//        boolean allVisible=false;
//        while (zoom>8 && !allVisible) {
//
//

//            zoomPoint = midPoint(northPoint, westPoint, southPoint, eastPoint);
//            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoomPoint, zoom));
//            googleMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), 1, null);
//            LatLngBounds bounds =  googleMap.getProjection().getVisibleRegion().latLngBounds;

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(top);
            builder.include(bottom);
            builder.include(left);
            builder.include(right);

            bounds = builder.build();

            try{
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 60));

            }catch (Exception e){
                Log.v("LATLNG", "MAP CRASH");
            }



//            if (bounds.contains(top)&& bounds.contains(bottom)&& bounds.contains(left)&& bounds.contains(right)){
//                allVisible=true;
//            }
//            --zoom;
//
//        }

    }

//    public LatLng midPoint(double lat1,double lon1,double lat2,double lon2){
//
//        double dLon = Math.toRadians(lon2 - lon1);
//
//        //convert to radians
//        lat1 = Math.toRadians(lat1);
//        lat2 = Math.toRadians(lat2);
//        lon1 = Math.toRadians(lon1);
//
//        double Bx = Math.cos(lat2) * Math.cos(dLon);
//        double By = Math.cos(lat2) * Math.sin(dLon);
//        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
//        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);
//
//        //print out in degrees
////        System.out.println(Math.toDegrees(lat3) + " " + Math.toDegrees(lon3));
////        googleMap.addMarker(new MarkerOptions()
//////                        .infoWindowAnchor(0.48f, 4.16f)
////
////                        .position(new LatLng(Math.toDegrees(lat3),Math.toDegrees(lon3)))
////                        .title("You are here")
////                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher))
////        );
//
//        return new LatLng(Math.toDegrees(lat3),Math.toDegrees(lon3));
//    }
    
    
    static FrgShowRuns init(int val) {
        FrgShowRuns truitonList = new FrgShowRuns();

        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);

        return truitonList;
    }




    private void confirmDelete(final Long trId,final int groupPosition, final int position){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        alertDialogBuilder.setTitle("Confirm")
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

        ((ArrayList<Running>)childItems.get(groupPosition)).remove(position);

//        runs.remove(position);
        adapterExp.notifyDataSetChanged();
//        adapterRunning.notifyDataSetChanged();
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

        private class runningViewHolder{
            TextView description;
            TextView intervalCount;
            TextView date;
        }

        private class headerViewHolder{
            TextView month;
        }

        @Override
        public void notifyDataSetChanged() {

            super.notifyDataSetChanged();
            showTextNoRuns();
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            child = (ArrayList<Running>) childtems.get(groupPosition);

            runningViewHolder holder =null;
            if (convertView == null || !(convertView.getTag() instanceof runningViewHolder)) {
                convertView = inflater.inflate(R.layout.list_running_row, parent, false);

                holder = new runningViewHolder();

                holder.intervalCount = (TextView) convertView
                        .findViewById(R.id.intervalCount);
                holder.description = (TextView) convertView
                        .findViewById(R.id.runningDescription);
                holder.date =  (TextView) convertView
                        .findViewById(R.id.runningDate);

                convertView.setTag(holder);
            } else {
                holder = (runningViewHolder) convertView.getTag();

            }

            holder.description.setText(child.get(childPosition).getDistance()+" meters with "+((int)(child.get(childPosition).getTime()/1000))+" secs rest");
            holder.date.setText( child.get(childPosition).getDate() );
            holder.intervalCount.setText(String.valueOf(child.get(childPosition).getIntervals().size())+" sessions");

        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                confirmDelete(child.get(childPosition).getRunning_id(),groupPosition, childPosition);
                return false;
            }
        });
//
//
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                currentRun = child.get(childPosition);
                showIntervalsForRun(child.get(childPosition));
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


            holder.month.setText(parentItems.get(groupPosition) + " - " + ((ArrayList<Running>) childtems.get(groupPosition)).size() + " workouts");

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

            if (childtems.size()>0)
                return ((ArrayList<String>) childtems.get(groupPosition)).size();
            else{
                showTextNoRuns();
                return 0;
            }
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
            runsExpListView.setVisibility(View.GONE);
            noRunsText.setVisibility(View.VISIBLE);
        }else{
            runsExpListView.setVisibility(View.VISIBLE);
            noRunsText.setVisibility(View.GONE);
        }
    }


}