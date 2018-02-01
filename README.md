# cloudwatchlogs-java-appender [![Build Status](https://api.travis-ci.org/boxfuse/cloudwatchlogs-java-appender.svg)](https://travis-ci.org/boxfuse/cloudwatchlogs-java-appender) [![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

The Boxfuse Java log appender for AWS CloudWatch Logs is a Logback and Log4J2 appender that ships your log events directly and securely to AWS CloudWatch Logs via HTTPS.

All log events are *structured* and *standardized*. Each Boxfuse [environment](https://boxfuse.com/docs/environments) maps to an AWS CloudWatch Logs
*LogGroup* which contains one *LogStream* per application.

More info: https://boxfuse.com/blog/cloudwatch-logs

[![LogGroup and LogStream overview](https://boxfuse.com/assets/img/cloudwatch-logs.png)](https://boxfuse.com/blog/cloudwatch-logs) 

## Supported logging systems

- **[Logback](http://logback.qos.ch/)** 1.1.3 and newer
- **[Log4J2](http://logging.apache.org/log4j/2.x/)** 2.7 and newer

## Installation

To include the Boxfuse Java log appender for AWS CloudWatch Logs in your application all you need to do is include the dependency in your build file.

### Maven

Start by adding the Boxfuse Maven repository to your list of repositories in your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central</id>
        <url>http://repo1.maven.org/maven2/</url>
    </repository>
    <repository>
        <id>boxfuse-repo</id>
        <url>https://files.boxfuse.com</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<dependency>
    <groupId>com.boxfuse.cloudwatchlogs</groupId>
    <artifactId>cloudwatchlogs-java-appender</artifactId>
    <version>1.1.9.61</version>
</dependency>
```

### Gradle

Start by adding the Boxfuse Maven repository to your list of repositories in your `build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven {
        url "https://files.boxfuse.com"
    }
}
```

Then add the dependency:

```groovy
dependencies {
    compile 'com.boxfuse.cloudwatchlogs:cloudwatchlogs-java-appender:1.1.9.61'
}
```

### Transitive dependencies

Besides Logback or Log4J2 this appender also requires the following dependency (declared as a transitive dependency in the `pom.xml`):

`com.amazonaws:aws-java-sdk-logs:1.1.143` (or newer)

## Usage

To use the appender you must add it to the configuration of your logging system.

### Logback

Add the appender to your `logback.xml` file at the root of your classpath. In a Maven or Gradle project you can find it under src/main/resources :

```xml
<configuration>
    <appender name="Boxfuse-CloudwatchLogs" class="com.boxfuse.cloudwatchlogs.logback.CloudwatchLogsLogbackAppender">
        <!-- Optional config parameters -->
        <config>
            <!-- Whether to fall back to stdout instead of disabling the appender when running outside of a Boxfuse instance. Default: false -->
            <stdoutFallback>false</stdoutFallback>
            
            <!-- The maximum size of the async log event queue. Default: 1000000.
                 Increase to avoid dropping log events at very high throughput.
                 Decrease to reduce maximum memory usage at the risk if the occasional log event drop when it gets full. -->
            <maxEventQueueSize>1000000</maxEventQueueSize>
                        
            <!-- The default maximum delay in milliseconds before forcing a flush of the buffered log events to CloudWatch Logs. Default: 500. -->
            <maxFlushDelay>500</maxFlushDelay>

            <!-- Custom MDC keys to include in the log events along with their values. -->            
            <customMdcKey>my-custom-key</customMdcKey>
            <customMdcKey>my-other-key</customMdcKey>

            <!-- The AWS CloudWatch Logs LogGroup to use. This is determined automatically within Boxfuse environments. -->
            <!--
            <logGroup>my-custom-log-group</logGroup>
            -->
        </config>    
    </appender>

    <root level="debug">
        <appender-ref ref="Boxfuse-CloudwatchLogs" />
    </root>
</configuration>
```

### Log4J2

Add the appender to your `log4j2.xml` file at the root of your classpath. In a Maven or Gradle project you can find it under src/main/resources :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="com.boxfuse.cloudwatchlogs.log4j2">
    <Appenders>
        <Boxfuse-CloudwatchLogs>
            <!-- Optional config parameters -->
            
            <!-- Whether to fall back to stdout instead of disabling the appender when running outside of a Boxfuse instance. Default: false -->
            <stdoutFallback>false</stdoutFallback>
            
            <!-- The maximum size of the async log event queue. Default: 1000000.
                 Increase to avoid dropping log events at very high throughput.
                 Decrease to reduce maximum memory usage at the risk if the occasional log event drop when it gets full. -->
            <maxEventQueueSize>1000000</maxEventQueueSize>
            
            <!-- The default maximum delay in milliseconds before forcing a flush of the buffered log events to CloudWatch Logs. Default: 500. -->
            <maxFlushDelay>500</maxFlushDelay>

            <!-- Custom MDC (ThreadContext) keys to include in the log events along with their values. -->            
            <customMdcKey key="my-custom-key"/>
            <customMdcKey key="my-other-key"/>

            <!-- The AWS CloudWatch Logs LogGroup to use. This is determined automatically within Boxfuse environments. -->
            <!--
            <logGroup>my-custom-log-group</logGroup>
            -->
        </Boxfuse-CloudwatchLogs>
    </Appenders>
    <Loggers>
        <Root level="debug">
            <AppenderRef ref="Boxfuse-CloudwatchLogs"/>
        </Root>
    </Loggers>
</Configuration>
```

## Standardized Structured Logging

All log events are *structured* and *standardized*. What this means is that instead of shipping log events as strings like this:

```
2014-03-05 10:57:51.702  INFO 45469 --- [ost-startStop-1] o.s.b.c.embedded.FilterRegistrationBean  : Mapping filter: 'hiddenHttpMethodFilter' to: [/*]
```

events are shipped as JSON documents will all required metadata:

```json
{
    "image": "myuser/myapp:123",
    "instance": "i-607b5ddc",
    "level": "INFO",
    "logger": "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping",
    "message": "Mapping filter: 'hiddenHttpMethodFilter' to: [/*]",
    "thread": "main"
}
```

This has several advantages:

- It cleanly separates presentation and formatting from log event content
- Log events are now machine searchable
- All log events from all applications now have exactly the same attributes, which enables searches across application boundaries

### Log streams and log groups

When the appender is run within a Boxfuse instance, it will send the log events to the AWS CloudWatch Logs *log group* for the
 current Boxfuse [environment](https://boxfuse.com/docs/environments). Within that *log group* the events will be placed
 in the *log stream* for the current Boxfuse application.


### Automatically populated attributes

A number of log event attributes are populated automatically when the appender is run within a Boxfuse instance:

- `image` is the current Boxfuse image
- `instance` is the current AWS instance id

When logging a message from your code using SLF4J as follows:

```java
Logger log = LoggerFactory.getLogger(MyClass.class);
...
log.info("My log message");
``` 
 
the timestamp of the log event is added to its metadata and the following attributes are also automatically extracted:

- `level` is the log level (`INFO` in this case)
- `logger` is the logger used (`com.mypkg.MyClass` in this case)
- `thread` that was logged from (`main` for the main application thread)
- `message` is the actual log message (`My log message` in this case)

When using an SLF4J marker you can also make it much easier to filter specific event types. The following code:

```java
Logger log = LoggerFactory.getLogger(MyClass.class);
Marker USER_CREATED = MarkerFactory.getMarker("USER_CREATED");
String username = "MyUser";
...
log.info(USER_CREATED, "Created user {}", username);
```

now also automatically defines an additional log event attribute:

- `event` which is the exact type of the event, making it easy to search and filter for this (`USER_CREATED` in this case)

### Optional additional attributes

Additionally a number of optional attributes can also be defined via MDC to provide further information of the log event:

- `account` is the current account in the system
- `action` is the current action in the system (for grouping log events all related to the same domain-specific thing like the current order for example)
- `user` is the user of the account (for systems with the concept of teams or multiple users per account)
- `session` is the ID of the current session of the user
- `request` is the ID of the request

They are populated in the MDC as follows:

```java
MDC.put(CloudwatchLogsMDCPropertyNames.ACCOUNT, "MyCurrentAccount");
MDC.put(CloudwatchLogsMDCPropertyNames.ACTION, "order-12345");
MDC.put(CloudwatchLogsMDCPropertyNames.USER, "MyUser");
MDC.put(CloudwatchLogsMDCPropertyNames.SESSION, "session-9876543210");
MDC.put(CloudwatchLogsMDCPropertyNames.REQUEST, "req-111222333");
```

When finishing processing (after sending out a response for example) they should be cleaned up again to prevent mixups:

```java
MDC.remove(CloudwatchLogsMDCPropertyNames.ACCOUNT);
MDC.remove(CloudwatchLogsMDCPropertyNames.ACTION);
MDC.remove(CloudwatchLogsMDCPropertyNames.USER);
MDC.remove(CloudwatchLogsMDCPropertyNames.SESSION);
MDC.remove(CloudwatchLogsMDCPropertyNames.REQUEST);
```

In a microservices architecture these attributes should be included in all requests sent between systems, to ensure they
 can be put in the MDC by each individual service in order to be correlated later. This is very powerful as it allows you to retrieve
  all the logs pertaining for example to a specific request across all microservices in your environment.

## Implementation

The log events are shipped asynchronously on a separate background thread, leaving the performance of your application thread unaffected.
 To make this possible the appender buffers your messages in a concurrent bounded queue. By default the buffer allows for 1,000,000 messages.
 If the buffer fills up it will not expand further. This is done to prevent OutOfMemoryErrors. Instead log events are dropped in a FIFO fashion.
 
 If you are seeing dropped messages without having been affected by AWS CloudWatch Logs availability issues,
 you should consider increasing `maxEventQueueSize` in the config to allow more log events to be buffered before having to drop them.

## Version History

### 1.1.9.61 (2018-02-01)

- Fixed `stdoutFallback` handling

### 1.1.8.60 (2018-01-22)

- Improved polling logic under high load
- Added optional `maxFlushDelay` configuration param
- Added optional `customMdcKey` configuration param

### 1.1.7.56 (2018-01-08)

- Added thread name
- Improved polling logic
- Added optional `logGroup` configuration param

### 1.1.6.49 (2017-09-19)

- Fixed: Handling of DataAlreadyAcceptedException

### 1.1.5.46 (2017-06-09)

- Prevent creation of AWS CloudWatch Logs client when disabled

### 1.1.4.40 (2017-06-08)

- Fixed: Flushing under high load caused maximum batch size to be exceeded 
- Fixed: Maximum batch size restored to 1,048,576 bytes 
- Added warning when an individual message exceeds the maximum allowed batch size

### 1.1.3.33 (2017-05-16)

- Fixed: Reduced maximum batch size to 1,000,000 bytes to avoid occasional batch size exceeded errors 

### 1.1.2.30 (2017-05-15)

- Fixed: Better handling of temporary network connectivity loss 
 
### 1.1.1.29 (2017-03-14)

- Fixed: Exception name is now part of the message along with the stacktrace 
 
### 1.1.0.23 (2017-03-02)

- Added `stdoutFallback` configuration property
- Fixed: Maximum batch size enforcement before flushing events to CloudWatch Logs 
 
### 1.0.3.20 (2017-01-04)

- Fixed: Do not let log thread die after an exception / auto-restart if possible
- Fixed: Enforce that all events within a single PutLogEvents call are always chronological 
 
### 1.0.2 (2016-11-02)

- Initial release
 
## License

Copyright (C) 2018 Boxfuse GmbH

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
