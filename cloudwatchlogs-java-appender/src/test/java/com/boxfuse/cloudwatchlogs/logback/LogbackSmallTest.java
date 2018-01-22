package com.boxfuse.cloudwatchlogs.logback;

import ch.qos.logback.classic.Logger;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class LogbackSmallTest {
    @Test
    public void log() throws Exception {
        MDC.put("my-custom-key", "my-custom-value");
        MDC.put("my-invalid-key", "my-invalid-value");
        checkLog("OK", null, "OK");
        checkLog("ABC", new IllegalArgumentException("XYZ"), "ABC\\njava.lang.IllegalArgumentException: XYZ");
    }

    private void checkLog(String inMsg, Throwable inEx, String outMsg) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        Logger logger = (Logger) LoggerFactory.getLogger("logback_test");
        logger.info(inMsg, inEx);
        Thread.sleep(1000);

        String stdOut = baos.toString("UTF-8");
        assertThat(stdOut, containsString(outMsg));
        assertThat(stdOut, containsString("my-custom-value"));
        assertThat(stdOut, not(containsString("my-invalid-value")));

        System.setOut(original);
    }
}
