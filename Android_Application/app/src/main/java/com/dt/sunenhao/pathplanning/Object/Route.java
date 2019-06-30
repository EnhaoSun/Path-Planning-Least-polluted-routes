package com.dt.sunenhao.pathplanning.Object;

import com.mapbox.geojson.Point;

import java.util.ArrayList;

public class Route {
    private double totalPollution;
    private ArrayList<Point> route;

    public Route(double totalPollution, ArrayList<Point> route) {
        this.totalPollution = totalPollution;
        this.route = route;
    }

    public double getTotalPollution() {
        return totalPollution;
    }

    public void setTotalPollution(double totalPollution) {
        this.totalPollution = totalPollution;
    }

    public ArrayList<Point> getRoute() {
        return route;
    }

    public void setRoute(ArrayList<Point> route) {
        this.route = route;
    }
}
