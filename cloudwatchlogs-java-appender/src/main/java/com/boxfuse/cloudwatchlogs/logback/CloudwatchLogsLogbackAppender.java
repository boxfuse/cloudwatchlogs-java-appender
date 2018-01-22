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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LogBack appender for Boxfuse's AWS CloudWatch Logs integration.
 */
public class CloudwatchLogsLogbackAppender extends AppenderBase<ILoggingEvent> {
    CloudwatchLogsConfig config = new CloudwatchLogsConfig();
    BlockingQueue<CloudwatchLogsLogEvent> eventQueue;
    CloudwatchLogsLogEventPutter putter;
    private AtomicLong processedCount;
    private AtomicLong discardedCount;

    /**
     * @return The config of the appender. This instance can be modified to override defaults.
     */
    public CloudwatchLogsConfig getConfig() {
        return config;
    }

    /**
     * @param config The config of the appender.
     */
    public void setConfig(CloudwatchLogsConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        super.start();
        eventQueue = new LinkedBlockingQueue<>(config.getMaxEventQueueSize());
        putter = createCloudwatchLogsLogEventPutter();
        processedCount = new AtomicLong(0);
        discardedCount = new AtomicLong(0);
        Thread t = new Thread(putter, CloudwatchLogsLogEventPutter.class.getSimpleName());
        t.setDaemon(true);
        t.start();
    }

    CloudwatchLogsLogEventPutter createCloudwatchLogsLogEventPutter() {
        return CloudwatchLogsLogEventPutter.create(config, eventQueue);
    }

    @Override
    public void stop() {
        putter.terminate();
        super.stop();
    }

    /**
     * @return The number of log events that have been processed by this appender since it started.
     */
    public long getProcessedCount() {
        return processedCount.get();
    }

    /**
     * @return The number of log events that had to be discarded because the event queue was full.
     * If this number is non zero without having been affected by AWS CloudWatch Logs availability issues,
     * you should consider increasing maxEventQueueSize in the config to allow more log events to be buffer before having to drop them.
     */
    public long getDiscardedCount() {
        return discardedCount.get();
    }

    /**
     * @return Whether the background thread responsible for sending events to AWS is still running.
     */
    public boolean isRunning() {
        return putter.isRunning();
    }

    @Override
    protected void append(ILoggingEvent event) {
        StringBuilder message = new StringBuilder(event.getFormattedMessage());
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        while (throwableProxy != null) {
            message.append("\n").append(dump(throwableProxy));
            throwableProxy = throwableProxy.getCause();
            if (throwableProxy != null) {
                message.append("\nCaused by:");
            }
        }

        String account = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.ACCOUNT);
        String action = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.ACTION);
        String user = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.USER);
        String session = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.SESSION);
        String request = event.getMDCPropertyMap().get(CloudwatchLogsMDCPropertyNames.REQUEST);

        Marker marker = event.getMarker();
        String eventId = marker == null ? null : marker.getName();

        Map<String, String> customMdcAttributes = new HashMap<>();
        for (String key : config.getCustomMdcKeys()) {
            String value = event.getMDCPropertyMap().get(key);
            if (value != null) {
                customMdcAttributes.put(key, value);
            }
        }

        CloudwatchLogsLogEvent logEvent = new CloudwatchLogsLogEvent(event.getLevel().toString(), event.getLoggerName(),
                eventId, message.toString(), event.getTimeStamp(), event.getThreadName(), account, action, user,
                session, request, customMdcAttributes);
        while (!eventQueue.offer(logEvent)) {
            // Discard old logging messages while queue is full.
            eventQueue.poll();
            discardedCount.incrementAndGet();
        }
        processedCount.incrementAndGet();
    }

    private String dump(IThrowableProxy throwableProxy) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwableProxy.getClassName()).append(": ").append(throwableProxy.getMessage()).append(CoreConstants.LINE_SEPARATOR);
        for (StackTraceElementProxy step : throwableProxy.getStackTraceElementProxyArray()) {
            String string = step.toString();
            builder.append(CoreConstants.TAB).append(string);
            ThrowableProxyUtil.subjoinPackagingData(builder, step);
            builder.append(CoreConstants.LINE_SEPARATOR);
        }
        return builder.toString();
    }
}
