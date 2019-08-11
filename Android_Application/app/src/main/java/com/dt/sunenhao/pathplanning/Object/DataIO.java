package com.dt.sunenhao.pathplanning.Object;

import com.mapbox.geojson.Point;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataIO {

    public static List<Route> getRoutes(boolean usePollution, boolean updatePM, boolean alternative, double sLat, double sLong, double tLat, double tLong) throws Exception{
        //Your server address
        URL url = new URL("https://enhao-244311.appspot.com/routes/" +
                        alternative + "/" + updatePM + "/" +
                        usePollution + "/" + sLat + "/" + sLong + "/" + tLat + "/" + tLong);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        System.out.println(con.getResponseCode());
        if(con.getResponseCode() != 200)
            return null;

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String line;
        ArrayList<String> routeJson = new ArrayList<>();

        while ((line = bufferedReader.readLine()) != null) {
            routeJson.add(line);
            System.out.println(routeJson);
        }
        List<Route> routes = new ArrayList<>();
        for(int i = 0; i < routeJson.size(); i++)
            routes.add(parseJson(routeJson.get(i)));
        return routes;
    }

    public static Route parseJson(String routeJson){
        Route route = new Route(null, 0, 0, 0, 0, routeJson);
        ArrayList<Point> nodes = new ArrayList<>();
        double total_PM1 = 0;
        double total_PM2 = 0;
        double total_PM10 = 0;
        try {
            JSONObject obj = new JSONObject(routeJson);
            JSONArray features = (JSONArray) obj.get("features");
            JSONObject crs = (JSONObject) obj.get("crs");
            JSONObject properties = (JSONObject) crs.get("properties");
            double distance = (double) properties.get("distance");
            //Just for test
            //String polTime = (String) properties.get("polTime");
            //route.setPolTime(polTime);
            System.out.println(features.length());
            for (int i = 0; i < features.length(); i++) {
                JSONObject node = (JSONObject) features.get(i);
                JSONObject geometry = node.getJSONObject("geometry");
                JSONObject airpollution = node.getJSONObject("properties");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                nodes.add(Point.fromLngLat(coordinates.getDouble(0), coordinates.getDouble(1)));
                total_PM1 += airpollution.getDouble("pm1");
                total_PM2 += airpollution.getDouble("pm2.5");
                total_PM10 += airpollution.getDouble("pm10");
            }
            route.setRoute(nodes);
            route.setTotalPM1(total_PM1/nodes.size());
            route.setTotalPM2(total_PM2/nodes.size());
            route.setTotalPM10(total_PM10/nodes.size());
            route.setTotalDistance(distance);

        }catch (JSONException e){
            System.out.println(e.toString());
        }
        return route;
    }

    public static String getHeatMap() throws Exception{
        URL url = new URL("https://enhao-244310.appspot.com/heatMap");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        System.out.println(con.getResponseCode());

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String line;
        String heatMapJson = "";
        while((line = bufferedReader.readLine()) != null){
            heatMapJson += line;
        }
        return heatMapJson;
    }
}
