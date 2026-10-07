package com.bcilab.attendance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Attendance attendance = new Attendance();
    private final Device device = new Device();
    private final Bootstrap bootstrap = new Bootstrap();

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMinutes;
    }

    @Getter
    @Setter
    public static class Attendance {
        private String timezone;
    }

    @Getter
    @Setter
    public static class Device {
        private String apiKeyHeader;
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String adminUsername;
        private String adminPassword;
        private String deviceCode;
        private String deviceLocation;
        private String deviceApiKey;
        private boolean seedDemoData;
    }
}
