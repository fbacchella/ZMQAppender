ZMQAppender
===========

A ØMQ (ZeroMQ, http://zeromq.org) Appender for log4j, that serialize Log4JEvent using the java serialization format.

Many options can be changed :

 * The options tied to ØMQ are
    * type: the socket type, either `PUB` or `PUSH`, default to `PUSH`
    * method: the socket connection method, either `connect` or `bind`, default to `connect`
    * endoint: the endpoint URL, like `tcp://localhost:2120`, default to `inproc://log4jappender`
    * hwm: the HWM for the socket, default to 1000
 * common options are
   * hostname, it will be added as a `hostname` property, default to the value of `java.net.InetAddress.getLocalHost().getHostName()`
   * locationInfo, true of false, it will send or not the log location (file, line, method), default to false
   * application, the application name, 


A simple declaration is :

    log4j.appender.A1=loghub.log4j.ZMQAppender
    log4j.appender.A1.endpoint=tcp://localhost:2120
    log4j.appender.A1.method=bind
    log4j.appender.A1.type=pub
    log4j.rootLogger=TRACE, A2
