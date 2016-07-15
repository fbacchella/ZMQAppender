package loghub.log4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.spi.LoggingEvent;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

public class MsgPackSerializer implements Serializer {

    private static final class ValueMap extends HashMap<Value, Value> {
        public void put(String k, Value v) {
            if(v != null) {
                put(ValueFactory.newString(k), v);
            } else {
                put(ValueFactory.newString(k), ValueFactory.newNil());
            }
        }
        public void put(String k, String v) {
            if(v != null) {
                put(ValueFactory.newString(k), ValueFactory.newString(v));
            } else {
                put(ValueFactory.newString(k), ValueFactory.newNil());
            }
        }
        public void put(String k, List<String> v) {
            List<Value> elements = new ArrayList<Value>(v.size());
            for (String s: v) {
                elements.add(ValueFactory.newString(s));
            }
            put(ValueFactory.newString(k), ValueFactory.newArray(elements));
        }
        public void put(String k, Map<String, ?> v) {
            ValueMap elements = new ValueMap();
            for(Map.Entry<String, ?> e: v.entrySet()) {
                if(e.getValue() != null) {
                    elements.put(e.getKey(), e.getValue().toString());
                } else {
                    elements.put(e.getKey(), (String) null);
                }
            }
            put(ValueFactory.newString(k), ValueFactory.newMap(elements));
        }
    };

    @Override
    public byte[] objectToBytes(LoggingEvent event) throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        ValueMap eventMap = new ValueMap();
        eventMap.put("path", event.getLoggerName());
        eventMap.put("time_stamp", (Value) ValueFactory.newInteger(event.getTimeStamp()));
        eventMap.put("priority", event.getLevel().toString());
        eventMap.put("logger_name", event.getLoggerName());
        eventMap.put("thread", event.getThreadName());
        if(event.locationInformationExists()) {
            Map<String, String> locationinfo = new HashMap<String, String>(4);
            locationinfo.put("class", event.getLocationInformation().getClassName());
            locationinfo.put("file", event.getLocationInformation().getFileName());
            locationinfo.put("method", event.getLocationInformation().getMethodName());
            locationinfo.put("line", event.getLocationInformation().getLineNumber());
            eventMap.put("location_info", locationinfo);
        }
        eventMap.put("NDC", event.getNDC());
        if(event.getThrowableStrRep() != null) {
            List<String> stack = new ArrayList<String>();
            for(String l: event.getThrowableStrRep()) {
                stack.add(l.replace("\t", "    "));
            }
            eventMap.put("stack_trace", stack);
        }
        @SuppressWarnings("unchecked")
        Map<String, ?> m = event.getProperties();
        if(m.size() > 0) {
            eventMap.put("properties", m);
        }
        eventMap.put("message", event.getRenderedMessage());

        Value v = ValueFactory.newMap(eventMap);

        packer.packValue(v);
        packer.close();
        return packer.toByteArray();
    }

}
