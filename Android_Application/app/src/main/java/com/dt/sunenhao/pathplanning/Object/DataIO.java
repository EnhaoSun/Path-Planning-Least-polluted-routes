package com.dt.sunenhao.pathplanning.Object;

import android.content.res.AssetManager;

import com.dt.sunenhao.pathplanning.RoutePlanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import smile.neighbor.CoverTree;

public class DataIO {

    public static List<MyPoint> getPollutionFromNeighbor(List<MyPoint> points, CoverTree coverTree, List<MyPoint> gridPoints){
        for(MyPoint p : points){
            p.setPM2(gridPoints.get(coverTree.nearest(p).index).getPM2());
        }
        return points;
    }

    public static List<MyPoint> readGridWithPollution(AssetManager assetManager){
        System.out.println("Loading grid with pollution");
        List<MyPoint> points = new ArrayList<>();
        InputStream inputStream;
        Scanner scanner;
        try{
            // Read the grid points
            inputStream = assetManager.open("grid.txt");
            scanner = new Scanner(inputStream);
            String line;

            while(scanner.hasNext()){
                line = scanner.nextLine();
                String[] geo = line.replaceAll("\\(|\\)|,", "").split("\\s+");
                /* first(0,1), third(4,5)
                   each line is a grid
                   use the first point and the third can calculate the center of the grid
                   get the center of each grid
                */
                double lat = (Double.parseDouble(geo[1]) + Double.parseDouble(geo[5]))/2;
                double lon = (Double.parseDouble(geo[0]) + Double.parseDouble(geo[4]))/2;
                points.add(new MyPoint(lat, lon));
            }
            // Read the pollution corresponding to each grid center
            inputStream = assetManager.open("PM2.5_prediction.txt");
            scanner = new Scanner(inputStream);
            int index = 0;
            while(scanner.hasNext()) {
                line = scanner.nextLine();
                String[] pm = line.split("\\s+");
                for(int i = 0; i < pm.length; i++)
                    points.get(index*20 + i).setPM2(Double.parseDouble(pm[i]));
                index++;
            }

        }catch (IOException e){
            e.printStackTrace();
        }

        System.out.println("Finished Loading grid with pollution");
        return points;
    }

    public static List<MyPoint> readPointsWithID(AssetManager assetManager){
        System.out.println("Loading points with ID");
        List<MyPoint> points = new ArrayList<>();
        InputStream inputStream = null;
        Scanner scanner = null;
        try{
            inputStream = assetManager.open("node.txt");
            scanner = new Scanner(inputStream).useDelimiter(":\\s+");
            String line;
            int index = 0;
            while (scanner.hasNext()) {
                line = scanner.nextLine();
                String[]nums = line.split("\\s+");
                // index: latitude longtitude
                double latitude = Double.parseDouble(nums[1]);
                double longtitude = Double.parseDouble(nums[2]);
                MyPoint p = new MyPoint(latitude, longtitude);
                p.setIndex(index);
                points.add(p);
                index++;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if(inputStream != null)
                    inputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
            if(scanner != null)
                scanner.close();
        }
        return points;
    }

    public static Map<Integer, List<Integer>> readAdjacencyList(AssetManager assetManager){
        System.out.println("Loading Adjacency ");
        Map<Integer, List<Integer>> neighbors = new HashMap<>();
        InputStream inputStream = null;
        Scanner scanner = null;
        try {
            inputStream = assetManager.open("adjacency.txt");
            scanner = new Scanner(inputStream).useDelimiter(":,");
            String line;
            int i = 0;

            while(scanner.hasNext()) {
                line = scanner.nextLine();
                neighbors.put(i, new ArrayList<Integer>());
                String[] divided = line.split(":");
                divided[1] = divided[1].substring(1);
                String[] nums = divided[1].split("\\s+");
                for(String num : nums) {
                    if (num.matches("[0-9]+")) {
                        int p = Integer.parseInt(num);
                        neighbors.get(i).add(p);
                    }
                }
                i++;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
            if(scanner != null)
                scanner.close();
        }
        return neighbors;
    }

    public static List<MyPoint> readAirpollution() throws Exception {
        List<MyPoint> points = new ArrayList<>();

        String startDate = "20180629";
        String endDate = "20180630";
        String sid = "XXM008";

        String userName = "jupyter";
        String password = "d84hr75yG4fE";

        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);


        URL url = new URL("https://dashboard.specknet.uk/login");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("referer", "https://dashboard.specknet.uk");
        con.setDoOutput(true);

        String auth = "username=" + URLEncoder.encode(userName, "UTF-8")+
                "&password= " + URLEncoder.encode(password, "UTF-8")+
                "&form.submitted=" + URLEncoder.encode("Login", "UTF-8");
        OutputStream os = con.getOutputStream();
        os.write(auth.getBytes());
        os.flush();
        os.close();

        System.out.println(con.getResponseCode());

        url = new URL("https://dashboard.specknet.uk/downloadPersonalAirspeck/"
                + sid + "/" + startDate + "/" + endDate);
        HttpURLConnection resumeConnection = (HttpURLConnection) url
                .openConnection();
        resumeConnection.connect();

        System.out.println(resumeConnection.getResponseCode());
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(resumeConnection.getInputStream()));
        String line = bufferedReader.readLine();
        while ((line = bufferedReader.readLine()) != null) {
            String[] data = line.split(",");
            MyPoint p = new MyPoint(Double.parseDouble(data[22]), Double.parseDouble(data[23]));
            double pm1 = Double.parseDouble(data[1]);
            double pm2 = Double.parseDouble(data[2]);
            double pm10 = Double.parseDouble(data[3]);
            p.setPM1(pm1);
            p.setPM2(pm2);
            p.setPM10(pm10);
            points.add(p);

            if(pm10 > RoutePlanner.getMaxPollution())
                RoutePlanner.setMaxPollution(pm10);
        }
        return points;
    }

    public static String toJson(List<MyPoint> array){
        JSONObject featureCollection = new JSONObject();
        try {
            featureCollection.put("type", "FeatureCollection");
            JSONObject properties = new JSONObject();
            properties.put("name", "Airpollution-Geo");
            JSONObject crs = new JSONObject();
            crs.put("type", "name");
            crs.put("properties", properties);
            featureCollection.put("crs", crs);

            JSONArray features = new JSONArray();

            for (int i = 0; i < array.size(); i++) {

                JSONObject geometry = new JSONObject();
                JSONObject airpollution = new JSONObject();
                JSONArray jsonArrayCoord = new JSONArray();
                JSONObject newFeature = new JSONObject();
                jsonArrayCoord.put(0, array.get(i).getLongitude());
                jsonArrayCoord.put(1, array.get(i).getLatitude());

                geometry.put("type", "Point");
                geometry.put("coordinates", jsonArrayCoord);

                airpollution.put("airpollution", array.get(i).getPM2());

                newFeature.put("type", "Feature");
                newFeature.put("geometry", geometry);
                newFeature.put("properties", airpollution);

                features.put(newFeature);
                featureCollection.put("features", features);
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
        return featureCollection.toString();
    }
}
