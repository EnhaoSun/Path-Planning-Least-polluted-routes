package com.dt.sunenhao.pathplanning;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

// classes needed to initialize map
import com.dt.sunenhao.pathplanning.Object.DataIO;
import com.dt.sunenhao.pathplanning.Object.Route;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.mapboxsdk.Mapbox;
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


    private MapView mapView;
    private MapboxMap mapboxMap;

    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;

    // variables for calculating and drawing a route
    private NavigationMapRoute navigationMapRoute;
    private ArrayList<DirectionsRoute> currentRoute;

    private Switch usePollution_button;
    private Button planRoute;
    private ImageButton showHeatMap;
    private boolean heatMapOn = false;

    private TextView usePollution_textView;

    private static final String AIRPOLLUTION_SOURCE_ID = "airpollution";
    private static final String HEATMAP_LAYER_ID = "airpollution-heat";

    private static String heatMapJson = "";
    private static boolean usePollution = true;

    private Point originPoint = null;
    private Point destinationPoint = null;
    private DecimalFormat df = new DecimalFormat("#.##");

    private Route route;

    CoordinatorLayout myLayout;


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

        df.setRoundingMode(RoundingMode.HALF_UP);

        myLayout = (CoordinatorLayout) findViewById(R.id.myLayout);

        usePollution_button = findViewById(R.id.usePollution);
        usePollution_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // The toggle is enabled
                usePollution = true;
            } else {
                // The toggle is disabled
                usePollution = false;
            }
        });

        usePollution_textView = findViewById(R.id.usePollution);
        usePollution_textView.setText("Use pollution: " + usePollution);

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
        showHeatMap = findViewById(R.id.showHeatMap);
        showHeatMap.setEnabled(true);

        currentRoute = new ArrayList<>();
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
        route = DataIO.getRoutes(originPoint, destinationPoint, usePollution, sLat, sLong, tLat, tLong);
        System.out.println(route.getRouteJson());
        //System.out.println("PM2 Level" + route.getTotalPollution());
        requestMapMatched(route.getRoute());
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS,
                style -> {
                    System.out.println("onStyleLoad");
                    enableLocationComponent(style);
                    MainActivity.this.planRoute.setEnabled(true);
                    addDestinationIconSymbolLayer(style);
                    mapboxMap.addOnMapClickListener(MainActivity.this);

                    showHeatMap.setOnClickListener((View v) ->
                            setShowHeatMap(style)
                    );

                });
    }
    private void setShowHeatMap(@NonNull Style style){
        if(heatMapOn){
            try{
                System.out.println("remove Source and Layer");
                style.removeLayer(HEATMAP_LAYER_ID);
                style.removeSource(AIRPOLLUTION_SOURCE_ID);
                heatMapOn = false;
            }catch (Exception e){
            }
        }else {
            if (style.getSource(AIRPOLLUTION_SOURCE_ID) == null) {
                System.out.println("addAirpollutionSource");
                try {
                    if (route.getRouteJson() != null) {
                        heatMapJson = route.getRouteJson();
                        System.out.println("Add HeatMap");
                        style.addSource(new GeoJsonSource(AIRPOLLUTION_SOURCE_ID, heatMapJson));
                        heatMapOn = true;
                    } else {
                        System.out.println("No heatmap available");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (style.getLayer(HEATMAP_LAYER_ID) == null) {
                System.out.println("addHeatmapLayer");
                if (style.getSource(AIRPOLLUTION_SOURCE_ID) != null)
                    addHeatmapLayer(style);
            } else {
                System.out.println("removeHeatmapLayer");
                style.removeLayer(HEATMAP_LAYER_ID);
                heatMapOn = false;
            }
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
                                linear(), get("pm2.5"),
                                stop(0, 0),
                                stop(10, 1)
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
        //originPoint = Point.fromLngLat(-3.189228,55.942515);
        originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }
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

    private void requestMapMatched(ArrayList<Point> points) {

        if (navigationMapRoute == null) {
            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);//, "natural-line-label");
        }else{
            navigationMapRoute.updateRouteArrowVisibilityTo(false);
            navigationMapRoute.updateRouteVisibilityTo(false);
        }

        System.out.println("Number of points: " + points.size());
        /*
        for(int i = 0; i < points.size(); i++) {
            System.out.println(points.get(i).coordinates());
        }
        */
        if (points.size() < 2){
            Snackbar snackbar = Snackbar.make(myLayout, "Unable to get there. Please try again!", Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();
            TextView tv = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
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
        }else{
            trace = new int[1];
            trace[0] = 0;
        }
        for(int i = 0; i<trace.length; i++)
            System.out.print(trace[i] + " ");
        System.out.println();


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
                                        currentRoute.add(response.body().matchings().get(i).toDirectionRoute());
                                    System.out.println("Number of route points: " + sub.size());
                                } else {
                                    // If the response code does not response "OK" an error has occurred.
                                    Timber.e("MapboxMapMatching failed with %s", response.code());
                                    System.out.println("MapboxMapMatching failed with " + response.toString());
                                    return;
                                }
                                List<LegStep> totalLegStep = new ArrayList<>();
                                int routeLegs = 0;
                                for (int i = 0; i < currentRoute.size(); i++)
                                    routeLegs += currentRoute.get(i).legs().size();

                                if(currentRoute.size() == trace.length) {
                                    double totalDistance = 0;
                                    for (int i = 0; i < currentRoute.size(); i++) {

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
                                        totalDistance += currentRoute.get(i).distance();
                                    }
                                    totalDistance = totalDistance / 1609.344;

                                    System.out.println("Total Distance: " );
                                    System.out.println("Current Route Size: " + currentRoute.size());
                                    System.out.println("Displaying Routes");
                                    navigationMapRoute.addRoutes(currentRoute);

                                    Snackbar snackbar = Snackbar.make(myLayout, "PM1     PM2.5    PM10     Distance\n"+
                                                                    df.format(route.getAver_PM1()) +  "    "
                                                                    +df.format(route.getAver_PM2()) + "    "
                                                                    +df.format(route.getAver_PM10()) +"    "
                                                                    + df.format(totalDistance) + " miles", Snackbar.LENGTH_INDEFINITE);
                                    View snackbarView = snackbar.getView();
                                    snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                                    snackbar.setAction("CLOSE", v -> {
                                    }).setActionTextColor(Color.BLACK).show();
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
        currentRoute.clear();
    }
}
