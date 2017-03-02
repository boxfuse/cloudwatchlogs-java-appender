package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchAppender;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEvent;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEventFactory;
import com.boxfuse.cloudwatchlogs.internal.LogEventWrapper;

/**
 * LogBack appender for Boxfuse's AWS CloudWatch Logs integration.
 */
public class CloudwatchLogsLogbackAppender extends AppenderBase<ILoggingEvent> {
    private CloudwatchAppender cloudwatchAppender = new CloudwatchAppender();

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
    protected void append(ILoggingEvent event) {
        LogEventWrapper logEventWrapper = new LogbackLogEventWrapper(event);
        CloudwatchLogsLogEvent logEvent = CloudwatchLogsLogEventFactory.getLogEvent(logEventWrapper);
        cloudwatchAppender.append(logEvent);
    }
}