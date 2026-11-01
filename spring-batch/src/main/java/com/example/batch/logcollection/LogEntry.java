package com.example.batch.logcollection;

import org.springframework.batch.infrastructure.item.ResourceAware;
import org.springframework.core.io.Resource;

public class LogEntry implements ResourceAware {

    private static final String LOG_SUFFIX = ".log";

    private String timestamp;
    private String level;
    private String message;
    private String server;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getServer() {
        return server;
    }

    @Override
    public void setResource(Resource resource) {
        String filename = resource.getFilename();
        this.server = filename != null && filename.endsWith(LOG_SUFFIX)
                ? filename.substring(0, filename.length() - LOG_SUFFIX.length())
                : filename;
    }
}