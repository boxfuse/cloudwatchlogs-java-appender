package com.boxfuse.cloudwatchlogs.log4j2;

import com.boxfuse.cloudwatchlogs.CloudwatchLogsMDCPropertyNames;
import com.boxfuse.cloudwatchlogs.internal.LogEventWrapper;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Created by kawnayeen on 3/2/17.
 */
public class Log4j2LogEventWrapper implements LogEventWrapper {
    private LogEvent event;

    public Log4j2LogEventWrapper(LogEvent event) {
        this.event = event;
    }

    @Override
    public String getAccount() {
        return event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.ACCOUNT);
    }

    @Override
    public String getAction() {
        return event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.ACTION);
    }

    @Override
    public String getUser() {
        return event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.USER);
    }

    @Override
    public String getSession() {
        return event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.SESSION);
    }

    @Override
    public String getRequest() {
        return event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.REQUEST);
    }

    @Override
    public String getEventId() {
        Marker marker = event.getMarker();
        return marker == null ? null : marker.getName();
    }

    @Override
    public String getMessage() {
        String message = event.getMessage().getFormattedMessage();
        Throwable throwable = event.getThrown();
        while (throwable != null) {
            message += "\n" + dump(throwable);
            throwable = throwable.getCause();
            if (throwable != null) {
                message += "\nCaused by:";
            }
        }
        return message;
    }

    @Override
    public String getLevel() {
        return event.getLevel().toString();
    }

    @Override
    public String getLoggerName() {
        return event.getLoggerName();
    }

    @Override
    public long getTimeInMillis() {
        return event.getTimeMillis();
    }

    @Override
    public String getThreadName() {
        return event.getThreadName();
    }

    private String dump(Throwable throwableProxy) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwableProxy.getMessage()).append("\n");
        for (StackTraceElement step : throwableProxy.getStackTrace()) {
            String string = step.toString();
            builder.append("\t").append(string);
            builder.append(step);
            builder.append("\n");
        }
        return builder.toString();
    }
}
