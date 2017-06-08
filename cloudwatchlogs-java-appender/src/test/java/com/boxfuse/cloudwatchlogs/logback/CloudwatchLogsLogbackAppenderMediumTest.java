package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.boxfuse.cloudwatchlogs.internal.CloudwatchLogsLogEventPutter;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudwatchLogsLogbackAppenderMediumTest {
    private static final int NUM_THREADS = 10;
    private static final int EVENTS_PER_THREAD = 100000;

    @Test(timeout = 15000)
    public void concurrencyAndThroughput() throws InterruptedException {
        final AWSLogs awsLogs = mock(AWSLogs.class);
        when(awsLogs.putLogEvents(any(PutLogEventsRequest.class))).thenReturn(new PutLogEventsResult());
        final CloudwatchLogsLogbackAppender appender = new CloudwatchLogsLogbackAppender() {
            @Override
            CloudwatchLogsLogEventPutter createCloudwatchLogsLogEventPutter() {
                return new CloudwatchLogsLogEventPutter(config, eventQueue, awsLogs, true);
            }
        };
        appender.setConfig(new CloudwatchLogsConfig());
        appender.start();

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);

        final LoggerContext loggerContext = new LoggerContext();

        for (int i = 0; i < NUM_THREADS; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String fqcn = "class-" + UUID.randomUUID();
                    Logger logger = loggerContext.getLogger(fqcn);
                    for (int j = 0; j < EVENTS_PER_THREAD; j++) {
                        appender.append(new LoggingEvent(fqcn, logger, Level.DEBUG, "msg-" + j, null, null));
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        while (NUM_THREADS * EVENTS_PER_THREAD > appender.putter.getProcessedCount()) {
            Thread.sleep(100);
        }

        appender.stop();

        assertEquals(0, appender.getDiscardedCount());
        assertEquals(NUM_THREADS * EVENTS_PER_THREAD, appender.getProcessedCount());
        assertEquals(NUM_THREADS * EVENTS_PER_THREAD, appender.putter.getProcessedCount());
    }
}
