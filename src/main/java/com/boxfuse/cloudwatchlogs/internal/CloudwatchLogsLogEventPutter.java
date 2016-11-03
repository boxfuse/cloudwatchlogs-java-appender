package com.boxfuse.cloudwatchlogs.internal;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
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
    private boolean running;
    private String app;
    private String logGroupName;

    public CloudwatchLogsLogEventPutter(CloudwatchLogsConfig config, ConcurrentLinkedQueue<CloudwatchLogsLogEvent> eventQueue) {
        this.config = config;
        logGroupName = "boxfuse/" + config.getEnv();
        app = config.getImage().substring(0, config.getImage().indexOf(":"));
        this.eventQueue = eventQueue;
        logsClient = new AWSLogsClient();

        if (config.getEndpoint() != null) {
            logsClient.setEndpoint(config.getEndpoint());
        }
        String awsRegion = System.getenv("AWS_REGION");
        if (awsRegion != null) {
            logsClient.setRegion(Region.getRegion(Regions.fromName(awsRegion)));
        }
    }

    @Override
    public void run() {
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
                eventMap.put("user", event.getUser());
                eventMap.put("session", event.getSession());
                eventMap.put("request", event.getRequest());

                String eventJson;
                try {
                    eventJson = objectMapper.writeValueAsString(eventMap);
                } catch (JsonProcessingException e) {
                    System.out.println("Unable to serialize log event: " + eventMap);
                    continue;
                }
                batchSize += eventJson.getBytes(StandardCharsets.UTF_8).length;
                int batchCount = eventBatch.size();
                if (batchCount >= MAX_BATCH_COUNT || batchSize >= MAX_BATCH_SIZE || lastFlush <= (System.nanoTime() - MAX_FLUSH_DELAY)) {
                    PutLogEventsRequest request = new PutLogEventsRequest(logGroupName, app, eventBatch).withSequenceToken(nextSequenceToken);
                    PutLogEventsResult result = logsClient.putLogEvents(request);
                    nextSequenceToken = result.getNextSequenceToken();
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

    public void terminate() {
        running = false;
    }
}
