package com.example.demo.MapService;

import smile.neighbor.CoverTree;
import org.json.JSONArray;
import org.json.JSONObject; 
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Enhao Sun
 * @version 2019-06-11.
 */
public class DataIO {
    private static String nodeFile = "node_ed.txt";
    private static String adjaFile = "adjacency_ed.txt";
    private static String pollution = "PM_gen.txt";
    private static String grid = "gridLarge.txt";

    private static double MIN_PM1 = 0.16;
    private static double MAX_PM1 = 2.0;
    private static double MIN_PM2 = 1.0;
    private static double MAX_PM2 = 29.0;
    private static double MIN_PM10 = 1.0;
    private static double MAX_PM10 = 5.0;
    private static Random r = new Random(2019);

    public static String toJson(List<Point> array){
        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");
        JSONObject properties = new JSONObject();
        properties.put("name", "Airpollution-Geo");
        JSONObject crs = new JSONObject();
        crs.put("type", "name");
        crs.put("properties", properties);
        featureCollection.put("crs", crs);

        JSONArray features = new JSONArray();

        for(int i = 0; i < array.size(); i++){

            JSONObject geometry = new JSONObject();
            JSONObject airpollution = new JSONObject();
            JSONArray jsonArrayCoord = new JSONArray();
            JSONObject newFeature = new JSONObject();
            jsonArrayCoord.put(0,array.get(i).getLongitude());
            jsonArrayCoord.put(1,array.get(i).getLatitude());

            geometry.put("type", "Point");
            geometry.put("coordinates", jsonArrayCoord);

            //airpollution.put("airpollution", array.get(i).getPM2());
            airpollution.put("pm1", array.get(i).getPM1());
            airpollution.put("pm2.5", array.get(i).getPM2());
            airpollution.put("pm10", array.get(i).getPM10());

            newFeature.put("type", "Feature");
            newFeature.put("geometry", geometry);
            newFeature.put("properties", airpollution);

            features.put(newFeature);
            featureCollection.put("features", features);
        }
        return featureCollection.toString();
    }

    public static List<Point> getPollutioFromNeighbor(List<Point> points, CoverTree coverTree, List<Point> gridPoints){
        for(Point p : points){
            p.setPM2(gridPoints.get(coverTree.nearest(p).index).getPM2());
        }
        return points;
    }

    public static List<Point> readGridWithPollution(){
        ClassLoader classLoader = DataIO.class.getClassLoader();
        List<Point> points = new ArrayList<>();

        InputStream inputStream_pollution = classLoader.getResourceAsStream(pollution);
        InputStream inputStream_grid = classLoader.getResourceAsStream(grid);

        try{
            // Read the grid points
            //BufferedReader br = new BufferedReader(new FileReader(String.valueOf(grid.toPath())));
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream_grid));
            String line;
            while((line = br.readLine())!=null){
                String[] geo = line.replaceAll("\\(|\\)|,", "").split("\\s+");
                /* first(0,1), third(4,5)
                   each line is a grid
                   use the first point and the third can calculate the center of the grid
                   get the center of each grid
                */
                double lat = (Double.parseDouble(geo[1]) + Double.parseDouble(geo[5]))/2;
                double lon = (Double.parseDouble(geo[0]) + Double.parseDouble(geo[4]))/2;
                points.add(new Point(lat, lon));
            }
            // Read the pollution corresponding to each grid center
            br = new BufferedReader(new InputStreamReader(inputStream_pollution));
            int index = 0;
            while((line = br.readLine())!=null) {
                String[] pm = line.split("\\s+");
                for(int i = 0; i < pm.length; i++)
                    points.get(index*160 + i).setPM2(Double.parseDouble(pm[i]));
                index++;
            }

        }catch (IOException e){
            e.printStackTrace();
        }
        return points;
    }

    public static List<Point> readPointsWithID(){
        ClassLoader classLoader = DataIO.class.getClassLoader();
        //File file = new File(classLoader.getResource(nodeFile).getFile());
        List<Point> points = new ArrayList<>();

        InputStream inputStream = classLoader.getResourceAsStream(nodeFile);
        try{
            //BufferedReader br = new BufferedReader(new FileReader(String.valueOf(file.toPath())));
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int index = 0;
            while ((line = br.readLine()) != null) {
                String[]nums = line.split("\\s+");
                // index: latitude longtitude
                double latitude = Double.parseDouble(nums[1]);
                double longtitude = Double.parseDouble(nums[2]);
                Point p = new Point(latitude, longtitude);
                p.setIndex(index);
                p.setPM1(MIN_PM1 + (MAX_PM1 - MIN_PM1) * r.nextDouble());
                //p.setPM2(MIN_PM2 + (MAX_PM2 - MIN_PM2) * r.nextDouble());
                p.setPM10(MIN_PM10 + (MAX_PM10 - MIN_PM10) * r.nextDouble());
                points.add(p);
                index++;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return points;
    }

    public static Map<Integer, List<Integer>> readAdjacencyList(){
        Map<Integer, List<Integer>> neighbors = new HashMap<>();
        ClassLoader classLoader = DataIO.class.getClassLoader();
        //File file = new File(classLoader.getResource(adjaFile).getFile());
        InputStream inputStream = classLoader.getResourceAsStream(adjaFile);
        try {
            //BufferedReader br = new BufferedReader(new FileReader(file.getPath()));
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int index = 0;

            while((line = br.readLine()) != null) {
                String[] divided = line.split(":");
                index = Integer.parseInt(divided[0]);
                neighbors.put(index, new ArrayList<Integer>());
                divided[1] = divided[1].substring(1);
                String[] nums = divided[1].split("\\s+");
                for(String num : nums) {
                    int p = Integer.parseInt(num);
                    neighbors.get(index).add(p);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return neighbors;
    }

    public static List<Point> readAirpollution() throws Exception {
        List<Point> points = new ArrayList<>();

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
            Point p = new Point(Double.parseDouble(data[22]), Double.parseDouble(data[23]));
            double pm1 = Double.parseDouble(data[1]);
            double pm2 = Double.parseDouble(data[2]);
            double pm10 = Double.parseDouble(data[3]);
            p.setPM1(pm1);
            p.setPM2(pm2);
            p.setPM10(pm10);
            points.add(p);
        }
       return points;
    }
}
