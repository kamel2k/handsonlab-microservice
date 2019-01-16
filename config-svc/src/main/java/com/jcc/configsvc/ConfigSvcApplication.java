package com.jcc.configsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigSvcApplication.class, args);
	}

}

