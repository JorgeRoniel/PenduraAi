package com.ufc.apiPenduraAi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiPenduraAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiPenduraAiApplication.class, args);
	}

}
