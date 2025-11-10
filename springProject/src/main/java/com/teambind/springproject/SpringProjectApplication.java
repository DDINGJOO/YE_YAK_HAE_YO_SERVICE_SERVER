package com.teambind.springproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringProjectApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(SpringProjectApplication.class, args);
	}
	
}
