package com.lithespeed.hellojava06.service;

import java.util.List;

public interface S3Service {
    void uploadFile(String fileName);
    List<String> listFiles();
}

