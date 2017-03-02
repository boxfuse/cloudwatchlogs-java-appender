package com.boxfuse.cloudwatchlogs.internal;

/**
 * Created by kawnayeen on 3/2/17.
 */
public interface LogEventWrapper {
    String getAccount();
    String getAction();
    String getUser();
    String getSession();
    String getRequest();
    String getEventId();
}
