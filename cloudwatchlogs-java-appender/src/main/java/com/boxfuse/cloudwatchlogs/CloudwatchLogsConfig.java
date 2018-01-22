package com.boxfuse.cloudwatchlogs;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * General configuration for the CloudWatch appender.
 */
public class CloudwatchLogsConfig {
    /**
     * The default size of the async log event queue.
     */
    public static final int DEFAULT_MAX_EVENT_QUEUE_SIZE = 1000000;

    /**
     * The default maximum delay in milliseconds before forcing a flush of the buffered log events to CloudWatch Logs.
     */
    public static final long DEFAULT_MAX_FLUSH_DELAY = 500;

    private int maxEventQueueSize = DEFAULT_MAX_EVENT_QUEUE_SIZE;

    private long maxFlushDelay = DEFAULT_MAX_FLUSH_DELAY;

    private boolean debug;
    private String endpoint = System.getenv("BOXFUSE_CLOUDWATCHLOGS_ENDPOINT");
    private String env = System.getenv("BOXFUSE_ENV");
    private String image = System.getenv("BOXFUSE_IMAGE_COORDINATES");
    private String instance = System.getenv("BOXFUSE_INSTANCE_ID");
    private String region = System.getenv("AWS_REGION");
    private String logGroup;
    private boolean stdoutFallback;
    private List<String> customMdcKeys = new ArrayList<>();

    public CloudwatchLogsConfig() {
        if (env == null) {
            env = "##unknown##";
        }
        if (image == null) {
            image = "##unknown##:##unknown##";
        }
        if (instance == null) {
            instance = getHostName();
        }
        logGroup = "boxfuse/" + env;
    }

    /**
     * @return Whether to fall back to stdout instead of disabling the appender when running outside of a Boxfuse instance. Default: false.
     */
    public boolean isStdoutFallback() {
        return stdoutFallback;
    }

    /**
     * @param stdoutFallback Whether to fall back to stdout instead of disabling the appender when running outside of a Boxfuse instance. Default: false.
     */
    public void setStdoutFallback(boolean stdoutFallback) {
        this.stdoutFallback = stdoutFallback;
    }

    /**
     * @return The maximum size of the async log event queue. Default: 1000000.
     * Increase to avoid dropping log events at very high throughput.
     * Decrease to reduce maximum memory usage at the risk if the occasional log event drop when it gets full.
     */
    public int getMaxEventQueueSize() {
        return maxEventQueueSize;
    }

    /**
     * @param maxEventQueueSize The maximum size of the async log event queue. Default: 1000000.
     *                          Increase to avoid dropping log events at very high throughput.
     *                          Decrease to reduce maximum memory usage at the risk if the occasional log event drop when it gets full.
     */
    public void setMaxEventQueueSize(int maxEventQueueSize) {
        if (maxEventQueueSize < 1) {
            throw new IllegalArgumentException("maxEventQueueSize may not be smaller than 1 but was " + maxEventQueueSize);
        }
        this.maxEventQueueSize = maxEventQueueSize;
    }

    /**
     * @return The maximum delay in milliseconds before forcing a flush of the buffered log events to CloudWatch Logs.
     */
    public long getMaxFlushDelay() {
        return maxFlushDelay;
    }

    /**
     * @param maxFlushDelay The default maximum delay in milliseconds before forcing a flush of the buffered log events to CloudWatch Logs.
     */
    public void setMaxFlushDelay(long maxFlushDelay) {
        if (maxFlushDelay < 1) {
            throw new IllegalArgumentException("maxFlushDelay may not be smaller than 1 but was " + maxFlushDelay);
        }
        this.maxFlushDelay = maxFlushDelay;
    }

    /**
     * @return The AWS CloudWatch Logs endpoint to connect to.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint The AWS CloudWatch Logs endpoint to connect to.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return The current Boxfuse environment.
     */
    public String getEnv() {
        return env;
    }

    /**
     * @param env The current Boxfuse environment.
     */
    public void setEnv(String env) {
        if (env == null) {
            throw new IllegalArgumentException("env may not be null");
        }
        this.env = env;
    }

    /**
     * @return The current Boxfuse image.
     */
    public String getImage() {
        return image;
    }

    /**
     * @param image The current Boxfuse image.
     */
    public void setImage(String image) {
        if (image == null) {
            throw new IllegalArgumentException("image may not be null");
        }
        this.image = image;
    }

    /**
     * @return The id of the current instance.
     */
    public String getInstance() {
        return instance;
    }

    /**
     * @param instance The id of the current instance.
     */
    public void setInstance(String instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance may not be null");
        }
        this.instance = instance;
    }

    /**
     * @return The AWS region to use.
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region The AWS region to use.
     */
    public void setRegion(String region) {
        if (region == null) {
            throw new IllegalArgumentException("region may not be null");
        }
        this.region = region;
    }

    /**
     * @return Whether internal debug output should produced. Only useful for diagnosing issues within the appender itself.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug Whether internal debug output should produced. Only useful for diagnosing issues within the appender itself.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return The AWS CloudWatch Logs LogGroup to use. This is determined automatically within Boxfuse environments.
     */
    public String getLogGroup() {
        return logGroup;
    }

    /**
     * @param logGroup The AWS CloudWatch Logs LogGroup to use. This is determined automatically within Boxfuse environments.
     */
    public void setLogGroup(String logGroup) {
        this.logGroup = logGroup;
    }

    /**
     * @return Custom MDC keys to include in the log events along with their values.
     */
    public List<String> getCustomMdcKeys() {
        return customMdcKeys;
    }

    /**
     * @param customMdcKey Custom MDC key to include in the log events along with its value.
     */
    public void addCustomMdcKey(String customMdcKey) {
        this.customMdcKeys.add(customMdcKey);
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException var1) {
            return getHostIp();
        }
    }

    private static String getHostIp() {
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            String backupCandidate = null;

            while (true) {
                NetworkInterface networkInterface;
                do {
                    if (!e.hasMoreElements()) {
                        if (backupCandidate != null) {
                            return backupCandidate;
                        }

                        return "<<unknown>>";
                    }

                    networkInterface = (NetworkInterface) e.nextElement();
                } while (!networkInterface.isUp());

                boolean firstChoice = !networkInterface.getName().contains("vboxnet") && !networkInterface.getName().contains("vmnet") && (networkInterface.getDisplayName() == null || !networkInterface.getDisplayName().contains("VirtualBox") && !networkInterface.getDisplayName().contains("VMware"));
                Enumeration inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = (InetAddress) inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        if (firstChoice) {
                            return inetAddress.getHostAddress();
                        }

                        backupCandidate = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            return "<<unknown>>";
        }
    }
}
