GELFJ - A GELF Appender for Log4j and a GELF Handler for JDK Logging
====================================================================

Downloading
-----------

Add the following dependency section to your pom.xml:

    <dependencies>
      ...
      <dependency>
        <groupId>org.graylog2</groupId>
        <artifactId>gelfj</artifactId>
        <version>1.1.14</version>
        <scope>compile</scope>
      </dependency>
      ...
    </dependencies>

What is GELFJ
-------------

It's very simple GELF implementation in pure Java with the Log4j appender and JDK Logging Handler. It supports chunked messages which allows you to send large log messages (stacktraces, environment variables, additional fields, etc.) to a [Graylog2](http://www.graylog2.org/) server.

Following transports are supported:

 * TCP
 * UDP
 * HTTP
 * AMQP


How to use GELFJ
----------------

Drop the latest JAR into your classpath and configure your logging system to use it.

Options
-------

GelfAppender supports the following options:

- **targetURI**: Target uri where client sends the GELF messages to. Currently supported schemes are TCP, UDP, HTTP and AMQP
- **originHost**: Name of the originating host; defaults to the local hostname (*optional*)
- **extractStacktrace** (true/false): Add stacktraces to the GELF message; default true (*optional*)
- **addExtendedInformation** (true/false): Add extended information like Log4j's NDC/MDC; default false (*optional*)
- **fieldExtractor** (class name): Field extractor, which is used to extract additional fields from log events / records; default ReflectionFieldExtractor
- **includeLocation** (true/false): Include caller location. Generating caller location information is relatively slow and should be avoided unless execution speed is not an issue; default false (*optional*)
- **maxRetries** (integer): Maximum number of send retries; default 5 (*optional*)
- **threaded** (true/false): Dispatch messages using sender thread; default false (*optional*)
- **threadedQueueMaxDepth** (integer): Maximum queue depth; default 1000 (*optional*)
- **threadedQueueTimeout** (integer): Timeout in milliseconds for waiting free space in the queue; default 1000 (*optional*)

targetURI format
----------------

**UDP**

    udp://<host>[:<port>][?sendBufferSize=<size>&sendTimeout=<timeout in ms>]

**TCP**

    tcp://<host>[:<port>][?sendBufferSize=<size>&sendTimeout=<timeout in ms>&keepalive=<true/false>]

**HTTP**

    http://<host>[:<port>][/path][?sendBufferSize=<size>&sendTimeout=<timeout in ms>]

**AMQP**

    http://<host>[:<port>][?exchange=<exchange name>&routingKey=<routing key>]

Log4j appender
--------------

GelfAppender will use the log message as a short message and a stacktrace (if exception available) as a long message if "extractStacktrace" is true.

To use GELF Facility as appender in Log4j (XML configuration format):

    <appender name="graylog2" class="org.graylog2.log.GelfAppender">
        <param name="targetURI" value="udp://192.168.0.201:12001"/>
        <param name="originHost" value="my.machine.example.com"/>
        <param name="extractStacktrace" value="true"/>
        <param name="addExtendedInformation" value="true"/>
        <param name="Threshold" value="INFO"/>
        <param name="fields" value="environment=DEV, application=MyAPP"/>
    </appender>

and then add it as a one of appenders:

    <root>
        <priority value="INFO"/>
        <appender-ref ref="graylog2"/>
    </root>

Or, in the log4j.properties format:

    # Define the graylog2 destination
    log4j.appender.graylog2=org.graylog2.log.GelfAppender
    log4j.appender.graylog2.targetURI=tcp://graylog2.example.com:12001
    log4j.appender.graylog2.originHost=my.machine.example.com
    log4j.appender.graylog2.extractStacktrace=true
    log4j.appender.graylog2.addExtendedInformation=true
    log4j.appender.graylog2.fields=environment=DEV, application=MyAPP

    # Send all INFO logs to graylog2
    log4j.rootLogger=INFO, graylog2
    
Log4j layout
------------

Configured via log4j.properties:

    # Define the console destination
    log4j.appender.console=org.apache.log4j.ConsoleAppender
    log4j.appender.console.layout=org.graylog2.log.GelfLayout
    log4j.appender.console.layout.originHost=my.machine.example.com
    log4j.appender.console.layout.extractStacktrace=true
    log4j.appender.console.layout.addExtendedInformation=true
    log4j.appender.console.layout.fields=environment=DEV, application=MyAPP

    # Send all INFO logs to console
    log4j.rootLogger=INFO, console

Logging Handler
---------------

Configured via properties as a standard Handler like

    handlers = org.graylog2.logging.GelfHandler

    .level = ALL

    org.graylog2.logging.GelfHandler.level = ALL
    org.graylog2.logging.GelfHandler.targetURI = udp://syslog.example.com:12201
    #org.graylog2.logging.GelfHandler.extractStacktrace = true
    #org.graylog2.logging.GelfHandler.additionalField.0 = foo=bah
    #org.graylog2.logging.GelfHandler.additionalField.1 = foo2=bah2

    .handlers=org.graylog2.logging.GelfHandler
    
Logging Formatter
-----------------
Configured via properties

    handlers = java.util.logging.ConsoleHandler
    
    java.util.logging.ConsoleHandler.formatter=org.graylog2.logging.GelfFormatter
    
    org.graylog2.logging.GelfFormatter.extractStacktrace = true
    org.graylog2.logging.GelfFormatter.additionalField.0 = foo=bar
    org.graylog2.logging.GelfFormatter.additionalField.1 = bar=heck
    org.graylog2.logging.GelfFormatter.facility = test    

What is GELF
------------

The Graylog Extended Log Format (GELF) avoids the shortcomings of classic plain syslog:

- Limited to length of 1024 byte
- Not much space for payloads like stacktraces
- Unstructured. You can only build a long message string and define priority, severity etc.

You can get more information here: [http://www.graylog2.org/about/gelf](http://www.graylog2.org/about/gelf)
