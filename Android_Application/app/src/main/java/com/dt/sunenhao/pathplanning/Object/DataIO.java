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

public class DataIO {

    public static Route getRoutes(Point orign, Point dest, boolean usePollution, double sLat, double sLong, double tLat, double tLong) throws Exception{
        ArrayList<Point> routes = new ArrayList<>();
        URL url = new URL("https://enhao-244311.appspot.com/routes/" +
                        usePollution + "/" + sLat + "/" + sLong + "/" + tLat + "/" + tLong);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        System.out.println(con.getResponseCode());

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String line;
        String routeJson = "";

        while ((line = bufferedReader.readLine()) != null) {
            routeJson += line;
        }
        //System.out.println(routeJson);
        return parseJson(routeJson);
    }

    public static Route parseJson(String routeJson){
        Route route = new Route(null, routeJson, 0, 0, 0);
        ArrayList<Point> nodes = new ArrayList<>();
        double total_PM1 = 0;
        double total_PM2 = 0;
        double total_PM10 = 0;
        try {
            JSONObject obj = new JSONObject(routeJson);
            JSONArray features = (JSONArray) obj.get("features");
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
                //System.out.print("[" + coordinates.get(0) + ", " + coordinates.get(1) + "]");
                //System.out.println(airpollution.get("pm1").toString() + ";" + airpollution.get("pm2.5") + ";" + airpollution.get("pm10"));
            }
            route.setRoute(nodes);
            route.setAver_PM1(total_PM1 / features.length());
            route.setAver_PM2(total_PM2 / features.length());
            route.setAver_PM10(total_PM10 / features.length());
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
