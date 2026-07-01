package com.eventhub.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    static {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            try {
                // Replace postgres:// or postgresql:// with http:// to easily parse user info, host, port using java.net.URI
                String cleanUrl = databaseUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI dbUri = new URI(cleanUrl);

                String username = properties.getUsername();
                String password = properties.getPassword();
                String userInfo = dbUri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] userInfoParts = userInfo.split(":", 2);
                    username = userInfoParts[0];
                    password = userInfoParts[1];
                }

                String host = dbUri.getHost();
                int port = dbUri.getPort();
                String path = dbUri.getPath();
                String query = dbUri.getQuery();

                StringBuilder jdbcUrlBuilder = new StringBuilder("jdbc:postgresql://")
                        .append(host);
                if (port != -1) {
                    jdbcUrlBuilder.append(":").append(port);
                }
                jdbcUrlBuilder.append(path);
                if (query != null) {
                    jdbcUrlBuilder.append("?").append(query);
                }

                return DataSourceBuilder.create()
                        .type(HikariDataSource.class)
                        .url(jdbcUrlBuilder.toString())
                        .username(username)
                        .password(password)
                        .build();
            } catch (URISyntaxException | NullPointerException e) {
                // Fallback to auto-configured datasource on parsing error
            }
        }

        // Default auto-configured DataSource using properties from application.yml / .env
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
