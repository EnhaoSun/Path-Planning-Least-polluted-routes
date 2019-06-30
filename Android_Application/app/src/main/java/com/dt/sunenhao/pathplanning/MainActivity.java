package com.dt.sunenhao.pathplanning;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// classes needed to initialize map
import com.dt.sunenhao.pathplanning.Object.DataIO;
import com.dt.sunenhao.pathplanning.Object.Route;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
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
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.DirectionsRouteType;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, PermissionsListener {


    private MapView mapView;
    private MapboxMap mapboxMap;

    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;

    // variables for calculating and drawing a route
    private NavigationMapRoute navigationMapRoute;
    private ArrayList<DirectionsRoute> currentRoute;

    private Button startNav;
    private Button planRoute;
    private Button showHeatMap;

    private TextView pollution;
    private TextView usePollution_textView;

    private static final String AIRPOLLUTION_SOURCE_ID = "airpollution";
    private static final String HEATMAP_LAYER_ID = "airpollution-heat";
    //private static final String HEATMAP_LAYER_SOURCE = "airpollution";

    private static String heatMapJson = "";
    private static boolean usePollution = true;

    private Point originPoint = null;
    private Point destinationPoint = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getApplicationContext(), getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT > 9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);

        System.out.println("initiating button 1");
        startNav = (Button) findViewById(R.id.startButton);
        startNav.setOnClickListener((View v) ->
                //launchNavigationWithRoute()
                setUsePollution()
        );
        //startNav.setVisibility(View.INVISIBLE);
        startNav.setEnabled(true);
        startNav.setBackgroundResource(R.color.mapbox_blue);

        pollution = findViewById(R.id.pollution);
        pollution.setText("PM2 Level");

        usePollution_textView = findViewById(R.id.usePollution);
        usePollution_textView.setText("Use pollution: " + usePollution);

        System.out.println("initiating button 2");
        planRoute = findViewById(R.id.planRoute);
        planRoute.setOnClickListener((View v) ->
                {
                try {
                    planRoutes();
                }catch (Exception e) {
                    e.printStackTrace();
                }
        }
        );

        System.out.println("initiating button 3");
        showHeatMap = findViewById(R.id.showHeatMap);
        //showHeatMap.setVisibility(View.INVISIBLE);

        currentRoute = new ArrayList<>();

    }

    private void setUsePollution(){
        usePollution = !usePollution;
        usePollution_textView.setText("Use pollution: " + usePollution);
    }
    private void planRoutes() throws Exception{
        if(originPoint == null || destinationPoint == null)
            return;

        System.out.println("Start plan the route");
        double sLat = originPoint.latitude();
        double sLong = originPoint.longitude();
        double tLat = destinationPoint.latitude();
        double tLong = destinationPoint.longitude();

        System.out.println("/" + sLat + "/" + sLong + "/" + tLat + "/" + tLong);
        Route route = DataIO.getRoutes(originPoint, destinationPoint, usePollution, sLat, sLong, tLat, tLong);
        System.out.println(route.getTotalPollution());
        pollution.setText("PM2 Level " + route.getTotalPollution());
        requestMapMatched(route.getRoute());
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS,
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        System.out.println("onStyleLoad");
                        enableLocationComponent(style);
                        MainActivity.this.planRoute.setEnabled(true);
                        MainActivity.this.planRoute.setBackgroundResource(R.color.mapbox_blue);
                        addDestinationIconSymbolLayer(style);
                        mapboxMap.addOnMapClickListener(MainActivity.this);

                        new loadMapData(MainActivity.this).execute();
                        showHeatMap.setOnClickListener((View v) ->
                                setShowHeatMap(style)
                        );

                    }
                });
    }
    private void setShowHeatMap(@NonNull Style style){
        if(style.getSource(AIRPOLLUTION_SOURCE_ID) == null){
            System.out.println("addAirpollutionSource");
            try {
                if(heatMapJson != null) {
                    System.out.println("Add HeatMap");
                    style.addSource(new GeoJsonSource(AIRPOLLUTION_SOURCE_ID, heatMapJson));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(style.getLayer(HEATMAP_LAYER_ID) == null) {
            System.out.println("addHeatmapLayer");
            if(style.getSource(AIRPOLLUTION_SOURCE_ID) != null)
                addHeatmapLayer(style);
        }else{
            System.out.println("removeHeatmapLayer");
            style.removeLayer(HEATMAP_LAYER_ID);
        }
    }
    private void addHeatmapLayer(@NonNull Style loadedMapStyle) {
        HeatmapLayer layer = new HeatmapLayer(HEATMAP_LAYER_ID, AIRPOLLUTION_SOURCE_ID);
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
                                linear(), get("airpollution"),
                                stop(0, 0),
                                stop(250, 1)
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
    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }
    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPoint = Point.fromLngLat(-3.189228,55.942515);
        //originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
        //        locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }

        //getRoute(originPoint, destinationPoint);
        //startNav.setEnabled(true);
        //startNav.setBackgroundResource(R.color.mapbox_blue);
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

    private void cacheHeatMap(Context context){
        File file;
        String content = "";
        try{
            file = new File(context.getCacheDir(), "/cacheHeatMap");
            /*
            while(context.getCacheDir().listFiles().length > 0){
                if(context.getCacheDir().listFiles()[0].exists()){
                    System.out.println("Deleting");
                    context.getCacheDir().listFiles()[0].delete();
                }
            }
            */

            if(!file.exists()) {
                System.out.println("Creating cache file");
                System.out.println("Read Heatmap from cloud");
                try {
                    content = DataIO.getHeatMap();
                }catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(file.getAbsolutePath());
                FileOutputStream outputStream = new FileOutputStream(file, false);
                outputStream.write(content.getBytes());
                outputStream.close();
                System.out.println("Finished caching file");
                heatMapJson = content;
            }else{
                System.out.println("Ready to read from cache");
                FileInputStream inputStream = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(isr);
                while((content = bufferedReader.readLine())!=null){
                    heatMapJson += content;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("Cache file error: " + e.toString());
        }
    }

    private static class loadMapData extends AsyncTask<Void, Boolean, Void>{
        private WeakReference<MainActivity> weakReference;

        public loadMapData(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                MainActivity activity = weakReference.get();
                if (activity != null) {
                    activity.cacheHeatMap(activity.getApplicationContext());
                    System.out.println("Downloaded Heatmap");
                }
            }catch (Exception e){
                System.out.println(e.toString());
                System.out.println("Error loading map data from file");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MainActivity activity = weakReference.get();
            try {
                activity.showHeatMap.setEnabled(true);
                activity.showHeatMap.setBackgroundResource(R.color.mapbox_blue);
                System.out.println("Enabled the heatmap button");
            }catch (Exception e){
                System.out.println("Failed to enable route planner button");
            }
        }
    }


    private void requestMapMatched(ArrayList<Point> points) {
        System.out.println("Number of points: " + points.size());
        ///*
        for(int i = 0; i < points.size(); i++) {
         //   double lat = points.get(i).latitude();
         //   double longt = points.get(i).longitude();
        //    mapboxMap.addMarker(new MarkerOptions().position(new LatLng(lat, longt)));
            System.out.println(points.get(i).coordinates());
        }
        //*/
        //    System.out.println(points.get(i).coordinates());


        if (points.size() < 3)
            return;
        try {
            // Setup the request using a client.
            MapboxMapMatching.builder()
                    .accessToken(Objects.requireNonNull(Mapbox.getAccessToken()))
                    .coordinates(points)
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
                                currentRoute.clear();
                                for(int i = 0; i < response.body().matchings().size(); i++)
                                    currentRoute.add(response.body().matchings().get(i).toDirectionRoute());
                                System.out.println("Number of routes: " + response.body().matchings().size());
                            } else {
                                // If the response code does not response "OK" an error has occurred.
                                Timber.e("MapboxMapMatching failed with %s", response.code());
                                System.out.println("MapboxMapMatching failed");
                                return;
                            }
                            List<LegStep> totalLegStep = new ArrayList<>();
                            int routeLegs = 0;
                            for(int i = 0; i < currentRoute.size(); i++)
                                routeLegs += currentRoute.get(i).legs().size();

                            System.out.println("Current Route Legs: " + routeLegs);


                            for(int i = 0; i< currentRoute.size(); i++) {

                                for (RouteLeg leg : currentRoute.get(i).legs()) {
                                    for (LegStep legStep : leg.steps()) {
                                        totalLegStep.add(legStep);
                                    }
                                }
                                RouteLeg leg1 = RouteLeg.builder()
                                        .distance(currentRoute.get(i).distance())
                                        .duration(currentRoute.get(i).duration())
                                        .steps(totalLegStep)
                                        .build();

                                currentRoute.get(i).legs().clear();
                                currentRoute.get(i).legs().add(leg1);
                            }


                            // Draw the route on the map
                            if (navigationMapRoute != null) {
                                //navigationMapRoute.removeRoute();
                                navigationMapRoute.updateRouteArrowVisibilityTo(false);
                                navigationMapRoute.updateRouteVisibilityTo(false);
                            } else {
                                navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute, "natural-line-label");
                            }
                            navigationMapRoute.addRoutes(currentRoute);
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
        //startNav.setEnabled(true);
        //startNav.setBackgroundResource(R.color.mapbox_blue);
    }


}
