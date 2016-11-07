package com.boxfuse.cloudwatchlogs.log4j2;

import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsMDCPropertyNames;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEvent;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEventPutter;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Log4J2 appender for Boxfuse's AWS CloudWatch Logs integration.
 */
@Plugin(name="Boxfuse-CloudwatchLogs", category="Core", elementType="appender", printObject=true)
public class CloudwatchLogsLog4J2Appender extends AbstractAppender {
    private final CloudwatchLogsConfig config = new CloudwatchLogsConfig();
    private BlockingQueue<CloudwatchLogsLogEvent> eventQueue;
    private CloudwatchLogsLogEventPutter putter;
    private long discardedCount;

    public CloudwatchLogsLog4J2Appender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    public CloudwatchLogsLog4J2Appender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    // Your custom appender needs to declare a factory method
    // annotated with `@PluginFactory`. Log4j will parse the configuration
    // and call this factory method to construct an appender instance with
    // the configured attributes.
    @PluginFactory
    public static CloudwatchLogsLog4J2Appender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("maxEventQueueSize") Integer maxEventQueueSize) {
        CloudwatchLogsLog4J2Appender appender = new CloudwatchLogsLog4J2Appender(name, filter, null, true);
        if (maxEventQueueSize != null) {
            appender.getConfig().setMaxEventQueueSize(maxEventQueueSize);
        }
        return appender;
    }

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
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        Throwable thrown = event.getThrown();
        while (thrown != null) {
            message += "\n" + dump(thrown);
            thrown = thrown.getCause();
            if (thrown != null) {
                message += "\nCaused by:";
            }
        }

        String account = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.ACCOUNT);
        String action = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.ACTION);
        String user = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.USER);
        String session = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.SESSION);
        String request = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.REQUEST);

        Marker marker = event.getMarker();
        String eventId = marker == null ? null : marker.getName();

        CloudwatchLogsLogEvent logEvent = new CloudwatchLogsLogEvent(event.getLevel().toString(), event.getLoggerName(), eventId, message, event.getTimeMillis(), event.getThreadName(), account, action, user, session, request);
        while (!eventQueue.offer(logEvent)) {
            eventQueue.poll();
            discardedCount++;
        }
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
