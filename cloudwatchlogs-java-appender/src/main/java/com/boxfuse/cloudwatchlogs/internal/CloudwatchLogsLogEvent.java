package com.boxfuse.cloudwatchlogs.internal;

import java.util.Map;

public class CloudwatchLogsLogEvent {
    private final String level;
    private final String logger;
    private final String event;
    private final String message;
    private final long timestamp;
    private final String thread;
    private final String account;
    private final String action;
    private final String user;
    private final String session;
    private final String request;
    private final Map<String, String> customMdcAttributes;

    public CloudwatchLogsLogEvent(String level, String logger, String event, String message, long timestamp,
                                  String thread, String account, String action, String user, String session,
                                  String request, Map<String, String> customMdcAttributes) {
        this.level = level;
        this.logger = logger;
        this.event = event;
        this.message = message;
        this.timestamp = timestamp;
        this.thread = thread;
        this.account = account;
        this.action = action;
        this.user = user;
        this.session = session;
        this.request = request;
        this.customMdcAttributes = customMdcAttributes;
    }

    public String getLevel() {
        return level;
    }

    public String getLogger() {
        return logger;
    }

    public String getEvent() {
        return event;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getThread() {
        return thread;
    }

    public String getAccount() {
        return account;
    }

    public String getAction() {
        return action;
    }

    public String getUser() {
        return user;
    }

    public String getSession() {
        return session;
    }

    public String getRequest() {
        return request;
    }

    public Map<String, String> getCustomMdcAttributes() {
        return customMdcAttributes;
    }
}
