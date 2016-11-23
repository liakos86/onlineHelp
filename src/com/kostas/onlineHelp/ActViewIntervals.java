package com.kostas.onlineHelp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.kostas.custom.MapWrapperLayout;
import com.kostas.dbObjects.Interval;
import com.kostas.dbObjects.Running;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;
import com.kostas.mongo.SyncHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liakos on 25/7/2016.
 */
public class ActViewIntervals extends BaseFrgActivityWithBottomButtons implements OnMapReadyCallback{

    /**
     * Parent view containing every layout
     */
    ViewFlipper viewFlipper;

    /**
     * All the intervals together
     */
    List<Interval> intervals = new ArrayList<Interval>();

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
    Button openMapButton,  shareFriendsButton, closeIntervalsButton;

    /**
     * If the map is drawn we dont need to redraw
     */
    boolean alreadyDrawn, isMyRun;
    ArrayList<Marker> markers = new ArrayList<Marker>();
    ListView intervalListView;
    IntervalAdapterItem adapterInterval;
    SyncHelper sh;
    Running run;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_intervals);

        Long runId = getIntent().getExtras().getLong("run");
        isMyRun = getIntent().getExtras().getBoolean("myRun");
        Database db = new Database(getApplication());

        if (isMyRun){
            run = Running.getFromId(getApplication(), runId, ContentDescriptor.Running.CONTENT_URI);
            intervals = db.fetchIntervalsForRun(runId, ContentDescriptor.Interval.CONTENT_URI);
            run.setIntervals(intervals);
        }else{
            run = Running.getFromId(getApplication(), runId, ContentDescriptor.RunningFriend.CONTENT_URI);
            intervals = db.fetchIntervalsForRun(runId, ContentDescriptor.IntervalFriend.CONTENT_URI);
            run.setIntervals(intervals);
        }
        initializeViews();
        initializeMap();
        intervalListView.setDivider(null);
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

        mapWrapperLayout = (MapWrapperLayout)findViewById(R.id.mapWrapperRuns);
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });


        if (googleMap!=null) {
            drawMap();
        }else{
            Toast.makeText(this, "Google maps not present...", Toast.LENGTH_SHORT).show();
        }
    }

    public void initializeMap(){
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapListKostas)).getMapAsync(this);

    }

    void initializeViews(){

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipperIntervals);

        sh = new SyncHelper((ExtApplication)getApplication());
        openMapButton = (Button) findViewById(R.id.buttonShowMap);
        closeMapButton = (ImageButton) findViewById(R.id.buttonCloseMap);

        intervalListView = (ListView) findViewById(R.id.listIntervals);

        shareFriendsButton = ((Button) findViewById(R.id.buttonShareFriends));

        closeIntervalsButton = ((Button) findViewById(R.id.buttonCloseIntervals));

        shareFriendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new shareRunAsync((ExtApplication)getApplication()).execute();


            }
        });

        if (!isMyRun){
            shareFriendsButton.setVisibility(View.GONE);
        }

        closeIntervalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                goBack();

            }
        });



        adapterInterval = new IntervalAdapterItem(this, getApplicationContext(),
                R.layout.list_interval_row, intervals, true);
        intervalListView.setAdapter(adapterInterval);



        closeMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.setDisplayedChild(0);
            }
        });

        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.setDisplayedChild(1);
            }
        });
    }

    public void goBack(){
        this.onBackPressed();
    }

    public void drawMap(){
        alreadyDrawn = true;
        List<Interval> currentIntervals = run.getIntervals();
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
                Toast.makeText(this,(i+1)+" : "+ current.getLatLonList(), Toast.LENGTH_SHORT).show();
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



    private class shareRunAsync extends AsyncTask<Void, Void, Integer> {
        private ExtApplication app;

        public shareRunAsync(ExtApplication app) {
            this.app = app;
        }

        protected void onPreExecute() {
            shareFriendsButton.setClickable(false);
        }

        @Override
        protected Integer doInBackground(Void... unused) {
            return  sh.shareRunToMongo(run);
        }

        @Override
        protected void onPostExecute(Integer result) {
//            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(getWindow().getWindowToken(), 0);

            if (result==0) {
                Toast.makeText(app, "Share failed", Toast.LENGTH_LONG).show();
                shareFriendsButton.setClickable(true);
            }else if (result==1){
                Toast.makeText(app, "Run shared", Toast.LENGTH_LONG).show();
                Database db = new Database(app);
                db.setSharedFlagTrue(run.getRunning_id());
                shareFriendsButton.setText("Run already shared");
            }
        }
    }

}
