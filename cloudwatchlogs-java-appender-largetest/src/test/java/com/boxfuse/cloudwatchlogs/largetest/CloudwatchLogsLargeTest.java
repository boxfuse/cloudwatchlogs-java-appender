package com.boxfuse.cloudwatchlogs.largetest;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CloudwatchLogsLargeTest {
    private static final int NUM_THREADS = 10;
    private static final int EVENTS_PER_THREAD = 100000;

    /**
     * Remove @Ignore to run.
     *
     * Test should complete sending 1,000,000 log events
     * in about 65-70 seconds
     * at an average rate of about 15,000 log events per second.
     */
    @Ignore
    @Test(timeout = 600000)
    public void concurrencyAndThroughput() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executorService.execute(new Runnable() {
                private final Logger log = LoggerFactory.getLogger(getClass().getName());

                @Override
                public void run() {
                    for (int j = 0; j < EVENTS_PER_THREAD; j++) {
                        log.debug("msg-" + j);
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        Thread.sleep(600000);
    }
}
