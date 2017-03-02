package com.boxfuse.cloudwatchlogs.log4j2;

import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchAppender;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEvent;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEventFactory;
import com.boxfuse.cloudwatchlogs.internal.LogEventWrapper;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

/**
 * Log4J2 appender for Boxfuse's AWS CloudWatch Logs integration.
 */
@Plugin(name = CloudwatchLogsLog4J2Appender.APPENDER_NAME, category = "Core", elementType = "appender", printObject = true)
public class CloudwatchLogsLog4J2Appender extends AbstractAppender {
    static final String APPENDER_NAME = "Boxfuse-CloudwatchLogs";
    private CloudwatchAppender cloudwatchAppender = new CloudwatchAppender();

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
            @PluginAttribute(value = "name", defaultString = APPENDER_NAME) String name,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute(value = "maxEventQueueSize", defaultInt = CloudwatchLogsConfig.DEFAULT_MAX_EVENT_QUEUE_SIZE) Integer maxEventQueueSize) {
        CloudwatchLogsLog4J2Appender appender = new CloudwatchLogsLog4J2Appender(name, filter, null, true);
        appender.getConfig().setMaxEventQueueSize(maxEventQueueSize);
        return appender;
    }

    /**
     * @return The config of the appender. This instance can be modified to override defaults.
     */
    public CloudwatchLogsConfig getConfig() {
        return cloudwatchAppender.getConfig();
    }

    @Override
    public void start() {
        super.start();
        cloudwatchAppender.start();
    }

    @Override
    public void stop() {
        cloudwatchAppender.stop();
        super.stop();
    }

    /**
     * @return The number of log events that had to be discarded because the event queue was full.
     * If this number is non zero without having been affected by AWS CloudWatch Logs availability issues,
     * you should consider increasing maxEventQueueSize in the config to allow more log events to be buffer before having to drop them.
     */
    public long getDiscardedCount() {
        return cloudwatchAppender.getDiscardedCount();
    }

    @Override
    public void append(LogEvent event) {
        LogEventWrapper logEventWrapper = new Log4j2LogEventWrapper(event);
        CloudwatchLogsLogEvent logEvent = CloudwatchLogsLogEventFactory.getLogEvent(logEventWrapper);
        cloudwatchAppender.append(logEvent);
    }
}