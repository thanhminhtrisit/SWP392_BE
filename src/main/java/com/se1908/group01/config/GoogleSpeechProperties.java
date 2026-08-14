package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.speech")
public class GoogleSpeechProperties {

    private String projectId;

    private String gcsBucketName;

    private String gcsKeyPrefix = "transcribe-temp/";

    private int timeoutSeconds = 600;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getGcsBucketName() {
        return gcsBucketName;
    }

    public void setGcsBucketName(String gcsBucketName) {
        this.gcsBucketName = gcsBucketName;
    }

    public String getGcsKeyPrefix() {
        return gcsKeyPrefix;
    }

    public void setGcsKeyPrefix(String gcsKeyPrefix) {
        this.gcsKeyPrefix = gcsKeyPrefix;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

}
