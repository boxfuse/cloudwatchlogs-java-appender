package com.boxfuse.cloudwatchlogs.internal;

import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by kawnayeen on 3/2/17.
 */
public class CloudwatchAppender {
    private final CloudwatchLogsConfig config = new CloudwatchLogsConfig();
    private BlockingQueue<CloudwatchLogsLogEvent> eventQueue;
    private CloudwatchLogsLogEventPutter putter;
    private long discardedCount;

    public void start() {
        eventQueue = new LinkedBlockingQueue<>(config.getMaxEventQueueSize());
        putter = new CloudwatchLogsLogEventPutter(config, eventQueue);
        new Thread(putter).start();
    }

    public void stop() {
        putter.terminate();
    }

    public void append(CloudwatchLogsLogEvent logEvent) {
        while (!eventQueue.offer(logEvent)) {
            eventQueue.poll();
            discardedCount++;
        }
    }

    /**
     * @return The config of the appender. This instance can be modified to override defaults.
     */
    public CloudwatchLogsConfig getConfig() {
        return config;
    }

    /**
     * @return The number of log events that had to be discarded because the event queue was full.
     * If this number is non zero without having been affected by AWS CloudWatch Logs availability issues,
     * you should consider increasing maxEventQueueSize in the config to allow more log events to be buffer before having to drop them.
     */
    public long getDiscardedCount() {
        return discardedCount;
    }
}
