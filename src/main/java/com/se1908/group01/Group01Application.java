package com.se1908.group01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class Group01Application {

	public static void main(String[] args) {
		SpringApplication.run(Group01Application.class, args);
	}

}
