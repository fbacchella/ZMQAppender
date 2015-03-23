ZMQAppender
===========

A ØMQ (ZeroMQ, http://zeromq.org) Appender for log4j, that serialize Log4JEvent using the java serialization format.

ZeroMQ are super-magic socket that totally hides the danger and complexity of raw TCP socket. It reduces the message loses, and prevent 
the application hang because of slow log server, unlike the commonly used Socket Appender.

It serialise the Log4j events in native java format, to avoid inefficient common formats like json and allow to keep all the events data as is.

To install it, just run `mvn package` and add target/zmqappender-<version>-SNAPSHOT.jar to your project's classpath. The default configuration of method and type should fit comme usage where logs are send to a remote central server, so only endpoint should be changed.

Many options can be changed :

 * The options tied to ØMQ are
    * endoint: the endpoint URL, like `tcp://localhost:2120`, mandatory.
    * type: the socket type, either `PUB` or `PUSH`, default to `PUSH`.
    * method: the socket connection method, either `connect` or `bind`, default to `connect`.
    * hwm: the HWM for the socket, default to 1000.
 * common options are
   * hostname: it will be added as a `hostname` property, default to the value of `java.net.InetAddress.getLocalHost().getHostName()`.
   * locationInfo: true of false, it will send or not the log event location (file, line, method), default to false.
   * application: the application name, it's optionnal.
 * serializer: a class used to serialize Log4j's events to a byte array. It must implements loghub.log4j.Serializer class and provide a constructor with no arguments.

Two serializers are provided: loghub.log4j.JavaSerializer (using native java serialization)and loghub.log4j.MsgPackSerializer (serialized to a msgpack object, as a map)

A complete declaration is :

    log4j.appender.A1=loghub.log4j.ZMQAppender
    log4j.appender.A1.endpoint=tcp://localhost:2120
    log4j.appender.A1.method=connect
    log4j.appender.A1.type=pub
    log4j.appender.A1.hwm=1000
    log4j.appender.A1.hostname=myhost
    log4j.appender.A1.locationInfo=true
    log4j.appender.A1.serializer=loghub.log4j.JavaSerializer
    log4j.appender.A1.application=some_application_name
    log4j.rootLogger=TRACE, A2
