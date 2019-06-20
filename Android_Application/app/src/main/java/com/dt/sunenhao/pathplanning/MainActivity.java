package com.dt.sunenhao.pathplanning;

import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

// classes needed to initialize map
import com.dt.sunenhao.pathplanning.Object.DataIO;
import com.dt.sunenhao.pathplanning.Object.MyPoint;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
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
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
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
    private DirectionsRoute currentRoute;

    private Button startNav;
    private Button planRoute;
    private Button showHeatMap;

    private static final String AIRPOLLUTION_SOURCE_ID = "airpollution";
    private static final String HEATMAP_LAYER_ID = "airpollution-heat";
    private static final String HEATMAP_LAYER_SOURCE = "airpollution";

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
                launchNavigationWithRoute()
        );


        System.out.println("initiating button 2");
        planRoute = findViewById(R.id.planRoute);
        planRoute.setOnClickListener((View v) ->
                planRoutes()
        );

        System.out.println("initiating button 3");
        showHeatMap = findViewById(R.id.showHeatMap);

    }

    private void planRoutes(){
        double sLat = 55.940639;
        double sLong = -3.182709;

        double tLat = 55.944941;
        double tLong = -3.194339;
        RoutePlanner.findRoute(sLat, sLong, tLat, tLong);
        requestMapMatched(RoutePlanner.getRoutePoints());
    }

    private void launchNavigationWithRoute(){
        System.out.println(currentRoute);
        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute).build();
        NavigationLauncher.startNavigation(MainActivity.this, options);
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
                        //addDestinationIconSymbolLayer(style);
                        //mapboxMap.addOnMapClickListener(MainActivity.this);
                        //new LoadGeoJson(MainActivity.this).execute();

                        //for (Layer singleLayer : mapboxMap.getStyle().getLayers())
                        //    System.out.println("onMapReady: layer id = " + singleLayer.getId());

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
                List<MyPoint> totalAirPollutionPoints = DataIO.readAirpollution();
                totalAirPollutionPoints.addAll(RoutePlanner.getPoints());
                style.addSource(new GeoJsonSource(AIRPOLLUTION_SOURCE_ID, DataIO.toJson(totalAirPollutionPoints)));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(style.getLayer(HEATMAP_LAYER_ID) == null) {
            System.out.println("addHeatmapLayer");
            addHeatmapLayer(style);
        }else{
            System.out.println("removeHeatmapLayer");
            style.removeLayer(HEATMAP_LAYER_ID);
        }
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

        Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }
        getRoute(originPoint, destinationPoint);
        startNav.setEnabled(true);
        startNav.setBackgroundResource(R.color.mapbox_blue);
        return true;
    }
    private void getRoute(Point origin, Point destination) {
        ///*
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .language(new Locale("en"))
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        Timber.d("Response code: " + response.code());
                        if (response.body() == null) {
                            Timber.e("No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Timber.e("No routes found");
                            return;
                        }
                        currentRoute = response.body().routes().get(0);
                        System.out.println("Navg: " + currentRoute.toString());
                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Timber.e("Error: " + throwable.getMessage());
                    }
                });
        //*/
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


    private static class LoadGeoJson extends AsyncTask<Void, Void, FeatureCollection> {

        private WeakReference<MainActivity> weakReference;

        LoadGeoJson(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        protected FeatureCollection doInBackground(Void... voids) {
            try {
                MainActivity activity = weakReference.get();
                if (activity != null) {
                    InputStream inputStream = activity.getAssets().open("test-area-lines.geojson");
                    return FeatureCollection.fromJson(convertStreamToString(inputStream));
                }
            } catch (Exception exception) {
                Timber.e("Exception Loading GeoJSON: %s" , exception.toString());
            }
            return null;
        }

        static String convertStreamToString(InputStream is) {
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }

        @Override
        protected void onPostExecute(@Nullable FeatureCollection featureCollection) {
            super.onPostExecute(featureCollection);
            MainActivity activity = weakReference.get();
            if (activity != null && featureCollection != null) {
                activity.drawLines(featureCollection);
            }
        }
    }

    private void drawLines(@NonNull FeatureCollection featureCollection) {
        List<Feature> features = featureCollection.features();
        if (features != null && features.size() > 0) {
            Feature feature = features.get(0);
            requestMapMatched1(feature);
        }
    }
    private void requestMapMatched1(Feature feature) {
        List<Point> points = ((LineString) Objects.requireNonNull(feature.geometry())).coordinates();

        try {
            // Setup the request using a client.
            MapboxMapMatching.builder()
                    .accessToken(Objects.requireNonNull(Mapbox.getAccessToken()))
                    .profile(DirectionsCriteria.PROFILE_WALKING)
                    .voiceInstructions(true)
                    .bannerInstructions(true)
                    .coordinates(points)
                    .steps(true)
                    .build()
                    .enqueueCall(new Callback<MapMatchingResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<MapMatchingResponse> call,
                                               @NonNull Response<MapMatchingResponse> response) {
                            System.out.println("In mapbox Respond");
                            if (response.isSuccessful()) {
                                System.out.println("Signing currentRoute");
                                currentRoute = response.body().matchings().get(0).toDirectionRoute();
                                System.out.println(currentRoute.legs().get(0).steps().get(0));
                            } else {
                                // If the response code does not response "OK" an error has occurred.
                                Timber.e("MapboxMapMatching failed with %s", response.code());
                                System.out.println("MapboxMapMatching failed");
                                return;
                            }

                            // Draw the route on the map
                            if (navigationMapRoute != null) {
                                navigationMapRoute.removeRoute();
                            } else {
                                navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                            }

                            List<LegStep> totalLegStep = new ArrayList<>();
                            for(RouteLeg leg: currentRoute.legs()){
                                for(LegStep legStep : leg.steps()){
                                    totalLegStep.add(legStep);
                                }
                            }
                            RouteLeg leg1 = RouteLeg.builder()
                                    .distance(currentRoute.distance())
                                    .duration(currentRoute.duration())
                                    .steps(totalLegStep)
                                    .build();

                            currentRoute.legs().clear();
                            currentRoute.legs().add(leg1);

                            navigationMapRoute.addRoute(currentRoute);
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
        startNav.setEnabled(true);
        startNav.setBackgroundResource(R.color.mapbox_blue);

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
                    System.out.println("Start do in background");
                    System.out.println("Load airpollution");
                    RoutePlanner.setAirpollution(DataIO.readAirpollution());
                    System.out.println("Load map data");
                    RoutePlanner.initialiseGrouph(activity.getAssets(), true);
                    RoutePlanner.setAirPollutionReady(true);
                    publishProgress(true);
                    RoutePlanner.setPlannerReady(true);
                    System.out.println("planner Ready: " + RoutePlanner.isPlannerReady());
                }
            }catch (Exception e){
                System.out.println(e.toString());
                System.out.println("Error loading map data from file");
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            MainActivity activity = weakReference.get();
            if(values.length > 0){
                if(values[0]){
                    if(RoutePlanner.isAirPollutionReady()){
                        System.out.println("Enable the HeatMap button");
                        activity.showHeatMap.setEnabled(true);
                        activity.showHeatMap.setBackgroundResource(R.color.mapbox_blue);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MainActivity activity = weakReference.get();
            try {

                if (RoutePlanner.isPlannerReady()) {
                    System.out.println("Enable the planner button");
                    activity.planRoute.setEnabled(true);
                    activity.planRoute.setBackgroundResource(R.color.mapbox_blue);
                }
            }catch (Exception e){
                System.out.println("Failed to enable route planner button");
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
                                linear(), get("airpollution"),
                                stop(0, 0),
                                stop(RoutePlanner.getMaxPollution(), 1)
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

    private void requestMapMatched(ArrayList<Point> points) {
        System.out.println("Number of points: " + points.size());
        ///*
        try {
            // Setup the request using a client.
            MapboxMapMatching.builder()
                    .accessToken(Objects.requireNonNull(Mapbox.getAccessToken()))
                    .profile(DirectionsCriteria.PROFILE_WALKING)
                    .voiceInstructions(true)
                    .bannerInstructions(true)
                    .coordinates(points)
                    .steps(true)
                    .build()
                    .enqueueCall(new Callback<MapMatchingResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<MapMatchingResponse> call,
                                               @NonNull Response<MapMatchingResponse> response) {
                            System.out.println("In mapbox Respond");
                            if (response.isSuccessful()) {
                                System.out.println("Signing currentRoute");
                                currentRoute = response.body().matchings().get(0).toDirectionRoute();
                                System.out.println(currentRoute.legs().get(0).steps().get(0));
                            } else {
                                // If the response code does not response "OK" an error has occurred.
                                Timber.e("MapboxMapMatching failed with %s", response.code());
                                System.out.println("MapboxMapMatching failed");
                                return;
                            }

                            List<LegStep> totalLegStep = new ArrayList<>();
                            for(RouteLeg leg: currentRoute.legs()){
                                for(LegStep legStep : leg.steps()){
                                    totalLegStep.add(legStep);
                                }
                            }
                            RouteLeg leg1 = RouteLeg.builder()
                                    .distance(currentRoute.distance())
                                    .duration(currentRoute.duration())
                                    .steps(totalLegStep)
                                    .build();

                            currentRoute.legs().clear();
                            currentRoute.legs().add(leg1);


                            // Draw the route on the map
                            if (navigationMapRoute != null) {
                                navigationMapRoute.removeRoute();
                            } else {
                                navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute, "natural-line-label");
                            }
                            navigationMapRoute.addRoute(currentRoute);
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
        //*/
        //startNav.setEnabled(true);
        //startNav.setBackgroundResource(R.color.mapbox_blue);
    }


}
