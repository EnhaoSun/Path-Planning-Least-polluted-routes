package com.dt.sunenhao.pathplanning.Object;

import com.mapbox.geojson.Point;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DataIO {

    public static Route getRoutes(Point orign, Point dest, boolean usePollution, double sLat, double sLong, double tLat, double tLong) throws Exception{
        Route route = new Route(0, null);

        ArrayList<Point> routes = new ArrayList<>();

        URL url = new URL("https://enhao-244310.appspot.com/routes/" +
                        usePollution + "/" + sLat + "/" + sLong + "/" + tLat + "/" + tLong);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        System.out.println(con.getResponseCode());

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String line = bufferedReader.readLine();
        try{
            route.setTotalPollution(Double.parseDouble(line));
        }catch (Exception e){
            System.out.println(e);
        }

        while ((line = bufferedReader.readLine()) != null) {
            String [] coo = line.split("\\s+");
            routes.add(Point.fromLngLat(Double.parseDouble(coo[1]), Double.parseDouble(coo[0])));
        }
        route.setRoute(routes);
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
