# Path-Planning-Least-polluted-routes

## Android application

* In folder "Android_Application"

* Use Android studio to run and install the application in mobile phones

## Least polluted route server 

The server file are in "lpr-server" folder

In terminal, cd into the folder "lpr-server", then run following code to deploy the server in google cloud: mvn clean package appengine:deploy. Make sure that you have configured your own google cloud app engine envionment. See tutorials in here: https://cloud.google.com/appengine/docs/java/

## DATA

* Map data: in folder "Data/MAPS", "ExtractNodes.ipynb" can extract nodes and adjacency lists from .osm file.
* GAN: GANs.ipynb can generate 160 x 160 grid of pollution data from 20 x 20 grid of pollution data. 20 x 20 grid of pollution data were in folder "kernel_size_(1,2,3)/valid date/PM2.5-prediction.csv"

## Experiment result

The experiment results are in folder "ExperimentsResult"