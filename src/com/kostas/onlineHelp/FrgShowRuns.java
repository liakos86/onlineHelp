package com.kostas.onlineHelp;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
//import com.facebook.share.model.SharePhoto;
//import com.facebook.share.model.SharePhotoContent;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.OnMapReadyCallback;
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
public class FrgShowRuns extends Fragment implements OnMapReadyCallback{

    List<Interval> intervals = new ArrayList<Interval>();
    private ArrayList<String> parentItems = new ArrayList<String>();
    private ArrayList<Object> childItems = new ArrayList<Object>();
    private ExpandableListView runsExpListView;
    MyExpandableAdapter adapterExp;
    TextView noRunsText;
    ProgressBar progressBarMap;
    Running currentRun;
    List<Interval> currentIntervals;
    ArrayList<Marker> markers = new ArrayList<Marker>();
    ListView intervalListView;
    IntervalAdapterItem adapterInterval;
    ViewFlipper viewFlipper;
    GoogleMap googleMap;
    MapWrapperLayout mapWrapperLayout;
    private ViewGroup infoWindow, infoWindowEmpty;
    LatLngBounds bounds;
    ImageButton closeMapButton;
    Button openMapButton, closeIntervalsButton, shareButton;
    boolean alreadyDrawn;



    private Button element_top;
    private Button element_bottom;
    private Button element_left;
    private Button element_right;

    AdView adView;
    AdRequest adRequest;



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

    public static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private boolean placeAd() {

        SharedPreferences app_preferences  = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String deviceId = app_preferences.getString("deviceId", null);

        if (deviceId!=null) {


            adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice(deviceId)
                    .build();

            return  true;
        }else return false;

    }



    private void setCustomMapInfoWindow(View v) {

         mapWrapperLayout = (MapWrapperLayout) v.findViewById(R.id.mapWrapperRuns);

        mapWrapperLayout.init(googleMap, getPixelsFromDp(getActivity().getApplicationContext(), 39 - 134));

        // We want to reuse the info window for all the markers,
        // so let's create only one class member instance
        infoWindow = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.custom_map_info, null);

        infoWindowEmpty = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.custom_map_info_empty, null);

        element_top = (Button) infoWindow.findViewById(R.id.element_top);
        element_bottom = (Button) infoWindow.findViewById(R.id.element_bottom);
        element_left = (Button) infoWindow.findViewById(R.id.element_left);
        element_right = (Button) infoWindow.findViewById(R.id.element_right);

        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                // We must call this to set the current marker and infoWindow references
                // to the MapWrapperLayout

                mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);


                if (marker.getTitle()==null)
                    return infoWindowEmpty;
                else {
                    int size = currentIntervals.size();
                    for (int i=0;i<size; i++){

                        Interval interval = currentIntervals.get(i);
                        if (marker.getTitle().equals(String.valueOf(interval.getInterval_id()))){
                            element_top.setText(
                                    "interval "+(i+1)+"\r\n"+
                                    (int)interval.getDistance()+"m"

                            );

                            element_left.setText(
                                    "Start: "+interval.getAltitudeStart()+"m \r\n"+
                                            "Finish: "+interval.getAltitudeFinish()+"m \r\n"+
                                            "Max: "+interval.getAltitudeMax()+"m \r\n"+
                                            "Min: "+interval.getAltitudeMin()+"m"

                            );

                            long intervalTime = interval.getMilliseconds();

                            int hours = (int)(intervalTime/3600000);
                            int mins = (int)((intervalTime - (hours*3600000))/60000);
                            int secs = (int)((intervalTime - (hours*3600000) - (mins*60000))/1000);
                            String timeText = String.format("%02d",hours)+":" + String.format("%02d",mins)+":"+String.format("%02d",secs);
                            String speedText =  String.format("%1$,.2f", ((double) ((interval.getDistance() / intervalTime) * 3600)));
                            float pace =  intervalTime / interval.getDistance();
                            int paceMinutes = (int)(pace/60);
                            int paceSeconds = (int)(pace - (paceMinutes*60));
                            String paceText = paceMinutes<60 ?   String.format("%02d", paceMinutes)+"m "+String.format("%02d", paceSeconds)+"s" : "over 1 hour";

                            element_right.setText(
                                    "Time: "+timeText+"\r\n"+
                                            "Speed: "+speedText+"km/h\r\n"+
                                            "Pace: "+paceText

                            );


                        }
                    }
                    return infoWindow;
                }

            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });



    }

    private void initializeViews(View v){


        adView = (AdView) v.findViewById(R.id.adViewInterval3);
        progressBarMap = (ProgressBar) v.findViewById(R.id.progressBarMap);
        viewFlipper = (ViewFlipper) v.findViewById(R.id.viewFlipper);

        openMapButton = (Button) v.findViewById(R.id.buttonShowMap);
        closeMapButton = (ImageButton) v.findViewById(R.id.buttonCloseMap);
        closeIntervalsButton = (Button) v.findViewById(R.id.buttonCloseIntervals);

        shareButton = (Button) v.findViewById(R.id.buttonShare);

        noRunsText = (TextView) v.findViewById(R.id.noRunsText);

        runsExpListView = (ExpandableListView) v.findViewById(R.id.listExpRunning);
        intervalListView = (ListView) v.findViewById(R.id.listIntervals);

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
//        new PerformAsyncTask(getActivity(), true).execute();


    }
    
    private void setList(){


        intervalListView.setDivider(null);

//        adapterRunning = new RunningAdapterItem(getActivity().getApplicationContext(),
//                R.layout.list_running_row, runs);
//        runningListView.setAdapter(adapterRunning);

         new PerformAsyncTask(getActivity()).execute();

//        getRunsFromDb();
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

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;

        //placing the indicator 'right' pixels from the right of the view
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            runsExpListView.setIndicatorBounds(width-GetPixelFromDips(40), width-GetPixelFromDips(20));

//            runsExpListView.setIndicatorBounds(right - getResources().getDrawable(R.drawable.arrow_down).getIntrinsicWidth(), right);
        } else {


            runsExpListView.setIndicatorBoundsRelative(width-GetPixelFromDips(40), width-GetPixelFromDips(20));

//            runsExpListView.setIndicatorBoundsRelative(right - getResources().getDrawable(R.drawable.arrow_down).getIntrinsicWidth(), right);
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

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {





//                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
//                Uri screenshotUri = Uri.parse("http://coins.silvercoinstoday.com/wp-content/uploads/2010/10/America-the-Beautiful-Silver-Coin-Obverse.jpg");

                try {
//                    InputStream stream = getActivity().getContentResolver().openInputStream(screenshotUri);

//                    final int w = 300;
//                    final int h = 300;
//
//                    Bitmap image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//
//
//                    SharePhoto photo = new SharePhoto.Builder()
//                            .setBitmap(image)
//                            .build();
//                    SharePhotoContent content = new SharePhotoContent.Builder()
//                            .addPhoto(photo)
//                            .build();
                }

                catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

//                sharingIntent.setType("image/*");
//                sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
//                startActivity(Intent.createChooser(sharingIntent, "Share image using"));

            }
        });
        
    }

    public int GetPixelFromDips(float pixels) {
        // Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);
    }

    public void getRunsFromDb(Activity activity, boolean fromAsync){
        Database db = new Database(activity);
        List <Running> newRuns = db.fetchRunsFromDb();

//        List<Long> err = new ArrayList<Long>();


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

                if (rounds>0)
                intervalsList.get(fastest).setFastest(true);
//                else
//                err.add(running.getRunning_id());

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

            if (adapterExp!=null&& !fromAsync) {
                adapterExp.notifyDataSetChanged();
                runsExpListView.expandGroup(0);
            }

        }

