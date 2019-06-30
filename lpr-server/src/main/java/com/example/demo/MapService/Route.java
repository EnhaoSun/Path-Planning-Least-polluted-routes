package com.example.demo.MapService;

import java.util.ArrayList;

/**
 * @author Enhao Sun
 * @version 2019-06-12.
 */
public class Route {
    private ArrayList<Point> routePoints;
    private String routeString;
    private double dist;
    private double pollution;

    public Route(ArrayList<Point> routePoints, String routeString, double dist, double pollution) {
        this.routePoints = routePoints;
        this.routeString = routeString;
        this.dist = dist;
        this.pollution = pollution;
    }

    public ArrayList<Point> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(ArrayList<Point> routePoints) {
        this.routePoints = routePoints;
    }

    public String getRouteString() {
        return routeString;
    }

    public void setRouteString(String routeString) {
        this.routeString = routeString;
    }

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public double getPollution() {
        return pollution;
    }

    public void setPollution(double pollution) {
        this.pollution = pollution;
    }
}
