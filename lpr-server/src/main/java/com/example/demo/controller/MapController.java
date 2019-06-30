package com.example.demo.controller;

import com.example.demo.MapService.RoutePlanner;
import com.example.demo.MapService.DataIO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Enhao Sun
 * @version 2019-06-18.
 */

@RestController
public class MapController {

    /*
    Example URL request:
    http://localhost:8080/routes/55.940639/-3.182709/55.944941/-3.194339
     */
    @RequestMapping("/routes/{usePollution}/{sLat}/{sLong}/{tLat}/{tLong}")
    String getRoutes(@PathVariable("usePollution") Boolean usePollution,
                     @PathVariable("sLat") Double sLat, @PathVariable("sLong") Double sLong,
                     @PathVariable("tLat") Double tLat, @PathVariable("tLong") Double tLong){
        /*
        double sLat = 55.940639;
        double sLong = -3.182709;
        double tLat = 55.944941;
        double tLong = -3.194339;
        */
        return RoutePlanner.findRoute(usePollution, sLat, sLong, tLat, tLong);
    }

    @RequestMapping("/heatMap")
    String getHeatMap(){
        return DataIO.toJson(RoutePlanner.getPoints());
    }
}