//        return null;
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
        //for each interval
        for (int i=0; i<number; i++) {

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


//               if (j%2==0)
                    locationList.add(new LatLng(latPoint, lonPoint));
                }

                int currSize = locationList.size() - 1;

                for (int k = 0; k < currSize; k++) {
                    googleMap.addPolyline(new PolylineOptions().add(locationList.get(k), locationList.get(k + 1)).width(6).color(color));
                }


                if (currSize > 0) {
                    markers.add(googleMap.addMarker(new MarkerOptions()
                                            .infoWindowAnchor(0.48f, 6.16f)
                                            .position(locationList.get(0))
                                            .title(String.valueOf(current.getInterval_id()))
//                                .title(String.valueOf(Html.fromHtml("<b>Interval " + (i + 1) + "</b>")))
//                                .snippet("Speed: " + String.format("%1$,.2f", ((double) ((current.getDistance() / current.getMilliseconds()) * 3600))) + " km/h")
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_start2))
                            )
                    );


                    googleMap.addMarker(new MarkerOptions()
//                        .infoWindowAnchor(0.48f, 4.16f)
                                    .position(locationList.get(currSize))
//                                .title(String.valueOf(Html.fromHtml("<b>Interval " + (i + 1) + "</b>")))
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
                //Log.v("LATLNG", "MAP CRASH");
            }
        }

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

    @Override
    public void onMapReady(GoogleMap gMap) {

            googleMap = gMap;
            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            googleMap.setIndoorEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        progressBarMap.setVisibility(View.GONE);

        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
            }
        });


        setCustomMapInfoWindow(getView());


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

                holder.description.setText((int) child.get(childPosition).getDistance() + " meters with " + ((int) (child.get(childPosition).getTime() / 1000)) + " secs rest");
                holder.date.setText(child.get(childPosition).getDate() + "  id = " + child.get(childPosition).getRunning_id());
                holder.intervalCount.setText(String.valueOf(child.get(childPosition).getIntervals().size()) + " sessions");

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        confirmDelete(child.get(childPosition).getRunning_id(), groupPosition, childPosition);
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

            int month=1;
            try {
                 month = Integer.valueOf(parentItems.get(groupPosition).split("/")[0]);
            }catch (Exception e){
                //Log.v("LATLNG", "month error");
            }
            String monthName="";

            switch (month){

                case 1: monthName = "January"; break;
                case 2: monthName = "February"; break;
                case 3: monthName = "March"; break;
                case 4 : monthName = "April"; break;
                case 5: monthName = "May"; break;
                case 6: monthName = "June"; break;
                case 7: monthName = "July"; break;
                case 8 : monthName = "August"; break;
                case 9: monthName = "September"; break;
                case 10: monthName = "October"; break;
                case 11: monthName = "November"; break;
                case 12 : monthName = "December"; break;
                default: monthName = parentItems.get(groupPosition);
            }




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



    private class PerformAsyncTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;
        private boolean shouldPlaceAd;
//        List<Long>err;


        public PerformAsyncTask(Activity activity) {
            this.activity = activity;
        }

        protected void onPreExecute() {
            runsExpListView.setClickable(false);
        }

        @Override
        protected Void doInBackground(Void... unused) {


             getRunsFromDb(activity, true);
            shouldPlaceAd = placeAd();

            return null;

        }

        @Override
        protected void onPostExecute(Void result) {

            runsExpListView.setClickable(true);
            if (adapterExp!=null) adapterExp.notifyDataSetChanged();
            if (shouldPlaceAd) adView.loadAd(adRequest);

//            if (err.size()>0){
//                for (long l:err){
//                    Toast.makeText(getActivity(), "id = "+l, Toast.LENGTH_SHORT).show();
//                }
//            }



        }

    }


}
