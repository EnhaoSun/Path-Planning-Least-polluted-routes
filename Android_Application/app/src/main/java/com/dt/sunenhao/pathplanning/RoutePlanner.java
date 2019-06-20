package com.dt.sunenhao.pathplanning;

import android.content.res.AssetManager;

import com.dt.sunenhao.pathplanning.Object.DataIO;
import com.dt.sunenhao.pathplanning.Object.MyPoint;
import com.dt.sunenhao.pathplanning.Object.PointMetric;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import smile.neighbor.CoverTree;

public class RoutePlanner {

    private static List<MyPoint> points = new ArrayList<>();
    private static Map<Integer, List<Integer>> neighbors = new HashMap<>();
    private static CoverTree coverTree;
    private static List<MyPoint> connectedPoints;
    private static ArrayList<Point> routePoints;
    private static List<MyPoint> airpollution = new ArrayList<>();
    private static double MAX_POLLUTION = 0;

    private static boolean plannerReady = false;
    private static boolean airPollutionReady = false;

    private static double totalPollution = 0;


    public static void initialiseGrouph(AssetManager assetManager, boolean pollutionGrid){
        points = DataIO.readPointsWithID(assetManager);
        neighbors = DataIO.readAdjacencyList(assetManager);

        if(pollutionGrid){
            List<MyPoint> gridPoints = DataIO.readGridWithPollution(assetManager);
            CoverTree gridCoverTree = new CoverTree(gridPoints.toArray(new MyPoint[gridPoints.size()]), new PointMetric());
            points = DataIO.getPollutionFromNeighbor(points, gridCoverTree, gridPoints);
        }

        connectedPoints = new ArrayList<>();
        for(MyPoint p : points){
            int idx = p.getIndex();
            List<Integer> curNeighbors = neighbors.get(idx);
            if(curNeighbors != null && !curNeighbors.isEmpty()) {
                connectedPoints.add(p);
            }
        }
        MyPoint[] connectedPointArray = connectedPoints.toArray(new MyPoint[connectedPoints.size()]);
        coverTree = new CoverTree(connectedPointArray, new PointMetric());
    }

    public static void findRoute(double lat1, double lng1, double lat2, double lng2){
        int s = connectedPoints.get(coverTree.nearest(new MyPoint(lat1, lng1)).index).getIndex();
        int t = connectedPoints.get(coverTree.nearest(new MyPoint(lat2, lng2)).index).getIndex();
        //dijkstra(s, t);
        dijkstraByPollution(s, t);
    }

    public static void dijkstraByPollution(int source, int target){
        PriorityQueue<MyPoint> queue = new PriorityQueue<>(11, Comparator.comparingDouble(MyPoint::getWeight));
        MyPoint s = points.get(source);
        queue.add(s);

        System.out.println("Source: " + source);
        System.out.println("Target: " + target);

        double[] dist = new double[100001];
        double[] pol = new double[100001];
        int[] parent = new int[100001];
        boolean[] settled = new boolean[100001];

        Arrays.fill(pol, 3000000000.0);

        dist[s.getIndex()] = 0;
        pol[s.getIndex()] = s.getPM2();

        while(!queue.isEmpty()){
            MyPoint p = queue.poll();
            Integer curNode = p.getIndex();
            settled[curNode] = true;

            if(curNode == target)
                break;

            for(Integer i : neighbors.get(curNode)){
                if(!settled[i]){
                    MyPoint neighbor = points.get(i);
                    if(pol[i] > pol[curNode] + neighbor.getPM2()){
                        pol[i] = pol[curNode] + neighbor.getPM2();
                        dist[i] = dist[curNode] + neighbor.getHarvesineDistance(p);
                        neighbor.setWeight(pol[i]);
                        parent[i] = curNode;
                        queue.add(neighbor);
                    }
                }
            }
        }

        routePoints = new ArrayList<>();
        String routeString = RoutePlanner.saveRouteNodes(points, target, parent);
        System.out.println("Total points: " + routePoints.size());
        System.out.println("Total pollution: " + totalPollution);
    }

    public static void dijkstra(int source, int target){
        PriorityQueue<MyPoint> queue = new PriorityQueue<>(11, Comparator.comparingDouble(MyPoint::getWeight));
        MyPoint s = points.get(source);
        queue.add(s);

        System.out.println("Source: " + source);
        System.out.println("Target: " + target);

        double[] dist = new double[100001];
        //double[] pol = new double[100001];
        int[] parent = new int[100001];
        boolean[] settled = new boolean[100001];

        Arrays.fill(dist, 3000000000.0);

        dist[s.getIndex()] = 0;

        while(!queue.isEmpty()){
            MyPoint p = queue.poll();
            Integer curNode = p.getIndex();
            settled[curNode] = true;

            if(curNode == target)
                break;

            for(Integer i : neighbors.get(curNode)){
                if(!settled[i]){
                    MyPoint neighbor = points.get(i);
                    double d = p.getHarvesineDistance(neighbor);
                    if(dist[i] > dist[curNode] + d){
                        dist[i] = dist[curNode] + d;
                        neighbor.setWeight(dist[i]);
                        parent[i] = curNode;
                        queue.add(neighbor);
                    }
                }
            }
        }

        routePoints = new ArrayList<>();
        String routeString = RoutePlanner.saveRouteNodes(points, target, parent);
        System.out.println("Total pollution: " + totalPollution);
    }


    public static String saveRouteNodes (List<MyPoint> points, int target, int[] parent) {

        String str = "";
        int curNode = target;
        totalPollution = 0;
        while (curNode != 0) {
            str += points.get(curNode).getLatitude() + " " + points.get(curNode-1).getLongitude() + "\n";
            routePoints.add(Point.fromLngLat(points.get(curNode).getLongitude(), points.get(curNode).getLatitude()));
            totalPollution += points.get(curNode).getPM2();
            curNode = parent[curNode];
        }
        return str;

    }

    public static List<MyPoint> getPoints() {
        return points;
    }

    public static boolean isPlannerReady() {
        return plannerReady;
    }

    public static void setPlannerReady(boolean plannerReady) {
        RoutePlanner.plannerReady = plannerReady;
    }

    public static ArrayList<Point> getRoutePoints() {
        return routePoints;
    }

    public static List<MyPoint> getAirpollution() {
        return airpollution;
    }

    public static void setAirpollution(List<MyPoint> airpollution) {
        RoutePlanner.airpollution = airpollution;
    }

    public static double getMaxPollution() {
        return MAX_POLLUTION;
    }

    public static void setMaxPollution(double maxPollution) {
        MAX_POLLUTION = maxPollution;
    }

    public static boolean isAirPollutionReady() {
        return airPollutionReady;
    }

    public static void setAirPollutionReady(boolean airPollutionReady) {
        RoutePlanner.airPollutionReady = airPollutionReady;
    }
}
