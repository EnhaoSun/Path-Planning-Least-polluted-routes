# Path-Planning-Least-polluted-routes
This is my MSc Dissertation: Least polluted routes

## Android application
* In folder "Android_Application"
* Use Android studio to run and install the application in mobile phones

## Google Map service and Mapbox access code
The map service of this Android application require Mapbox access tokens (displaying the map) as well as Google Maps services (searching locations). 

To get your Mapbox access tokens, follow the tutorial as below:

https://docs.mapbox.com/help/how-mapbox-works/access-tokens/#how-access-tokens-work

The mapbox access token key should be placed at "**res/values/string.xml**" file for string name "**access_token**".

To optain an API Key for your Android application, please follow the tutorial as below:

https://developers.google.com/maps/documentation/android-sdk/get-api-key

The google API Key is assigned for a String variable called "**apiKey**" in function "**private void setupAutoCompleteFragment**" in "**MainActivity**"

## Least polluted route server 
The least polluted route server (lpr-server) is responsible for running path planning algorithms as well as sending the path in JSON to android application.

The server file are in "lpr-server" folder

In terminal, cd into the folder "lpr-server", then run following code to deploy the server in google cloud:
```linux
cd lpr-server
mvn clean package appengine:deploy
```
Make sure that you have configured your own google cloud app engine envionment. See tutorials in here: https://cloud.google.com/appengine/docs/java/

To test the lpr-server locally, run it with
```linux
mvn spring-boot:run
```

## DATA

* Map data: in folder "Data/MAPS", "ExtractNodes.ipynb" can extract nodes and adjacency lists from .osm file.
* GAN: GANs.ipynb can generate 160 x 160 grid of pollution data from 20 x 20 grid of pollution data. 20 x 20 grid of pollution data were in folder "kernel_size_(1,2,3)/valid date/PM2.5-prediction.csv"

## Experiment result
The experiment results are in folder "ExperimentsResult"
