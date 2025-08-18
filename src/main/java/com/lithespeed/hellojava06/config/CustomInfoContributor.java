package com.lithespeed.hellojava06.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomInfoContributor implements InfoContributor {

    @Value("${app.environment:unknown}")
    private String environment;

    @Value("${app.version:unknown}")
    private String version;

    @Value("${aws.s3.bucket-name:unknown}")
    private String s3Bucket;

    @Value("${aws.s3.region:unknown}")
    private String s3Region;

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("environment", environment);
        appInfo.put("version", version);

        Map<String, Object> s3Info = new HashMap<>();
        s3Info.put("bucket", s3Bucket);
        s3Info.put("region", s3Region);

        builder.withDetail("application", appInfo);
        builder.withDetail("s3", s3Info);
        builder.withDetail("features", new String[] { "user-management", "s3-integration" });
    }
}
