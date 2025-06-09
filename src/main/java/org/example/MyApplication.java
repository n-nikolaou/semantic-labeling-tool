package org.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class MyApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Code to run after application starts
		System.out.println("Application started successfully!");
	}
}
