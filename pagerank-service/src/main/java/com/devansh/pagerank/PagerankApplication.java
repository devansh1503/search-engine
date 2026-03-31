package com.devansh.pagerank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PagerankApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagerankApplication.class, args);
	}

}
