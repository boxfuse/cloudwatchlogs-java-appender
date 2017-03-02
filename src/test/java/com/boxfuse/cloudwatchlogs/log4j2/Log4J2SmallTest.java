package com.boxfuse.cloudwatchlogs.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

public class Log4J2SmallTest {
    private static final String MESSAGE = "OK";

    @Test
    public void log() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        Logger logger = LogManager.getLogger("log4j2_test");
        logger.info(MESSAGE);
        Thread.sleep(1000);

        assertTrue(baos.toString("UTF-8").contains(MESSAGE));

        System.setOut(original);
    }
}
