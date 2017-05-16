package com.boxfuse.cloudwatchlogs.internal;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;

public class CloudwatchLogsLogEventPutter implements Runnable {
    private static final int MAX_FLUSH_DELAY = 500 * 1000 * 1000;
    private static final int MAX_BATCH_COUNT = 10000;
    private static final int MAX_BATCH_SIZE = 1000000;

    private final CloudwatchLogsConfig config;
    private final BlockingQueue<CloudwatchLogsLogEvent> eventQueue;
    private final AWSLogs logsClient;
    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private final boolean enabled;
    private boolean running;
    private String app;
    private String logGroupName;
    private int batchSize;
    private long lastFlush;
    private List<InputLogEvent> eventBatch;
    private String nextSequenceToken;

    public CloudwatchLogsLogEventPutter(CloudwatchLogsConfig config, BlockingQueue<CloudwatchLogsLogEvent> eventQueue) {
        this.config = config;
        logGroupName = "boxfuse/" + config.getEnv();
        String image = config.getImage();
        app = image.substring(0, image.indexOf(":"));
        this.eventQueue = eventQueue;

        String awsRegion = System.getenv("AWS_REGION");
        enabled = awsRegion != null || config.getEndpoint() != null;
        if (config.getEndpoint() == null) {
            logsClient = new AWSLogsClient();
            if (awsRegion != null) {
                logsClient.setRegion(Region.getRegion(Regions.fromName(awsRegion)));
            }
        } else {
            // Non-AWS mock endpoint
            logsClient = new AWSLogsClient(new AnonymousAWSCredentials());
            logsClient.setEndpoint(config.getEndpoint());
        }
    }

    @Override
    public void run() {
        if (!enabled && !config.isStdoutFallback()) {
            System.out.println("WARNING: AWS CloudWatch Logs appender is disabled (Unable to detect the AWS region and no CloudWatch Logs endpoint specified)");
            return;
        }

        running = true;
        nextSequenceToken = null;
        eventBatch = new ArrayList<>();
        batchSize = 0;
        lastFlush = System.nanoTime();

        while (running) {
            CloudwatchLogsLogEvent event = eventQueue.poll();
            if (event != null) {
                Map<String, Object> eventMap = new TreeMap<>();
                eventMap.put("instance", config.getInstance());
                eventMap.put("image", config.getImage());
                eventMap.put("level", event.getLevel());
                eventMap.put("event", event.getEvent());
                eventMap.put("message", event.getMessage());
                eventMap.put("logger", event.getLogger());
                eventMap.put("thread", event.getThread());
                eventMap.put("account", event.getAccount());
                eventMap.put("action", event.getAction());
                eventMap.put("user", event.getUser());
                eventMap.put("session", event.getSession());
                eventMap.put("request", event.getRequest());

                String eventJson;
                try {
                    eventJson = toJson(eventMap);
                } catch (JsonProcessingException e) {
                    System.out.println("Unable to serialize log event: " + eventMap);
                    continue;
                }

                // Source: http://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html
                // The maximum batch size is 1,048,576 bytes,
                int eventSize =
                        // and this size is calculated as the sum of all event messages in UTF-8,
                        eventJson.getBytes(StandardCharsets.UTF_8).length
                                // plus 26 bytes for each log event.
                                + 26;

                if ((eventBatch.size() + 1) >= MAX_BATCH_COUNT || (batchSize + eventSize) >= MAX_BATCH_SIZE) {
                    flush();
                }

                eventBatch.add(new InputLogEvent().withMessage(eventJson).withTimestamp(event.getTimestamp()));
                batchSize += eventSize;
            } else {
                flush();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        }
    }

    private boolean isTimeToFlush() {
        return lastFlush <= (System.nanoTime() - MAX_FLUSH_DELAY);
    }

    private void flush() {
        if (!eventBatch.isEmpty() && isTimeToFlush()) {
            Collections.sort(eventBatch, new Comparator<InputLogEvent>() {
                @Override
                public int compare(InputLogEvent o1, InputLogEvent o2) {
                    return o1.getTimestamp().compareTo(o2.getTimestamp());
                }
            });
            if (!enabled && config.isStdoutFallback()) {
                for (InputLogEvent event : eventBatch) {
                    System.out.println(new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS").format(event.getTimestamp())
                            + " " + logGroupName + " " + app + " " + event.getMessage());
                }
            } else {
                int retries = 15;
                do {
                    PutLogEventsRequest request =
                            new PutLogEventsRequest(logGroupName, app, eventBatch).withSequenceToken(nextSequenceToken);
                    try {
                        PutLogEventsResult result = logsClient.putLogEvents(request);
                        nextSequenceToken = result.getNextSequenceToken();
                        break;
                    } catch (InvalidSequenceTokenException e) {
                        nextSequenceToken = e.getExpectedSequenceToken();
                    } catch (SdkClientException e) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            // Ignore
                        }
                        if (--retries == 0) {
                            System.out.println(
                                    new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS").format(new Date())
                                            + " Unable to send logs to AWS CloudWatch Logs (" + e.getMessage()
                                            + "). Dropping log events batch ...");
                        }
                    }
                } while (retries > 0);
            }
            eventBatch = new ArrayList<>();
            batchSize = 0;
            lastFlush = System.nanoTime();
        }
    }

    /* private -> for testing */ String toJson(Map<String, Object> eventMap) throws JsonProcessingException {
        // Compensate for https://github.com/FasterXML/jackson-databind/issues/1442
        Map<String, Object> nonNullMap = new TreeMap<>();
        for (Map.Entry<String, Object> entry : eventMap.entrySet()) {
            if (entry.getValue() != null) {
                nonNullMap.put(entry.getKey(), entry.getValue());
            }
        }
        return objectMapper.writeValueAsString(nonNullMap);
    }

    public void terminate() {
        running = false;
    }
}
