package com.boxfuse.cloudwatchlogs.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Log4J2 appender config for Boxfuse's AWS CloudWatch Logs integration.
 */
@Plugin(name = "customMdcKey", category = "Core", printObject = true)
public class CustomMdcKeyElement {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final String key;

    private CustomMdcKeyElement(String key) {
        this.key = key;
    }

    @PluginFactory
    public static CustomMdcKeyElement createCustomMdcKey(@PluginAttribute("key") final String key) {
        if (Strings.isEmpty(key)) {
            LOGGER.error("customMdcKey needs a key and cannot be empty.");
            return null;
        }
        return new CustomMdcKeyElement(key);
    }

    public String getKey() {
        return key;
    }
}
