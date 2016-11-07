# cloudwatchlogs-java-appender [![Build Status](https://api.travis-ci.org/boxfuse/cloudwatchlogs-java-appender.svg)](https://travis-ci.org/boxfuse/cloudwatchlogs-java-appender) [![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

The Boxfuse Java log appender for AWS CloudWatch Logs is a logging appender that ships your log events directly and securely to AWS CloudWatch Logs via HTTPS.

**Supported logging systems:** LogBack, Log4J2

All log events are *structured* and *standardized*. Each Boxfuse [environment](https://boxfuse.com/docs/environments) maps to an AWS CloudWatch Logs
*LogGroup* which contains one *LogStream* per application.

More info: https://boxfuse.com/blog/cloudwatch-logs

[![LogGroup and LogStream overview](https://boxfuse.com/assets/img/cloudwatch-logs.png)](https://boxfuse.com/blog/cloudwatch-logs) 

## Installation

To include the Boxfuse Java log appender for AWS CloudWatch Logs in your application all you need to do is include the dependency in your build file.

### Maven

Start by adding the Boxfuse Maven repository to your list of repositories in your `pom.xml`:

```
<repositories>
    <repository>
        <id>boxfuse-repo</id>
        <url>https://files.boxfuse.com</url>
    </repository>
    <repository>
        <id>central</id>
        <url>http://repo1.maven.org/maven2/</url>
    </repository>
</repositories>
```

Then add the dependency:

```
<dependency>
    <groupId>com.boxfuse.cloudwatchlogs</groupId>
    <artifactId>cloudwatchlogs-java-appender</artifactId>
    <version>1.0.0.9</version>
</dependency>
```

### Gradle

Start by adding the Boxfuse Maven repository to your list of repositories in your `build.gradle`:

```
repositories {
    mavenCentral()
    maven {
        url "https://files.boxfuse.com"
    }
}
```

Then add the dependency:

```
dependencies {
    compile 'com.boxfuse.cloudwatchlogs:cloudwatchlogs-java-appender:1.0.0.9'
}
```

## Usage

To use the appender you must add it to the configuration of your logging system.

### LogBack

Add the appender to your `logback.xml` file at the root of your classpath. In a Maven or Gradle project you can find it under src/main/resources :

```
<configuration>
    <appender name="Boxfuse-CloudwatchLogs" class="com.boxfuse.cloudwatchlogs.logback.CloudwatchLogsLogbackAppender"/>

    <root level="debug">
        <appender-ref ref="Boxfuse-CloudwatchLogs" />
    </root>
</configuration>
```

### Log4J2

Add the appender to your `log4j2.xml` file at the root of your classpath. In a Maven or Gradle project you can find it under src/main/resources :

```
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="com.boxfuse.cloudwatchlogs.log4j2">
  <Appenders>
    <Boxfuse-CloudwatchLogs/>
  </Appenders>
  <Loggers>
    <Root level="error">
      <AppenderRef ref="Boxfuse-CloudwatchLogs"/>
    </Root>
  </Loggers>
</Configuration>
```

## Implementation

The log events are shipped asynchronously on a separate background thread, leaving the performance of your application thread unaffected.
 To make this possible the appender buffers your messages in a concurrent bounded queue. By default the buffer allows for 1,000,000 messages.
 If the buffer fills up it will not expand further. This is done to prevent OutOfMemoryErrors. Instead log events are dropped in a FIFO fashion.
 
 If you are seeing dropped messages without having been affected by AWS CloudWatch Logs availability issues,
 you should consider increasing `maxEventQueueSize` in the config to allow more log events to be buffered before having to drop them.
 
## License

Copyright (C) 2016 Boxfuse GmbH

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.