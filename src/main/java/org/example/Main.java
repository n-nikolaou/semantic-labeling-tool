package org.example;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;


public class Main {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    public static Adapter initialize(String text) {
        return new Adapter(text);
    }

}