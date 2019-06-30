package com.example.demo.MapService;

import smile.neighbor.CoverTree;

import java.util.*;

/**
 * @author Enhao Sun
 * @version 2019-06-12.
 */
public final class RoutePlanner {
    private static List<Point> points = new ArrayList<>();
    private static Map<Integer, List<Integer>> neighbors = new HashMap<>();
    private static CoverTree coverTree;
    private static List<Point> connectedPoints;
    private static ArrayList<Point> routePoints;
    private static List<Point> gridPoints;

    public static void initialiseGrouph(boolean pollutionGrid){
        System.out.println("Initializing Grouph");
        points = DataIO.readPointsWithID();
        neighbors = DataIO.readAdjacencyList();

        if(pollutionGrid){
            gridPoints = DataIO.readGridWithPollution();
            CoverTree gridCoverTree = new CoverTree(gridPoints.toArray(new Point[gridPoints.size()]), new PointMetric());
            points = DataIO.getPollutioFromNeighbor(points, gridCoverTree, gridPoints);
        }

        //for(int i = 0; i < neighbors.size(); i++)
        //    System.out.println(neighbors.get(i));

        connectedPoints = new ArrayList<>();
        for(Point p : points){
            int idx = p.getIndex();
            List<Integer> curNeighbors = neighbors.get(idx);
            if(curNeighbors != null && !curNeighbors.isEmpty()) {
                connectedPoints.add(p);
            }
        }
        Point[] connectedPointArray = connectedPoints.toArray(new Point[connectedPoints.size()]);
        coverTree = new CoverTree(connectedPointArray, new PointMetric());
    }

    public static String findRoute(boolean usePollution, double lat1, double lng1, double lat2, double lng2){
        int s = connectedPoints.get(coverTree.nearest(new Point(lat1, lng1)).index).getIndex();
        int t = connectedPoints.get(coverTree.nearest(new Point(lat2, lng2)).index).getIndex();

        //return dijkstraByPollution(usePollution, s, t);
        return aStar(usePollution, s,t);
        //return bidirectionalAStar(s, t);
    }

