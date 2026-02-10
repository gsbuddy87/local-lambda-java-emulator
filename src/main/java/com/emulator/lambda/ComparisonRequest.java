package com.emulator.lambda;

public class ComparisonRequest {
    private String bucketName;
    private String key1;
    private String key2;

    public ComparisonRequest() {
    }

    public ComparisonRequest(String bucketName, String key1, String key2) {
        this.bucketName = bucketName;
        this.key1 = key1;
        this.key2 = key2;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKey1() {
        return key1;
    }

    public void setKey1(String key1) {
        this.key1 = key1;
    }

    public String getKey2() {
        return key2;
    }

    public void setKey2(String key2) {
        this.key2 = key2;
    }
}
