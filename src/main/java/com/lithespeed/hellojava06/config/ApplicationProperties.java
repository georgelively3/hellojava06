package com.lithespeed.hellojava06.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private String environment = "unknown";
    private String version = "unknown";
    private Cors cors = new Cors();
    private S3Settings s3 = new S3Settings();

    // Getters and setters
    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public S3Settings getS3() {
        return s3;
    }

    public void setS3(S3Settings s3) {
        this.s3 = s3;
    }

    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private String exposedHeaders;
        private boolean allowCredentials = false;
        private int maxAge = 3600;

        // Getters and setters
        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public String getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(String exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public int getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(int maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class S3Settings {
        private boolean healthCheckEnabled = true;
        private int healthCheckTimeout = 5000;
        private String defaultPrefix = "uploads/";

        public boolean isHealthCheckEnabled() {
            return healthCheckEnabled;
        }

        public void setHealthCheckEnabled(boolean healthCheckEnabled) {
            this.healthCheckEnabled = healthCheckEnabled;
        }

        public int getHealthCheckTimeout() {
            return healthCheckTimeout;
        }

        public void setHealthCheckTimeout(int healthCheckTimeout) {
            this.healthCheckTimeout = healthCheckTimeout;
        }

        public String getDefaultPrefix() {
            return defaultPrefix;
        }

        public void setDefaultPrefix(String defaultPrefix) {
            this.defaultPrefix = defaultPrefix;
        }
    }
}
