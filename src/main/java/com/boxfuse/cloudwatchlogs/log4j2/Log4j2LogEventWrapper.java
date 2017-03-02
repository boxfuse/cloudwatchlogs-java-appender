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
}