    public static String bidirectionalAStar(int source, int target){
        //System.out.println("Birectional A Star");
        PriorityQueue<Point> queue_forward = new PriorityQueue<>(11, Comparator.comparingDouble(Point::getWeight));
        PriorityQueue<Point> queue_backward = new PriorityQueue<>(11, Comparator.comparingDouble(Point::getWeight));
        Point s = points.get(source);
        Point t = points.get(target);
        queue_forward.add(s);
        queue_backward.add(t);

        double[] pol_forward = new double[100001];
        double[] pol_backward = new double[100001];
        double[] dist_forward = new double[100001];
        double[] dist_backward = new double[100001];
        int[] parent_forward = new int[100001];
        int[] parent_backward = new int[100001];
        boolean[] settled_forward = new boolean[100001];
        boolean[] settled_backward = new boolean[100001];

        Arrays.fill(pol_forward, 3000000000.0);
        Arrays.fill(pol_backward, 3000000000.0);

        pol_forward[s.getIndex()] = s.getPM2();
        pol_backward[t.getIndex()] = t.getPM2();

        dist_forward[s.getIndex()] = 0;
        dist_backward[t.getIndex()] = 0;

        boolean turnFlag = true;
        double leastPollution = 0;
        double distance = 0;
        int meetingPoint = 0;

        while(true){
            if (turnFlag && queue_forward.isEmpty()) continue;
            if (!turnFlag && queue_backward.isEmpty()) continue;
            if (queue_forward.isEmpty() && queue_backward.isEmpty()) break;

            if(turnFlag){
                Point p = queue_forward.poll();
                Integer curNode = p.getIndex();
                while (settled_forward[curNode]) {
                    p = queue_forward.poll();
                    curNode = p.getIndex();
                }
                settled_forward[curNode] = true;
                if (settled_backward[curNode] == true) {
                    leastPollution = pol_forward[curNode] + pol_backward[curNode] - p.getPM2();
                    distance = dist_forward[curNode] + dist_backward[curNode];
                    meetingPoint = curNode;
                    break;
                }
                for(Integer i : neighbors.get(curNode)) {
                    if (!settled_forward[i]) {
                        Point neighbor = points.get(i);
                        Double d = neighbor.getPM2();
                        if (pol_forward[i] > pol_forward[curNode] + d) {
                            pol_forward[i] = pol_forward[curNode] + d;
                            dist_forward[i] = dist_forward[curNode] + neighbor.getHarvesineDistance(p);
                            neighbor.setWeight(pol_forward[i] + neighbor.getHarvesineDistance(t));
                            parent_forward[i] = curNode;
                            queue_forward.add(neighbor);
                        }
                    }
                }
            }else{
                Point p = queue_backward.poll();
                Integer curNode = p.getIndex();
                while (settled_backward[curNode]) {
                    p = queue_backward.poll();
                    curNode = p.getIndex();
                }
                settled_backward[curNode] = true;

                if (settled_forward[curNode] == true) {
                    leastPollution = pol_forward[curNode] + pol_backward[curNode] - p.getPM2();
                    distance = dist_forward[curNode] + dist_backward[curNode];
                    meetingPoint = curNode;
                    break;
                }
                for(Integer i : neighbors.get(curNode)) {
                    if (!settled_backward[i]) {
                        Point neighbor = points.get(i);
                        Double d = neighbor.getPM2();
                        if (pol_backward[i] > pol_backward[curNode] + d) {
                            pol_backward[i] = pol_backward[curNode] + d;
                            dist_backward[i] = dist_backward[curNode] + neighbor.getHarvesineDistance(p);
                            neighbor.setWeight(pol_backward[i] + neighbor.getHarvesineDistance(s));
                            parent_backward[i] = curNode;
                            queue_backward.add(neighbor);
                        }
                    }
                }
            }
            turnFlag = !turnFlag;
        }
        if (leastPollution == 0) return null;
        while(!queue_forward.isEmpty()) {
            Point p = queue_forward.poll();
            Integer curNode = p.getIndex();
            for(Integer i : neighbors.get(curNode)) {
                if (leastPollution > pol_forward[curNode] + pol_backward[i]) {
                    leastPollution = pol_forward[curNode] + pol_backward[i];
                    distance = dist_forward[curNode] + dist_backward[curNode];
                    parent_forward[i] = curNode;
                    meetingPoint = i;
                }
            }
        }
        while(!queue_backward.isEmpty()) {
            Point p = queue_backward.poll();
            Integer curNode = p.getIndex();
            for(Integer i : neighbors.get(curNode)) {
                if (leastPollution > pol_backward[curNode] + pol_forward[i]) {
                    leastPollution = pol_backward[curNode] + pol_forward[i];
                    distance = dist_forward[curNode] + dist_backward[curNode];
                    parent_forward[i] = curNode;
                    meetingPoint = i;
                }
            }
        }

        routePoints = new ArrayList<Point>();
        double totalPM2 = 0;
        String routeString = RoutePlanner.saveRouteNodesReversed(points, meetingPoint, parent_backward) + RoutePlanner.saveRouteNodes(points, meetingPoint, parent_forward);
        for(Point p : routePoints){
            totalPM2 += p.getPM2();
        }
        //System.out.println("Execution time Bidirectional A*: " + (endTime - startTime));
        //System.out.println("Total points: " + routePoints.size());
        //System.out.println("Total PM2.5: " + totalPM2);
        return routeString;
    }

