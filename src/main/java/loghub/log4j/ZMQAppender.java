package loghub.log4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.zeromq.ZMQ;

/**
 * Sends {@link LoggingEvent} objects to a remote a ØMQ socket,.
 * 
 * <p>Remote logging is non-intrusive as far as the log event is concerned. In other
 *    words, the event will be logged with the same time stamp, {@link
 *    org.apache.log4j.NDC}, location info as if it were logged locally by
 *    the client.
 * 
 *  <p>SocketAppenders do not use a layout. They ship a
 *     serialized {@link LoggingEvent} object to the server side.
 * 
 * @author Fabrice Bacchella
 *
 */
public class ZMQAppender extends AppenderSkeleton {

    private enum Method {
        CONNECT {
            @Override
            void act(ZMQ.Socket socket, String address) { socket.connect(address); }
        },
        BIND {
            @Override
            void act(ZMQ.Socket socket, String address) { socket.bind(address); }
        };
        abstract void act(ZMQ.Socket socket, String address);
    }

    private enum ZMQSocketType {
        PUSH(ZMQ.PUSH),
        PUB(ZMQ.PUB);
        public final int type;
        ZMQSocketType(int type) {
            this.type = type;
        }
    }

    private ZMQ.Socket socket;
    // If the appender uses it's own context, it must terminate it itself
    private final boolean localCtx;
    private final ZMQ.Context ctx;
    private String hostname;
    private ZMQSocketType type = ZMQSocketType.PUSH;
    private Method method = Method.CONNECT;
    private String endpoint = "inproc://log4jappender";
    boolean locationInfo = false;
    private String application;
    private long hwm = 1000;

    public ZMQAppender() {
        ctx = ZMQ.context(1);
        localCtx = true;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    public ZMQAppender(ZMQ.Context ctx) {
        this.ctx = ctx;
        localCtx = false;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void activateOptions() {
        super.activateOptions();
        socket = ctx.socket(type.type);
        socket.setLinger(1);
        socket.setHWM(hwm);
        method.act(socket, endpoint);
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
        }
        if(localCtx) {
            ctx.term();
        }
    }

    public boolean requiresLayout() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void append(LoggingEvent event) {
        if ( closed) {
            return;            
        }
        try {
            // The event is copied, because a host field is added in the properties
            LoggingEvent modifiedEvent = new LoggingEvent(event.getFQNOfLoggerClass(), event.getLogger(), event.getTimeStamp(), event.getLevel(), event.getMessage(),
                    event.getThreadName(), event.getThrowableInformation(), event.getNDC(), locationInfo ? event.getLocationInformation() : null,
                            new HashMap<String,String>(event.getProperties()));

            // Done in org.apache.log4j.net.SocketAppender
            // Might be cargo cult
            event.getNDC();
            event.getThreadName();
            event.getMDCCopy();
            event.getRenderedMessage();
            event.getThrowableStrRep();

            if (application != null) {
                event.setProperty("application", application);
            }
            modifiedEvent.setProperty("hostname", hostname);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(modifiedEvent);
            oos.flush();

            socket.send(buffer.toByteArray());
            oos.close();
            buffer.close();
        } catch (zmq.ZError.IOException e ) {
            try {
                socket.close();
            } catch (Exception e1) {
            }
        } catch (java.nio.channels.ClosedSelectorException e ) {
            try {
                socket.close();
            } catch (Exception e1) {
            }
        } catch (org.zeromq.ZMQException e ) {
            try {
                socket.close();
            } catch (Exception e1) {
            }
        } catch (IOException e) {
            errorHandler.error(e.getMessage(), e, ErrorCode.GENERIC_FAILURE);
            LogLog.error(e.getMessage());
        }
    }

    /**
     * Define the ØMQ socket type. Current allowed value are PUB or PUSH.
     * 
     * @param type
     */
    public void setType(String type) {
        try {
            this.type = ZMQSocketType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            String msg = "[" + type + "] should be one of [PUSH, PUB]" + ", using default ØMQ socket type, PUSH by default.";
            errorHandler.error(msg, e, ErrorCode.GENERIC_FAILURE);
            LogLog.error(msg);
        }
    }

    /**
     * @return the ØMQ socket type.
     */
    public String getType() {
        return type.toString();
    }

    /**
     * The <b>method</b> define the connection method for the ØMQ socket. It can take the value
     * connect or bind, it's case insensitive.
     * @param method
     */
    public void setMethod(final String method) {
        try {
            this.method = Method.valueOf(method.toUpperCase());
        } catch (Exception e) {
            String msg = "[" + type + "] should be one of [connect, bind]" + ", using default ØMQ socket type, connect by default.";
            errorHandler.error(msg, e, ErrorCode.GENERIC_FAILURE);
            LogLog.error(msg);
        }
    }


    /**
     * @return the 0MQ socket connection method.
     */
    public String getMethod() {
        return method.name();
    }

    /**
     * The <b>endpoint</b> take a string value. It's the ØMQ socket endpoint.
     * @param endpoint
     */
    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return the ØMQ socket endpoint.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * The <b>Hostname</b> take a string value. The default value is resolved using InetAddress.getLocalHost().getHostName().
     * It will be send in a custom hostname property.
     * @param hostname
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return value of the <b>Hostname</b> option.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * The <b>LocationInfo</b> option takes a boolean value. If true,
     * the information sent to the remote host will include location
     * information. By default no location information is sent to the server.
     */
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    /**
     * @return value of the <b>LocationInfo</b> option.
     */
    public boolean getLocationInfo() {
        return locationInfo;
    }

    /**
     * The <b>App</b> option takes a string value which should be the name of the 
     * application getting logged.
     * If property was already set (via system property), don't set here.
     */
    public void setApplication(String lapp) {
        this.application = lapp;
    }

    /**
     *  @return value of the <b>Application</b> option.
     */
    public String getApplication() {
        return application;
    }

    /**
     * The <b>hwm</b> option define the ØMQ socket HWM (high water mark).
     */
    public void setHwm(long hwm) {
        this.hwm = hwm;
    }

    /**
     * @return the ØMQ socket HWM.
     */
    public long getHwm() {
        return hwm;
    }

}
