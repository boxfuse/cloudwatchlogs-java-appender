package com.boxfuse.cloudwatchlogs.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class Log4J2SmallTest {
    @Test
    public void log() throws InterruptedException {
        Logger logger = LogManager.getLogger("logback_test");
        logger.info("OK");
        Thread.sleep(1000);
    }
}
