package com.boxfuse.cloudwatchlogs;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * General configuration for the CloudWatch appender.
 */
public class CloudwatchLogsConfig {
    private int maxEventQueueSize = 1000000;

    private String endpoint = System.getenv("BOXFUSE_CLOUDWATCHLOGS_ENDPOINT");
    private String env = System.getenv("BOXFUSE_ENV");
    private String image = System.getenv("BOXFUSE_IMAGE_COORDINATES");
    private String instance = System.getenv("BOXFUSE_INSTANCE_ID");

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
            throw new IllegalArgumentException("maxEventQueueSize may not be smaller than 1");
        }
        this.maxEventQueueSize = maxEventQueueSize;
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
        } catch (SocketException var6) {
            return "<<unknown>>";
        }
    }
}
