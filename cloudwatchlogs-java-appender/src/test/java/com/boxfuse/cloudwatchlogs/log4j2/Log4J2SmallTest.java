package com.boxfuse.cloudwatchlogs.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class Log4J2SmallTest {
    @Test
    public void log() throws Exception {
        ThreadContext.put("my-custom-key", "my-custom-value");
        ThreadContext.put("my-invalid-key", "my-invalid-value");
        checkLog("OK", null, "OK");
        checkLog("ABC", new IllegalArgumentException("XYZ"), "ABC\\njava.lang.IllegalArgumentException: XYZ");
    }

    private void checkLog(String inMsg, Throwable inEx, String outMsg) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        Logger logger = LogManager.getLogger("log4j2_test");
        logger.info(inMsg, inEx);
        Thread.sleep(1000);

        String stdOut = baos.toString("UTF-8");
        assertThat(stdOut, containsString(outMsg));
        assertThat(stdOut, containsString("my-custom-value"));
        assertThat(stdOut, not(containsString("my-invalid-value")));

        System.setOut(original);
    }
}
