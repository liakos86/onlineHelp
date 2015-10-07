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
import com.google.android.gms.maps.model.*;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Running;
import com.kostas.model.Database;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


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



//    List<Polyline> mapLines;

    Marker markerStart, markerFinish;

    Running currentRun;

    ListView runningListView, intervalListView;
//    RunningAdapterItem adapterRunning;
    IntervalAdapterItem adapterInterval;

    ViewFlipper viewFlipper;
    GoogleMap googleMap;
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

        setList(v);
        initializeMap();


        return  v;
    }

    public void initializeMap(){
        SupportMapFragment fm = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapListKostas);
        googleMap = fm.getMap();
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.setIndoorEnabled(false);
//        mapLines = new ArrayList<Polyline>();
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
        double latPoint, lonPoint;


        //merging the list of the points of the intervals
        String latLonList = currentIntervals.get(0).getLatLonList();
        int number = currentIntervals.size();
        for (int i=1; i<number; i++){
            latLonList +=","+ currentIntervals.get(i).getLatLonList();
        }


        String[] pointsList = latLonList.split(",");
        int pointsLength = pointsList.length;

        List<LatLng> locationList = new ArrayList<LatLng>();




        double northPoint=-85.05115 , southPoint=85.05115 , eastPoint=-180, westPoint=180;
        LatLng top = new LatLng(0,0), bottom=new LatLng(0,0), left=new LatLng(0,0), right=new LatLng(0,0);

//        for (int i=0; i<pointsLength-1; i+=2){
//
//            latPoint = Double.valueOf(pointsList[i]);
//            lonPoint = Double.parseDouble(pointsList[i + 1]);
//
//
//            //create a box that contains the run, then take the center of the diagonal
//            if (latPoint>northPoint) {
//                northPoint = latPoint;
//                top = new LatLng(latPoint, lonPoint);
//            }
//            if (latPoint< southPoint) {
//                southPoint = latPoint;
//                bottom = new LatLng(latPoint, lonPoint);
//            }
//
//            if (lonPoint>eastPoint) {
//                eastPoint = lonPoint;
//                right = new LatLng(latPoint, lonPoint);
//            }
//            if (lonPoint< westPoint) {
//                westPoint = lonPoint;
//                left = new LatLng(latPoint, lonPoint);
//            }

            //todo re-enable if error
//            locationList.add(new LatLng(latPoint, lonPoint));

//        }
//        int latlonLength = locationList.size();
//
//
//        for (int i=0; i<latlonLength-1; i++){
//
//
//            PolylineOptions line  = new PolylineOptions().add(locationList.get(i), locationList.get(i + 1)).width(5).color(Color.RED);
//
//            Polyline pline = googleMap.addPolyline(line);
//
//
//            mapLines.add(pline);
//
//        }



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



                locationList.add(new LatLng(latPoint, lonPoint));
            }

            int currSize = locationList.size()-1;

            for (int k=0; k<currSize; k++){


//                PolylineOptions line  = new PolylineOptions().add(locationList.get(k), locationList.get(k + 1)).width(6).color(color);

//                Polyline pline =
                        googleMap.addPolyline(new PolylineOptions().add(locationList.get(k), locationList.get(k + 1)).width(6).color(color));
//                mapLines.add(pline);

            }

        }

        int zoom = 20;
        boolean allVisible=false;
        while (zoom>8 && !allVisible) {

            zoomPoint = midPoint(northPoint, westPoint, southPoint, eastPoint);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(midPoint(northPoint, westPoint, southPoint, eastPoint), zoom));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), 1, null);
            LatLngBounds bounds =  googleMap.getProjection().getVisibleRegion().latLngBounds;

            if (bounds.contains(top)&& bounds.contains(bottom)&& bounds.contains(left)&& bounds.contains(right)){
                allVisible=true;
            }
            --zoom;
        }

    }

    public LatLng midPoint(double lat1,double lon1,double lat2,double lon2){

        double dLon = Math.toRadians(lon2 - lon1);

        //convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        //print out in degrees
//        System.out.println(Math.toDegrees(lat3) + " " + Math.toDegrees(lon3));
//        googleMap.addMarker(new MarkerOptions()
////                        .infoWindowAnchor(0.48f, 4.16f)
//
//                        .position(new LatLng(Math.toDegrees(lat3),Math.toDegrees(lon3)))
//                        .title("You are here")
//                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher))
//        );

        return new LatLng(Math.toDegrees(lat3),Math.toDegrees(lon3));
    }

    public void zoomMap(){

        Log.v("LATLNG", "zooming only");
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoomPoint, 18));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(16), 2000, null);

    }
    
    
    static FrgShowRuns init(int val) {
        FrgShowRuns truitonList = new FrgShowRuns();

        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);

        return truitonList;
    }

    // here's our beautiful adapterRunning
//    public class RunningAdapterItem extends ArrayAdapter<Running> {
//
//        Context mContext;
//        int layoutResourceId;
//        List<Running> data;
//
//        public RunningAdapterItem(Context mContext, int layoutResourceId,
//                                List<Running> data) {
//
//            super(mContext, layoutResourceId, data);
//            this.layoutResourceId = layoutResourceId;
//            this.mContext = mContext;
//            this.data = data;
//        }
//
//        @Override
//        public View getView(final int position, View convertView, ViewGroup parent) {
//
//            runningViewHolder holder =null;
//            if (convertView == null || !(convertView.getTag() instanceof runningViewHolder)) {
//                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_running_row, parent, false);
//
//                holder = new runningViewHolder();
//
//                holder.intervalCount = (TextView) convertView
//                        .findViewById(R.id.intervalCount);
//                holder.description = (TextView) convertView
//                        .findViewById(R.id.runningDescription);
//                holder.date =  (TextView) convertView
//                        .findViewById(R.id.runningDate);
//
//                convertView.setTag(holder);
//            } else {
//                holder = (runningViewHolder) convertView.getTag();
//
//            }
//
//            holder.description.setText(runs.get(position).getDistance()+" meters with "+((int)(runs.get(position).getTime()/1000))+" secs rest");
//            holder.date.setText( runs.get(position).getDate() );
//            holder.intervalCount.setText(String.valueOf(runs.get(position).getIntervals().size())+" sessions");
//
//            convertView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View view) {
//                    confirmDelete(runs.get(position).getRunning_id(), position);
//                    return false;
//                }
//            });
//
//
//            convertView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//
//                        currentRun = runs.get(position);
//                        showIntervalsForRun(runs.get(position));
//                }
//            });
//
//            return convertView;
//
//        }
//
//    }



    private class runningViewHolder{
        TextView description;
        TextView intervalCount;
        TextView date;
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

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.group_header, null);
            }else{

            }


            ((TextView) convertView.findViewById(R.id.textView1)).setText(parentItems.get(groupPosition)+" - "+((ArrayList<Running>)childtems.get(groupPosition)).size()+" workouts");

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
            return ((ArrayList<String>) childtems.get(groupPosition)).size();
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




}
