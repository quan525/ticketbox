package com.example.ticketbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class TicketboxApplication {

    public static void main(String[] args) {
        applyDatabaseUrl();
        SpringApplication.run(TicketboxApplication.class, args);
    }

    static void applyDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || System.getenv("SPRING_DATASOURCE_URL") != null) {
            return;
        }
        String normalized = databaseUrl.replaceFirst("^postgres(ql)?:", "http:");
        URI uri = URI.create(normalized);
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                System.setProperty("spring.datasource.username", decode(userInfo.substring(0, colon)));
                System.setProperty("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            } else {
                System.setProperty("spring.datasource.username", decode(userInfo));
            }
        }
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath();
        jdbc += (uri.getRawQuery() == null || uri.getRawQuery().isBlank()) ? "?sslmode=require" : "?" + uri.getRawQuery();
        System.setProperty("spring.datasource.url", jdbc);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
