package com.boxfuse.cloudwatchlogs.log4j2;

import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsMDCPropertyNames;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEvent;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEventPutter;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Log4J2 appender for Boxfuse's AWS CloudWatch Logs integration.
 */
@Plugin(name = CloudwatchLogsLog4J2Appender.APPENDER_NAME, category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class CloudwatchLogsLog4J2Appender extends AbstractAppender {
    static final String APPENDER_NAME = "Boxfuse-CloudwatchLogs";
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
            @PluginAttribute(value = "name", defaultString = APPENDER_NAME) String name,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("customMdcKey") final CustomMdcKeyElement[] customMdcKeys,
            @PluginAttribute("debug") Boolean debug,
            @PluginAttribute("stdoutFallback") Boolean stdoutFallback,
            @PluginAttribute("endpoint") String endpoint,
            @PluginAttribute("env") String env,
            @PluginAttribute("image") String image,
            @PluginAttribute("instance") String instance,
            @PluginAttribute(value = "maxEventQueueSize", defaultInt = CloudwatchLogsConfig.DEFAULT_MAX_EVENT_QUEUE_SIZE) Integer maxEventQueueSize,
            @PluginAttribute(value = "maxFlushDelay", defaultLong = CloudwatchLogsConfig.DEFAULT_MAX_FLUSH_DELAY) Long maxFlushDelay,
            @PluginAttribute("region") String region,
            @PluginAttribute("logGroup") String logGroup) {
        CloudwatchLogsLog4J2Appender appender = new CloudwatchLogsLog4J2Appender(name, filter, null, true);
        if (debug != null) {
            appender.getConfig().setStdoutFallback(debug);
        }
        if (stdoutFallback != null) {
            appender.getConfig().setStdoutFallback(stdoutFallback);
        }
        if (endpoint != null) {
            appender.getConfig().setEndpoint(endpoint);
        }
        if (env != null) {
            appender.getConfig().setEnv(env);
        }
        if (image != null) {
            appender.getConfig().setImage(image);
        }
        if (instance != null) {
            appender.getConfig().setInstance(instance);
        }
        appender.getConfig().setMaxEventQueueSize(maxEventQueueSize);
        appender.getConfig().setMaxFlushDelay(maxFlushDelay);
        if (region != null) {
            appender.getConfig().setRegion(region);
        }
        if (logGroup != null) {
            appender.getConfig().setLogGroup(logGroup);
        }
        for (CustomMdcKeyElement customMdcKey : customMdcKeys) {
            appender.getConfig().addCustomMdcKey(customMdcKey.getKey());
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
        putter = CloudwatchLogsLogEventPutter.create(config, eventQueue);
        Thread t = new Thread(putter, CloudwatchLogsLogEventPutter.class.getSimpleName());
        t.setDaemon(true);
        t.start();
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

    /**
     * @return Whether the background thread responsible for sending events to AWS is still running.
     */
    public boolean isRunning() {
        return putter.isRunning();
    }
    
    @Override
    public void append(LogEvent event) {
        StringBuilder message = new StringBuilder(event.getMessage().getFormattedMessage());
        Throwable thrown = event.getThrown();
        while (thrown != null) {
            message.append("\n").append(dump(thrown));
            thrown = thrown.getCause();
            if (thrown != null) {
                message.append("\nCaused by:");
            }
        }

        String account = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.ACCOUNT);
        String action = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.ACTION);
        String user = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.USER);
        String session = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.SESSION);
        String request = event.getContextData().getValue(CloudwatchLogsMDCPropertyNames.REQUEST);

        Marker marker = event.getMarker();
        String eventId = marker == null ? null : marker.getName();

        Map<String, String> customMdcAttributes = new HashMap<>();
        for (String key : config.getCustomMdcKeys()) {
            String value = event.getContextData().getValue(key);
            if (value != null) {
                customMdcAttributes.put(key, value);
            }
        }

        CloudwatchLogsLogEvent logEvent = new CloudwatchLogsLogEvent(event.getLevel().toString(), event.getLoggerName(),
                eventId, message.toString(), event.getTimeMillis(), event.getThreadName(), account, action, user,
                session, request, customMdcAttributes);
        while (!eventQueue.offer(logEvent)) {
            eventQueue.poll();
            discardedCount++;
        }
    }

    private String dump(Throwable throwableProxy) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwableProxy.getClass().getName()).append(": ").append(throwableProxy.getMessage()).append("\n");
        for (StackTraceElement step : throwableProxy.getStackTrace()) {
            String string = step.toString();
            builder.append("\t").append(string);
            builder.append(step);
            builder.append("\n");
        }
        return builder.toString();
    }
}
