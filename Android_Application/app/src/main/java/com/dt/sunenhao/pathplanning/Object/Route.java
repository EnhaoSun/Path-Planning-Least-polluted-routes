package com.dt.sunenhao.pathplanning.Object;

import com.mapbox.geojson.Point;

import java.util.ArrayList;

public class Route {
    private ArrayList<Point> route;
    private double aver_PM1;
    private double aver_PM2;
    private double aver_PM10;
    private String routeJson;


    public Route(ArrayList<Point> route, String routeJson, double aver_PM1, double aver_PM2, double aver_PM10) {
        this.route = route;
        this.aver_PM1 = aver_PM1;
        this.aver_PM2 = aver_PM2;
        this.aver_PM10 = aver_PM10;
        this.routeJson = routeJson;
    }

    public ArrayList<Point> getRoute() {
        return route;
    }
    public void setRoute(ArrayList<Point> route) {
        this.route = route;
    }

    public double getAver_PM1() {
        return aver_PM1;
    }

    public void setAver_PM1(double aver_PM1) {
        this.aver_PM1 = aver_PM1;
    }

    public double getAver_PM2() {
        return aver_PM2;
    }

    public void setAver_PM2(double aver_PM2) {
        this.aver_PM2 = aver_PM2;
    }

    public double getAver_PM10() {
        return aver_PM10;
    }

    public void setAver_PM10(double aver_PM10) {
        this.aver_PM10 = aver_PM10;
    }

    public String getRouteJson() {
        return routeJson;
    }

    public void setRouteJson(String routeJson) {
        this.routeJson = routeJson;
    }
}
