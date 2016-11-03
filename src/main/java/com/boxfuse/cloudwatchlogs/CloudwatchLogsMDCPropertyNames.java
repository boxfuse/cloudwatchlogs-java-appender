package com.boxfuse.cloudwatchlogs;

/**
 * Standard property names for CloudwatchLogs appender MDC.
 */
public class CloudwatchLogsMDCPropertyNames {
    private static final String PREFIX = "BOXFUSE_CLOUDWATCHLOGS_";

    /**
     * The current account in the system.
     */
    public static final String ACCOUNT = PREFIX + "ACCOUNT";

    /**
     * The current action in the system. (optional, for things like orders, ...)
     */
    public static final String ACTION = PREFIX + "ACTION";

    /**
     * The user of the account. (optional, for systems with the concept of teams or multiple users per account)
     */
    public static final String USER = PREFIX + "USER";

    /**
     * The id of the current session of the user or account.
     */
    public static final String SESSION = PREFIX + "SESSION";

    /**
     * The id of the current request.
     */
    public static final String REQUEST = PREFIX + "REQUEST";

    private CloudwatchLogsMDCPropertyNames() {
    }
}
