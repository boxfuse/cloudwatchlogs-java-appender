package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
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

    @Override
    public String getMessage() {
        String message = event.getFormattedMessage();
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        while (throwableProxy != null) {
            message += "\n" + dump(throwableProxy);
            throwableProxy = throwableProxy.getCause();
            if (throwableProxy != null) {
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
        return event.getTimeStamp();
    }

    @Override
    public String getThreadName() {
        return event.getThreadName();
    }

    private String dump(IThrowableProxy throwableProxy) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwableProxy.getMessage()).append(CoreConstants.LINE_SEPARATOR);
        for (StackTraceElementProxy step : throwableProxy.getStackTraceElementProxyArray()) {
            String string = step.toString();
            builder.append(CoreConstants.TAB).append(string);
            ThrowableProxyUtil.subjoinPackagingData(builder, step);
            builder.append(CoreConstants.LINE_SEPARATOR);
        }
        return builder.toString();

    }
}