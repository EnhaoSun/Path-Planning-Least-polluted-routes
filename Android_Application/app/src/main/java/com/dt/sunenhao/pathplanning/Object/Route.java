package com.dt.sunenhao.pathplanning.Object;

import com.mapbox.geojson.Point;

import java.util.ArrayList;

public class Route {
    private ArrayList<Point> route;
    private double totalPM1;
    private double totalPM2;
    private double totalPM10;
    private double totalDistance;
    private String routeJson;
    private String polTime;

    public Route(ArrayList<Point> route, double totalPM1, double totalPM2, double totalPM10, double totalDistance, String routeJson) {
        this.route = route;
        this.totalPM1 = totalPM1;
        this.totalPM2 = totalPM2;
        this.totalPM10 = totalPM10;
        this.totalDistance = totalDistance;
        this.routeJson = routeJson;
    }

    public ArrayList<Point> getRoute() {
        return route;
    }

    public void setRoute(ArrayList<Point> route) {
        this.route = route;
    }

    public double getTotalPM1() {
        return totalPM1;
    }

    public void setTotalPM1(double totalPM1) {
        this.totalPM1 = totalPM1;
    }

    public double getTotalPM2() {
        return totalPM2;
    }

    public void setTotalPM2(double totalPM2) {
        this.totalPM2 = totalPM2;
    }

    public double getTotalPM10() {
        return totalPM10;
    }

    public void setTotalPM10(double totalPM10) {
        this.totalPM10 = totalPM10;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public String getRouteJson() {
        return routeJson;
    }

    public void setRouteJson(String routeJson) {
        this.routeJson = routeJson;
    }

    public String getPolTime() {
        return polTime;
    }

    public void setPolTime(String polTime) {
        this.polTime = polTime;
    }
}
