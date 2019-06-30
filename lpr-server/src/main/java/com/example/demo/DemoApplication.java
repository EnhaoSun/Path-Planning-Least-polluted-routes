package com.example.demo;

import com.example.demo.MapService.RoutePlanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

    @EventListener(ApplicationReadyEvent.class)
    public void InitializeMap(){
        RoutePlanner.initialiseGrouph(true);
    }


}
