package com.jcc.discoverysvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class DiscoverySvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoverySvcApplication.class, args);
	}

}

