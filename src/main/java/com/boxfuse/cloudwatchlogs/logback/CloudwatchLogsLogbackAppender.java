package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.CoreConstants;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEvent;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsMDCPropertyNames;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEventPutter;
import org.slf4j.Marker;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * LogBack appender for Boxfuse's AWS CloudWatch Logs integration.
 */
public class CloudwatchLogsLogbackAppender extends AppenderBase<ILoggingEvent> {
    private final CloudwatchLogsConfig config = new CloudwatchLogsConfig();
    private final ConcurrentLinkedQueue<CloudwatchLogsLogEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private CloudwatchLogsLogEventPutter putter;

    /**
     * @return The config of the appender. This instance can be modified to override defaults.
     */
    public CloudwatchLogsConfig getConfig() {
        return config;
    }

    @Override
    public void start() {
        super.start();
        putter = new CloudwatchLogsLogEventPutter(config, eventQueue);
        new Thread(putter).start();
    }

    @Override
    public void stop() {
        putter.terminate();
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        while (throwableProxy != null) {
            message += "\n" + dump(throwableProxy);
            throwableProxy = throwableProxy.getCause();
            if (throwableProxy != null) {
                message += "\nCaused by:";
            }
        }

        String account = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.ACCOUNT);
        String action = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.ACTION);
        String user = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.USER);
        String session = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.SESSION);
        String request = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.REQUEST);

        Marker marker = event.getMarker();
        String eventId = marker == null ? null : marker.getName();

        eventQueue.add(new CloudwatchLogsLogEvent(event.getLevel().levelStr, event.getLoggerName(), eventId, message, event.getTimeStamp(), event.getThreadName(), account, action, user, session, request));
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
