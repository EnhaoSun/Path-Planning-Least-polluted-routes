package com.dt.sunenhao.pathplanning;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


// classes needed to initialize map
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.dt.sunenhao.pathplanning.Object.DataIO;
import com.dt.sunenhao.pathplanning.Object.Route;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

// classes needed to add the location component
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;

// classes needed to add a marker
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgba;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapWeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, PermissionsListener {

    private static final String TAG = "MyActivity";

    //MapView
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    private static final float DEFAULT_ZOOM = 15f;

    // variables for calculating and drawing a route
    private NavigationMapRoute navigationMapRoute;
    private ArrayList<ArrayList<DirectionsRoute>> currentRoute;
    ArrayList<DirectionsRoute> allRoutes;

    //widgets
    private Button usePollution_button;
    private Button altRoute_button;
    private Button updatePOL_button;
    private ImageView planRoute;
    private ImageView showHeatMap;
    private ImageView mGps;
    private ImageView mClear;
    private ImageView showRouteInfo;
    private CoordinatorLayout myLayout;
    private TableLayout tableLayout;
    private ProgressDialog progressDialog;

    //Heatmap
    private boolean heatMapOn = false;
    private static ArrayList<String> AIRPOLLUTION_SOURCE_ID;
    private static ArrayList<String> HEATMAP_LAYER_ID;
    private static ArrayList<String> heatMapJson;

    private static final String destinationSymbolLayer_ID = "destination-symbol-layer-id";
    private static final String destinationSource_ID = "destination-source-id";

    //URL parameters
    private static boolean updatePM = false;
    private static boolean alternative = false;
    private static boolean usePollution = false;

    //Point and Route
    private Point originPoint = null;
    private Point destinationPoint = null;
    private DecimalFormat df = new DecimalFormat("#.##");
    private List<Route> routes;


    //Variables for test
    ArrayList<String> routeJson = null;
    int testRouteIndex = 0;
    String testPolTime = null;
    loadHM lhm;
    String testRoutefile = "routeJsonShort2.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getApplicationContext(), getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT > 9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //init widgets
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        mGps = findViewById(R.id.ic_gps);
        mClear = findViewById(R.id.ic_clear);
        myLayout = (CoordinatorLayout) findViewById(R.id.myLayout);
        tableLayout = findViewById(R.id.tableLayout);
        showRouteInfo = findViewById(R.id.showRouteInfo);
        usePollution_button = findViewById(R.id.usePollution);
        altRoute_button = findViewById(R.id.ALT_route);
        updatePOL_button = findViewById(R.id.updatePOL);
        planRoute = findViewById(R.id.planRoute);
        showHeatMap = findViewById(R.id.showHeatMap);
        showHeatMap.setEnabled(true);

        // Initializing variables
        df.setRoundingMode(RoundingMode.HALF_UP);

        currentRoute = new ArrayList<>();
        allRoutes = new ArrayList<>();
        routes = new ArrayList<>();

        AIRPOLLUTION_SOURCE_ID = new ArrayList<>();
        HEATMAP_LAYER_ID = new ArrayList<>();
        heatMapJson = new ArrayList<>();

        setupAutoCompleteFragment();

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Route Planner");
        progressDialog.setMessage("Planning...");


        //hide button
        /*
        mClear.setVisibility(View.GONE);
        mGps.setVisibility(View.GONE);
        updatePOL_button.setVisibility(View.GONE);
        usePollution_button.setVisibility(View.GONE);
        altRoute_button.setVisibility(View.GONE);
        */
        //showHeatMap.setVisibility(View.GONE);
    }

    /*
     * Table: Information of route: PM1, PM2.5, PM10, Distance
     */
    private void addRowToTable(String routeName, String PM1, String PM2, String PM10, String distance){
        System.out.println("Add row to table");
        TableRow tableRow = new TableRow(this);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        tableRow.setLayoutParams(layoutParams);
        TextView tvRoute = new TextView(this);
        TextView tvPM1 = new TextView(this);
        TextView tvPM2 = new TextView(this);
        TextView tvPM10 = new TextView(this);
        TextView tvDistance = new TextView(this);
        tvRoute.setGravity(Gravity.CENTER);
        tvRoute.setText(routeName);
        tvPM1.setGravity(Gravity.CENTER);
        tvPM1.setText(PM1);

        tvPM2.setText(PM2);
        tvPM2.setGravity(Gravity.CENTER);

        tvPM10.setText(PM10);
        tvPM10.setGravity(Gravity.CENTER);

        tvDistance.setText(distance);
        tvDistance.setGravity(Gravity.CENTER);

        tableRow.addView(tvRoute, 0);
        tableRow.addView(tvPM1, 1);
        tableRow.addView(tvPM2, 2);
        tableRow.addView(tvPM10, 3);
        tableRow.addView(tvDistance, 4);
        tableLayout.addView(tableRow);

        /*
        TableRow tableRow2 = new TableRow(this);
        TextView tvTime = new TextView(this);
        tvTime.setText(testPolTime);
        tvTime.setGravity(Gravity.CENTER);
        tableRow2.addView(tvTime);

        TableRow.LayoutParams layoutParams2 = (TableRow.LayoutParams) tvTime.getLayoutParams();
        layoutParams2.span = 2;
        layoutParams2.weight = 1;
        tableRow2.setLayoutParams(layoutParams2);
        tableLayout.addView(tableRow2);
        */
    }
    private void clearTable(){
        int rowCount = tableLayout.getChildCount();
        while(tableLayout.getChildCount() > 1){
            View rowView = tableLayout.getChildAt(1);
            if(rowView instanceof TableRow){
                tableLayout.removeViewAt(1);
            }
        }
        System.out.println("Number of table children : " + rowCount);
    }
    private void showOrHideTable(){
        for(int i = 0; i < 5; i++) {
            if (tableLayout.isColumnCollapsed(i))
                tableLayout.setColumnCollapsed(i, false);
            else
                tableLayout.setColumnCollapsed(i, true);
        }
    }
    private void updateRouteTableInfo(){
        clearTable();
        if(routes.size() >= 1){
            for(int i = 0; i < routes.size(); i++){
                String routeName = (i == 0 ? "Blue" : "Gray");
                String PM1 = df.format(routes.get(i).getTotalPM1()/routes.get(i).getTotalDistance());
                String PM2 = df.format(routes.get(i).getTotalPM2()/routes.get(i).getTotalDistance());
                String PM10 = df.format(routes.get(i).getTotalPM10()/routes.get(i).getTotalDistance());
                String distance = df.format(routes.get(i).getTotalDistance());
                addRowToTable(routeName, PM1, PM2, PM10, distance);
            }
        }
    }
    private void displayRouteInfo(){
        showOrHideTable();
    }

    /*
     * Search Bar
     */
    private void setupAutoCompleteFragment(){
        String apiKey = "API Key for Google Map service";
        if (!Places.isInitialized()){
            Places.initialize(getApplicationContext(), apiKey);
        }
        PlacesClient placesClient= Places.createClient(this);

        final AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        View clearButton = autocompleteSupportFragment.getView().findViewById(R.id.places_autocomplete_clear_button);
        if(clearButton != null){
            autocompleteSupportFragment.getView().findViewById(R.id.places_autocomplete_clear_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearPoint();
                    autocompleteSupportFragment.setText("");
                }
            });
                                                                                                                                                                        }

        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                final com.google.android.gms.maps.model.LatLng googlelatLng = place.getLatLng();
                LatLng latLng = new LatLng(googlelatLng.latitude, googlelatLng.longitude);
                addDestinationIconSymbolLayer(mapboxMap.getStyle());
                moveCamera(latLng, DEFAULT_ZOOM);
                Log.d(TAG, "autocomplete: found a location: " + latLng.toString());
                if(locationComponent.getCameraMode() == CameraMode.TRACKING){
                    locationComponent.setCameraMode(CameraMode.NONE);
                }
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }

    /*
     * Initializing onClick listeners
     */
    private void init(){
        Log.d(TAG, "init: initializing");

        showRouteInfo.setOnClickListener((View v) ->{
            displayRouteInfo();
        });

        usePollution_button.setOnClickListener((View v) -> {
            if (usePollution) {
                usePollution = false;
                usePollution_button.setBackgroundResource(R.drawable.round_button);
            } else {
                usePollution = true;
                usePollution_button.setBackgroundResource(R.drawable.round_button_activate);
            }
        });

        altRoute_button.setOnClickListener((View v) -> {
            if (alternative) {
                alternative = false;
                altRoute_button.setBackgroundResource(R.drawable.round_button);
            } else {
                alternative = true;
                altRoute_button.setBackgroundResource(R.drawable.round_button_activate);
            }
        });

        updatePOL_button.setOnClickListener((View v) -> {
            if (updatePM) {
                updatePM = false;
                updatePOL_button.setBackgroundResource(R.drawable.round_button);
            } else {
                updatePM = true;
                updatePOL_button.setBackgroundResource(R.drawable.round_button_activate);
            }
        });

        planRoute.setOnClickListener((View v) ->
                {
                try {
                    new planTask().execute();
                    //testPlanRoutes();
                    //planRoutes();
                    //updateRouteUI();
                }catch (Exception e) {
                    e.printStackTrace();
                }
        }
        );

        mGps.setOnClickListener(v -> {
            Log.d(TAG, "onClick: click gps icon");
            getDeviceLocation(DEFAULT_ZOOM);
        });

        showHeatMap.setOnClickListener((View v) ->
                            setShowHeatMap(mapboxMap.getStyle())
        );

         mClear.setOnClickListener((View v) ->
                 clearRoute()
         );

    }

    /*
     * Move camera
     */
    private void getDeviceLocation(float zoom){
        originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(originPoint.latitude(), originPoint.longitude()))
                .zoom(zoom)
                .tilt(20)
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 1000);
    }
    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to : lat: " + latLng.getLatitude() + ", lng: " + latLng.getLongitude());

        destinationPoint = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
        GeoJsonSource source = mapboxMap.getStyle().getSourceAs(destinationSource_ID);
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(latLng.getLatitude(), latLng.getLongitude()))
                .zoom(zoom)
                .tilt(20)
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 1000);
        Log.d(TAG, "Finished moveCamera: moving the camera to : lat: " + latLng.getLatitude() + ", lng: " + latLng.getLongitude());
    }

    /*
     * plan routes
     */
    private boolean planRoutes() throws Exception{
        System.out.println("navigation button clicked");
        originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
        if(originPoint == null || destinationPoint == null)
            return false;

        System.out.println("Start plan the route");
        double sLat = originPoint.latitude();
        double sLong = originPoint.longitude();
        double tLat = destinationPoint.latitude();
        double tLong = destinationPoint.longitude();

        System.out.println("/" + sLat + "/" + sLong + "/" + tLat + "/" + tLong);
        routes = DataIO.getRoutes(usePollution, updatePM, alternative, sLat, sLong, tLat, tLong);
        return true;
    }

    private void updateRouteUI(Boolean result){
        if(!result){
            Snackbar snackbar = Snackbar.make(myLayout, "Please select a destination", Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();
            TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(Color.BLACK);
            snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            snackbar.setAction("CLOSE", v -> {
            }).setActionTextColor(Color.BLACK).show();
            return;
        }

        if (routes == null){
            Snackbar snackbar = Snackbar.make(myLayout, "Server Error, please try it later", Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();
            TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(Color.BLACK);
            snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            snackbar.setAction("CLOSE", v -> {
            }).setActionTextColor(Color.BLACK).show();
            return;
        }
        clearHeatMap();
        setHeatmapLayerAndSourceID();
        for(int i = 0; i < routes.size(); i++)
            requestMapMatched(routes.get(i), i + 1);
        updateRouteTableInfo();
        currentRoute.clear();
        allRoutes.clear();
        getDeviceLocation(13f);
    }

    /*
     * Clear routes and points
     */
    private void clearPoint(){
        if (mapboxMap.getStyle().getLayer(destinationSymbolLayer_ID) != null) {
            System.out.println("remove point layer");
            mapboxMap.getStyle().removeLayer(destinationSymbolLayer_ID);
        }
    }
    private void clearRoute(){
        Log.d(TAG, "onClick: click clear icon");
        addNavigationMapRoute();
        clearTable();
        clearPoint();
        clearHeatMap();
        destinationPoint = null;
    }

    /*
     * add navigation map route
     */
    private void addNavigationMapRoute(){
        if (navigationMapRoute == null) {
            System.out.println("Initializing navigation map route");
            //Add route above heatmap
            //navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute, "natural-line-label");
            //Add route below heatmap
            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
        }else{
            navigationMapRoute.updateRouteArrowVisibilityTo(false);
            navigationMapRoute.updateRouteVisibilityTo(false);
        }
    }

    /*
     * Display route on map
     */
    private void requestMapMatched(Route route, int routeIndex) {
        addNavigationMapRoute();
        ArrayList<Point> points = route.getRoute();

        System.out.println("Number of points: " + points.size());
        /*
        for(int i = 0; i < points.size(); i++) {
            System.out.println(points.get(i).coordinates());
        }
        */
        if (points.size() < 2){
            Snackbar snackbar = Snackbar.make(myLayout, "Unable to get there. Please try again!", Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();
            TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(Color.BLACK);
            snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            snackbar.setAction("CLOSE", v -> {
            }).setActionTextColor(Color.BLACK).show();
            return;
        }

        int[] trace;
        if(points.size() > 100){
            trace = new int[(points.size()/100) + 1];
            for(int i = 0; i < trace.length; i++){
                trace[i] = i*99;
            }
        }
        else{
            trace = new int[1];
            trace[0] = 0;
        }
        for(int i = 0; i<trace.length; i++)
            System.out.print(trace[i] + " ");
        System.out.println();

        ArrayList<DirectionsRoute> currentRouteI = new ArrayList<>();

        for(int i = 0; i<trace.length; i++) {
            ArrayList<Point> sub = new ArrayList<>();
            if(i == trace.length - 1) {
                sub.addAll(points.subList(trace[i], points.size()));
            }
            else {
                sub.addAll(points.subList(trace[i], trace[i] + 100));
            }
            try {
                // Setup the request using a client.
                MapboxMapMatching.builder()
                        .accessToken(Objects.requireNonNull(Mapbox.getAccessToken()))
                        .coordinates(sub)
                        .steps(true)
                        .voiceInstructions(true)
                        .bannerInstructions(true)
                        .profile(DirectionsCriteria.PROFILE_WALKING)
                        .build()
                        .enqueueCall(new Callback<MapMatchingResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<MapMatchingResponse> call,
                                                   @NonNull Response<MapMatchingResponse> response) {
                                //System.out.println("In mapbox Respond");
                                if (response.isSuccessful()) {
                                    System.out.println("Signing currentRoute");
                                    for (int i = 0; i < response.body().matchings().size(); i++)
                                        currentRouteI.add(response.body().matchings().get(i).toDirectionRoute());
                                    System.out.println("Number of route points: " + sub.size());
                                } else {
                                    // If the response code does not response "OK" an error has occurred.
                                    Timber.e("MapboxMapMatching failed with %s", response.code());
                                    System.out.println("MapboxMapMatching failed with " + response.toString());
                                    return;
                                }
                                List<LegStep> totalLegStep = new ArrayList<>();
                                System.out.println("Current route I size : " + currentRouteI.size());
                                System.out.println("trace size : " + trace.length);

                                if (currentRouteI.size() == trace.length) {
                                    for (int i = 0; i < currentRouteI.size(); i++) {

                                        for (RouteLeg leg : currentRouteI.get(i).legs()) {
                                            for (LegStep legStep : leg.steps()) {
                                                totalLegStep.add(legStep);
                                            }
                                        }
                                        RouteLeg leg1 = RouteLeg.builder()
                                                .distance(currentRouteI.get(i).distance())
                                                .duration(currentRouteI.get(i).duration())
                                                .steps(totalLegStep)
                                                .build();

                                        currentRouteI.get(i).legs().clear();
                                        currentRouteI.get(i).legs().add(leg1);
                                    }

                                    System.out.println("Total Distance: ");
                                    System.out.println("Current Route Size: " + currentRoute.size());
                                    currentRoute.add(currentRouteI);
                                }
                                if(currentRoute.size() == routes.size())
                                {
                                    System.out.println("Displaying Routes");

                                    for(int i = 0; i < currentRoute.size(); i++) {
                                         ArrayList<DirectionsRoute> currentRouteJ = currentRoute.get(i);
                                        for (int j = 0; j < currentRouteJ.size(); j++){
                                            allRoutes.add(currentRouteJ.get(j));
                                        }
                                    }
                                    navigationMapRoute.addRoutes(allRoutes);

                                    /*
                                    Snackbar snackbar = Snackbar.make(myLayout, "PM1    PM2.5    PM10    Distance\n" +
                                            df.format(route.getTotalPM1()) + "     "
                                            + df.format(route.getTotalPM2()) + "        "
                                            + df.format(route.getTotalPM10()) + "      "
                                            + df.format(route.getTotalDistance()) + " miles", Snackbar.LENGTH_INDEFINITE);
                                    View snackbarView = snackbar.getView();
                                    snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                                    snackbar.setAction("CLOSE", v -> {
                                    }).setActionTextColor(Color.BLACK).show();
                                    */
                                }
                            }

                            @Override
                            public void onFailure(Call<MapMatchingResponse> call, Throwable throwable) {
                                Timber.e(throwable, "MapboxMapMatching error");
                            }
                        });
            } catch (ServicesException servicesException) {
                Timber.e(servicesException, "MapboxMapMatching error");
                System.out.println("MapboxMapMatching error");
            }
        }
    }

    /*
     * Initialize heatmap Layer and Source ID
     */
    private void setHeatmapLayerAndSourceID(){
        HEATMAP_LAYER_ID.clear();
        AIRPOLLUTION_SOURCE_ID.clear();
        heatMapJson.clear();

        for(Route route : routes)
            heatMapJson.add(route.getRouteJson());

        for(int i = 0; i < routes.size(); i++){
            String hmLayerID = "HeatMapLayer-" + i;
            String hmSourceID = "HeatMapSource-" + i;
            HEATMAP_LAYER_ID.add(hmLayerID);
            AIRPOLLUTION_SOURCE_ID.add(hmSourceID);
        }
    }

    /*
     * show/hide heatmap layer
     */

    private void clearHeatMap(){
        Style style = mapboxMap.getStyle();
        try{
            System.out.println("remove Source and Layer");
            for(String hmLayerID : HEATMAP_LAYER_ID) {
                if(style.getLayer(hmLayerID) != null)
                    style.removeLayer(hmLayerID);
            }
            for(String airpolSourceID : AIRPOLLUTION_SOURCE_ID) {
                if(style.getSource(airpolSourceID) != null)
                    style.removeSource(airpolSourceID);
            }
            heatMapOn = false;
        }catch (Exception e){
        }
    }

    private void setShowHeatMap(@NonNull Style style){
        if(heatMapOn){
            try{
                System.out.println("remove Source and Layer");
                for(String hmLayerID : HEATMAP_LAYER_ID) {
                    if(style.getLayer(hmLayerID) != null)
                        style.removeLayer(hmLayerID);
                }
                for(String airpolSourceID : AIRPOLLUTION_SOURCE_ID) {
                   if(style.getSource(airpolSourceID) != null)
                        style.removeSource(airpolSourceID);
                }
                heatMapOn = false;
            }catch (Exception e){
            }
        }else {
            if(AIRPOLLUTION_SOURCE_ID.size() == 0){
                System.out.println("No hm available");
                Snackbar snackbar = Snackbar.make(myLayout, "No Heat Map available", Snackbar.LENGTH_INDEFINITE);
                View snackbarView = snackbar.getView();
                TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextColor(Color.BLACK);
                snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                snackbar.setAction("CLOSE", v -> {}).setActionTextColor(Color.BLACK).show();
                return;
            }

            for(int i = 0; i < AIRPOLLUTION_SOURCE_ID.size(); i++) {
                String airpolSourceID = AIRPOLLUTION_SOURCE_ID.get(i);
                String hmJson = heatMapJson.get(i);
                if (style.getSource(airpolSourceID) == null) {
                    System.out.println("Add air pollution Source" + airpolSourceID);
                    try {
                        if (hmJson != null) {
                            System.out.println("Add HeatMap");
                            style.addSource(new GeoJsonSource(airpolSourceID, hmJson));
                            heatMapOn = true;
                        } else {
                            System.out.println("No heatmap available");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(e.toString());
                    }
                }
            }
            for(int i = 0; i < HEATMAP_LAYER_ID.size(); i++) {
                String hmLayerID = HEATMAP_LAYER_ID.get(i);
                String airpolSourceID = AIRPOLLUTION_SOURCE_ID.get(i);
                if (style.getLayer(hmLayerID) == null) {
                    System.out.println("add Heat map Layer: " + hmLayerID);
                    if (style.getSource(airpolSourceID) != null)
                        addHeatmapLayer(style, hmLayerID, airpolSourceID);
                }
            }
        }
    }

    /*
     * Setting of heatmap layer
     */
    private void addHeatmapLayer(@NonNull Style loadedMapStyle, String hmLayerID, String airpolSourceID) {
        HeatmapLayer layer = new HeatmapLayer(hmLayerID, airpolSourceID);
        layer.setMaxZoom(18);
        layer.setProperties(
                // Color ramp for heatmap.  Domain is 0 (low) to 1 (high).
                // Begin color ramp at 0-stop with a 0-transparency color
                // to create a blur-like effect.
                heatmapColor(
                        interpolate(
                                linear(), heatmapDensity(),
                                literal(0), rgba(33, 102, 172, 0),
                                literal(0.2), rgb(103, 169, 207),
                                literal(0.4), rgb(209, 229, 240),
                                literal(0.6), rgb(253, 219, 199),
                                literal(0.8), rgb(239, 138, 98),
                                literal(1), rgb(178, 24, 43)
                        )
                ),
                // Increase the heatmap weight based on frequency and property magnitude
                heatmapWeight(
                        interpolate(
                                linear(), get("pm2.5"),
                                stop(0, 0),
                                stop(30, 1)
                        )
                ),

                // Increase the heatmap color weight weight by zoom level
                // heatmap-intensity is a multiplier on top of heatmap-weight
                heatmapIntensity(
                        interpolate(
                                linear(), zoom(),
                                stop(0, 1),
                                stop(20, 5)
                        )
                ),

                // Adjust the heatmap radius by zoom level
                heatmapRadius(
                        interpolate(
                                linear(), zoom(),
                                stop(0, 2),
                                stop(20, 20)
                        )
                ),

                // Transition from heatmap to circle layer by zoom level
                heatmapOpacity(1f
                )
        );

        loadedMapStyle.addLayerBelow(layer, "waterway-label");
    }

    /*
     * Destination marker
     */
    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        if(loadedMapStyle.getImage("destination-icon-id") == null) {
            loadedMapStyle.addImage("destination-icon-id",
                    BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        }
        if(loadedMapStyle.getSource(destinationSource_ID) == null) {
            GeoJsonSource geoJsonSource = new GeoJsonSource(destinationSource_ID);
            loadedMapStyle.addSource(geoJsonSource);
        }
        if(loadedMapStyle.getLayer(destinationSymbolLayer_ID) == null) {
            SymbolLayer destinationSymbolLayer = new SymbolLayer(destinationSymbolLayer_ID, destinationSource_ID);
            destinationSymbolLayer.withProperties(
                    iconImage("destination-icon-id"),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true)
            );
            loadedMapStyle.addLayer(destinationSymbolLayer);
        }
    }

    /*
     * Just for test
     */
    private void testPlanRoutes() throws Exception{
        Log.d(TAG, "Test case: navigation button clicked");
        Log.d(TAG, "Test case: Current Route Index: " + testRouteIndex);

        /*
        double sLat = 55.94485;
        double sLong = -3.19726;
        double tLat = 55.94120;
        double tLong = -3.18302;
        */
        double sLat = 55.94194;
        double sLong = -3.20329;
        double tLat = 55.94438;
        double tLong = -3.18622;


        originPoint = Point.fromLngLat(sLong, sLat);
        destinationPoint = Point.fromLngLat(tLong, tLat);
        List<Route> testRoutes = new ArrayList<>();
        testRoutes.add(DataIO.parseJson(routeJson.get(testRouteIndex)));
        routes = testRoutes;
        for(int i = 0; i < routes.size(); i++) {
            testPolTime = routes.get(i).getPolTime();
            requestMapMatched(routes.get(i), i + 1);
        }
        clearHeatMap();
        setHeatmapLayerAndSourceID();
        updateRouteTableInfo();
        currentRoute.clear();
        allRoutes.clear();
        //getTestDeviceLocation(13f, sLat, sLong);
        testRouteIndex++;
        if(testRouteIndex == routeJson.size()) {
            System.out.println("Reset testRouteIndex");
            testRouteIndex = 0;
        }
    }

    /*
     * Test: Load heatmap for an area
     */
    private class loadHM extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                InputStream is = getAssets().open("hmJson.txt");
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                StringBuilder total = new StringBuilder();
                int lineNumber = 0;
                for (String line; (line = r.readLine()) != null; ) {
                    if(lineNumber == testRouteIndex) {
                        heatMapJson.add(line);
                        System.out.println("Got HM: " + heatMapJson);
                        break;
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
                System.out.println(e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            System.out.println("Finished loading hm");
            System.out.println(heatMapJson);
        }
    }

    /*
     * Test: Load route Json
     */
    private class LoadRouteJson extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //System.out.println("Start loading hm");
            System.out.println("Start loading routeJson");
            routeJson = new ArrayList<>();
            try{
                InputStream is = getAssets().open(testRoutefile);
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                StringBuilder total = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    routeJson.add(line);
                    System.out.println(line);
                }
            }catch (IOException e){
                e.printStackTrace();
                System.out.println(e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            System.out.println("Finished loading routeJson");
        }
    }

    private class planTask extends AsyncTask<String, Boolean, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("Show progress bar");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... args) {
            try {
                boolean result =  planRoutes();
                publishProgress(result);
            }catch (Exception e ){
                System.out.println(e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            super.onProgressUpdate(values);
            System.out.println("Update Route on Map");
            updateRouteUI(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            System.out.println("Hide progress bar");
            progressDialog.dismiss();
        }

    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS,
                style -> {
                    System.out.println("onStyleLoad");
                    //Initial map settings
                    enableLocationComponent(style);
                    MainActivity.this.planRoute.setEnabled(true);
                    addDestinationIconSymbolLayer(style);
                    //mapboxMap.addOnMapClickListener(MainActivity.this);

                    // Hide mapbox logo
                    mapboxMap.getUiSettings().setAttributionEnabled(false);
                    mapboxMap.getUiSettings().setLogoEnabled(false);
                    mapboxMap.getUiSettings().setCompassMargins(0, 200, 10, 0);

                    // Initalize onClick listeners
                    init();
                    //new LoadRouteJson().execute();
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        /*L3
        double sLat = 55.94069;
        double sLong = -3.18082;
        double tLat = 55.94207;
        double tLong = -3.20076;

        originPoint = Point.fromLngLat(sLong,sLat);
        destinationPoint = Point.fromLngLat(tLong, tLat);
        */

        ///*
        destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
        //*/

        addDestinationIconSymbolLayer(mapboxMap.getStyle());
        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }
        addNavigationMapRoute();
        return true;
    }
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(this, loadedMapStyle);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
