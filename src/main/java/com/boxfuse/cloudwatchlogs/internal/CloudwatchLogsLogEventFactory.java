package com.boxfuse.cloudwatchlogs.internal;

/**
 * Created by kawnayeen on 3/2/17.
 */
public class CloudwatchLogsLogEventFactory {
    public static CloudwatchLogsLogEvent getLogEvent(LogEventWrapper logEventWrapper) {
        return new CloudwatchLogsLogEvent(
                logEventWrapper.getLevel(),
                logEventWrapper.getLoggerName(),
                logEventWrapper.getEventId(),
                logEventWrapper.getMessage(),
                logEventWrapper.getTimeInMillis(),
                logEventWrapper.getThreadName(),
                logEventWrapper.getAccount(),
                logEventWrapper.getAction(),
                logEventWrapper.getUser(),
                logEventWrapper.getSession(),
                logEventWrapper.getRequest()
        );
    }
}
