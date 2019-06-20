package com.dt.sunenhao.pathplanning.Object;

public class MyPoint {

    private int index;
    private double latitude, longitude, PM1, PM2, PM10, weight;

    public MyPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double getHarvesineDistance(MyPoint p) {
        final int R = 6371; // Radius of the earth
        Double lat1 = this.latitude;
        Double lon1 = longitude;
        Double lat2 = p.getLatitude();
        Double lon2 = p.getLongitude();
        Double latDistance = (lat2-lat1) * Math.PI / 100;
        Double lonDistance = (lon2-lon1) * Math.PI / 100;
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(lat1 * Math.PI / 100) * Math.cos(lat2 * Math.PI / 100) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        Double distance = R * c;
        return distance;
    }

    public double getPM1() {
        return PM1;
    }

    public void setPM1(double PM1) {
        this.PM1 = PM1;
    }

    public double getPM2() {
        return PM2;
    }

    public void setPM2(double PM2) {
        this.PM2 = PM2;
    }

    public double getPM10() {
        return PM10;
    }

    public void setPM10(double PM10) {
        this.PM10 = PM10;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "MyPoint{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", PM2=" + PM2 +
                '}';
    }
}
