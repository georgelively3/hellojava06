package com.lithespeed.hellojava06.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile({ "fake-s3", "default", "test" })
public class FakeS3Service implements S3Service {

    private final List<String> files = new ArrayList<>();

    @Override
    public void uploadFile(String fileName) {
        files.add(fileName);
    }

    @Override
    public List<String> listFiles() {
        return new ArrayList<>(files);
    }
}