    public static String aStar(boolean usePollution, int source, int target){
        //System.out.println("A Star By pollution");
        PriorityQueue<Point> queue = new PriorityQueue<>(11, Comparator.comparingDouble(Point::getWeight));
        Point s = points.get(source);
        Point t = points.get(target);
        queue.add(s);

        //System.out.println("Source: " + source);
        //System.out.println("Target: " + target);

        double[] dist = new double[100001];
        double[] pol = new double[100001];
        int[] parent = new int[100001];
        boolean[] settled = new boolean[100001];

        Arrays.fill(pol, 3000000000.0);
        Arrays.fill(dist, 3000000000.0);

        dist[s.getIndex()] = 0;
        pol[s.getIndex()] = s.getPM2();

        while(!queue.isEmpty()){
            Point p = queue.poll();
            Integer curNode = p.getIndex();
            settled[curNode] = true;

            if(curNode == target)
                break;

            for(Integer i : neighbors.get(curNode)){
                if(!settled[i]){
                    Point neighbor = points.get(i);
                    if(usePollution){
                        if(pol[i] > pol[curNode] + neighbor.getPM2()){
                            pol[i] = pol[curNode] + neighbor.getPM2();
                            dist[i] = dist[curNode] + neighbor.getHarvesineDistance(p);
                            neighbor.setWeight(pol[i] + neighbor.getHarvesineDistance(t));
                            parent[i] = curNode;
                            queue.add(neighbor);
                        }
                    }else{
                        if(dist[i] > dist[curNode] + neighbor.getHarvesineDistance(p)){
                            dist[i] = dist[curNode] + neighbor.getHarvesineDistance(p);
                            pol[i] = pol[curNode] + neighbor.getPM2();
                            neighbor.setWeight(dist[i] + neighbor.getHarvesineDistance(t));
                            parent[i] = curNode;
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        routePoints = new ArrayList<>();
        double totalPM2 = 0;
        String routeString = RoutePlanner.saveRouteNodes(points, target, parent);
        for(Point p : routePoints){
            totalPM2 += p.getPM2();
        }
        //System.out.println("Execution time A*: " + (endTime - startTime));
        //System.out.println("Total points: " + routePoints.size());
        System.out.println("Use Pollution: " + usePollution + "Total PM2.5: " + totalPM2);
        return totalPM2 + "\n" + routeString;
    }


    public static String dijkstraByPollution(boolean usePollution, int source, int target){
        //System.out.println("Dijkstra By pollution");
        PriorityQueue<Point> queue = new PriorityQueue<>(11, Comparator.comparingDouble(Point::getWeight));
        Point s = points.get(source);
        queue.add(s);

        System.out.println("Source: " + source);
        System.out.println("Target: " + target);

        double[] dist = new double[100001];
        double[] pol = new double[100001];
        int[] parent = new int[100001];
        boolean[] settled = new boolean[100001];

        Arrays.fill(pol, 3000000000.0);
        Arrays.fill(dist, 3000000000.0);

        dist[s.getIndex()] = 0;
        pol[s.getIndex()] = s.getPM2();

        while(!queue.isEmpty()){
            Point p = queue.poll();
            Integer curNode = p.getIndex();
            settled[curNode] = true;

            if(curNode == target)
                break;

            for(Integer i : neighbors.get(curNode)){
                if(!settled[i]){
                    Point neighbor = points.get(i);
                    if(usePollution){
                        if(pol[i] > pol[curNode] + neighbor.getPM2()){
                            pol[i] = pol[curNode] + neighbor.getPM2();
                            dist[i] = dist[curNode] + neighbor.getHarvesineDistance(p);
                            neighbor.setWeight(pol[i]);
                            parent[i] = curNode;
                            queue.add(neighbor);
                        }
                    }else{
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
        }

        routePoints = new ArrayList<>();
        double totalPM2 = 0;
        String routeString = RoutePlanner.saveRouteNodes(points, target, parent);
        long endTime = System.currentTimeMillis();
        for(Point p : routePoints){
            totalPM2 += p.getPM2();
        }
        //System.out.println("Execution time Dijkstra: " + (endTime - startTime));
        //System.out.println("Total poitns: " + routePoints.size());
        //System.out.println("Total PM2.5: " + totalPM2);
        System.out.println("Use Pollution: " + usePollution + "Total PM2.5: " + totalPM2);
        return routeString;
    }


    public static String dijkstra(int source, int target){
        PriorityQueue<Point> queue = new PriorityQueue<>(11, Comparator.comparingDouble(Point::getWeight));
        Point s = points.get(source);
        queue.add(s);

        double[] dist = new double[100001];
        //double[] pol = new double[100001];
        int[] parent = new int[100001];
        boolean[] settled = new boolean[100001];

        Arrays.fill(dist, 3000000000.0);

        dist[s.getIndex()] = 0;

        while(!queue.isEmpty()){
            Point p = queue.poll();
            Integer curNode = p.getIndex();
            settled[curNode] = true;

            if(curNode == target)
                break;

            for(Integer i : neighbors.get(curNode)){
                if(!settled[i]){
                    Point neighbor = points.get(i);
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
        double totalPM2 = 0;
        String routeString = RoutePlanner.saveRouteNodes(points, target, parent);
        for(Point p : routePoints){
            totalPM2 += p.getPM2();
        }
        //System.out.println("Total poitns: " + routePoints.size());
        //System.out.println("Total PM2.5: " + totalPM2);
        return routeString;
    }


    public static String saveRouteNodes (List<Point> points, int target, int[] parent) {

        String str = "";
        int curNode = target;
        while (curNode != 0) {
            str += points.get(curNode).getLatitude() + " " + points.get(curNode).getLongitude() + "\n";
            routePoints.add(points.get(curNode));
            curNode = parent[curNode];
        }
        return str;

    }

    public static String saveRouteNodesReversed (List<Point> points, int target, int[] parent) {

        String str = "";
        int curNode = parent[target];
        while (curNode != 0) {
            str = points.get(curNode-1).getLatitude() + " " + points.get(curNode).getLongitude() + "\n" + str;
            //routePoints.add(0, points.get(curNode));
            curNode = parent[curNode];
        }
        return str;
    }




    public static List<Point> getConnectedPoints() {
        return connectedPoints;
    }

    public static List<Point> getPoints() {
        return points;
    }

    public static Map<Integer, List<Integer>> getNeighbors() {
        return neighbors;
    }

    public static CoverTree getCoverTree() {
        return coverTree;
    }
}
