package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.CoreConstants;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsMDCPropertyNames;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEvent;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEventPutter;
import org.slf4j.Marker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * LogBack appender for Boxfuse's AWS CloudWatch Logs integration.
 */
public class CloudwatchLogsLogbackAppender extends AppenderBase<ILoggingEvent> {
    private final CloudwatchLogsConfig config = new CloudwatchLogsConfig();
    private BlockingQueue<CloudwatchLogsLogEvent> eventQueue;
    private CloudwatchLogsLogEventPutter putter;
    private long discardedCount;

    /**
     * @return The config of the appender. This instance can be modified to override defaults.
     */
    public CloudwatchLogsConfig getConfig() {
        return config;
    }

    @Override
    public void start() {
        super.start();
        eventQueue = new LinkedBlockingQueue<>(config.getMaxEventQueueSize());
        putter = new CloudwatchLogsLogEventPutter(config, eventQueue);
        new Thread(putter).start();
    }

    @Override
    public void stop() {
        putter.terminate();
        super.stop();
    }

    /**
     * @return The number of log events that had to be discarded because the event queue was full.
     * If this number is non zero without having been affected by AWS CloudWatch Logs availability issues,
     * you should consider increasing maxEventQueueSize in the config to allow more log events to be buffer before having to drop them.
     */
    public long getDiscardedCount() {
        return discardedCount;
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

        CloudwatchLogsLogEvent logEvent = new CloudwatchLogsLogEvent(event.getLevel().toString(), event.getLoggerName(), eventId, message, event.getTimeStamp(), event.getThreadName(), account, action, user, session, request);
        while (!eventQueue.offer(logEvent)) {
            // Discard old logging messages while queue is full.
            eventQueue.poll();
            discardedCount++;
        }
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
