package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.Logger;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class LogbackSmallTest {
    @Test
    public void log() throws InterruptedException {
        Logger logger = (Logger) LoggerFactory.getLogger("logback_test");
        logger.info("OK");
        Thread.sleep(1000);
    }
}
