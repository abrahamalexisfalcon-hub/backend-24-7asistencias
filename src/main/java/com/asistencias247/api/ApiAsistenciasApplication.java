package com.asistencias247.api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;
import java.util.TimeZone;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApiAsistenciasApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        SpringApplication app = new SpringApplication(ApiAsistenciasApplication.class);
        app.setDefaultProperties(Map.of(
                "spring.servlet.multipart.max-file-size", "20MB",
                "spring.servlet.multipart.max-request-size", "25MB",
                "server.tomcat.max-swallow-size", "25MB",
                "server.tomcat.max-http-form-post-size", "25MB",
                "spring.jackson.time-zone", "America/Lima"
        ));
        app.run(args);
    }
}
