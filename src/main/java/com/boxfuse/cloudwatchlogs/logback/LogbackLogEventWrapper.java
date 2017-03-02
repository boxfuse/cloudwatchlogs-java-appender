package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsMDCPropertyNames;
import com.boxfuse.cloudwatchlogs.internal.LogEventWrapper;
import org.slf4j.Marker;

/**
 * Created by kawnayeen on 3/2/17.
 */
public class LogbackLogEventWrapper implements LogEventWrapper {
    private ILoggingEvent event;

    public LogbackLogEventWrapper(ILoggingEvent loggingEvent) {
        this.event = loggingEvent;
    }

    @Override
    public String getAccount() {
        return event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.ACCOUNT);
    }

    @Override
    public String getAction() {
        return event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.ACTION);
    }

    @Override
    public String getUser() {
        return event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.USER);
    }

    @Override
    public String getSession() {
        return event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.SESSION);
    }

    @Override
    public String getRequest() {
        return event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.REQUEST);
    }

    @Override
    public String getEventId() {
        Marker marker = event.getMarker();
        return marker == null ? null : marker.getName();
    }
}