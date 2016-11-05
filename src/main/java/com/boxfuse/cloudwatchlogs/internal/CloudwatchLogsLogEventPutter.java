package com.boxfuse.cloudwatchlogs.internal;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.amazonaws.services.logs.model.ServiceUnavailableException;
import com.boxfuse.cloudwatchlogs.CloudwatchLogsConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CloudwatchLogsLogEventPutter implements Runnable {
    private static final int MAX_FLUSH_DELAY = 500 * 1000 * 1000;
    private static final int MAX_BATCH_COUNT = 10000;
    private static final int MAX_BATCH_SIZE = 1 << 20;

    private final CloudwatchLogsConfig config;
    private final ConcurrentLinkedQueue<CloudwatchLogsLogEvent> eventQueue;
    private final AWSLogs logsClient;
    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private final boolean enabled;
    private boolean running;
    private String app;
    private String logGroupName;

    public CloudwatchLogsLogEventPutter(CloudwatchLogsConfig config, ConcurrentLinkedQueue<CloudwatchLogsLogEvent> eventQueue) {
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
            logsClient = new AWSLogsClient(new AnonymousAWSCredentials());
            logsClient.setEndpoint(config.getEndpoint());
        }
    }

    @Override
    public void run() {
        if (!enabled) {
            System.out.println("WARNING: AWS CloudWatch Logs appender is disabled (Unable to detect the AWS region and no CloudWatch Logs endpoint specified)");
            return;
        }

        running = true;
        String nextSequenceToken = null;
        List<InputLogEvent> eventBatch = new ArrayList<>();
        int batchSize = 0;
        long lastFlush = System.nanoTime();

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
                batchSize += eventJson.getBytes(StandardCharsets.UTF_8).length;
                int batchCount = eventBatch.size();
                if (batchCount >= MAX_BATCH_COUNT || batchSize >= MAX_BATCH_SIZE || lastFlush <= (System.nanoTime() - MAX_FLUSH_DELAY)) {
                    boolean retry;
                    do {
                        retry = false;
                        PutLogEventsRequest request =
                                new PutLogEventsRequest(logGroupName, app, eventBatch).withSequenceToken(nextSequenceToken);
                        try {
                            PutLogEventsResult result = logsClient.putLogEvents(request);
                            nextSequenceToken = result.getNextSequenceToken();
                        } catch (InvalidSequenceTokenException e) {
                            nextSequenceToken = e.getExpectedSequenceToken();
                            retry = true;
                        } catch (ServiceUnavailableException e) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e1) {
                                // Ignore
                            }
                            retry = true;
                        }
                    } while (retry);
                    eventBatch = new ArrayList<>();
                    batchSize = 0;
                    lastFlush = System.nanoTime();
                }

                eventBatch.add(new InputLogEvent().withMessage(eventJson).withTimestamp(event.getTimestamp()));
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
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
