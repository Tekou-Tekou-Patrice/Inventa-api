package com.onescompany.Inventa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InventaApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventaApplication.class, args);
	}

}
