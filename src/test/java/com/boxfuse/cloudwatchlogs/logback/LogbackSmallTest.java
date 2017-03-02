package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.Logger;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

public class LogbackSmallTest {
    private static final String MESSAGE = "OK";

    @Test
    public void log() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        Logger logger = (Logger) LoggerFactory.getLogger("logback_test");
        logger.info(MESSAGE);
        Thread.sleep(1000);

        assertTrue(baos.toString("UTF-8").contains(MESSAGE));

        System.setOut(original);
    }
}
