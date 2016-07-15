package loghub.log4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;

public class BasicTest {

    @Test
    public void testBasic() throws IOException {

        MsgPackSerializer ser = new MsgPackSerializer();

        LoggingEvent le = new LoggingEvent("Category", Logger.getLogger(BasicTest.class), 0, 
                Level.DEBUG, "a message", Thread.currentThread().getName(), null, null, new LocationInfo(new Exception(), BasicTest.class.getName()), null);
        byte[] bytes = ser.objectToBytes(le);

        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes);

        int count = unpacker.unpackMapHeader();
        Map<String, Object> message = new HashMap<String, Object>(count);
        for (int i=0 ; i < count ; i++) {
            String key = unpacker.unpackString();
            Value v = unpacker.unpackValue();
            message.put(key, v);
        }
        Assert.assertEquals("loghub.log4j.BasicTest", ((Value)message.get("path")).asStringValue().asString());
        Assert.assertEquals("DEBUG", ((Value)message.get("priority")).asStringValue().asString());
        Assert.assertEquals(BasicTest.class.getName(), ((Value)message.get("logger_name")).asStringValue().asString());
        Assert.assertEquals(Thread.currentThread().getName(), ((Value)message.get("thread")).asStringValue().asString());
        Assert.assertEquals(0, ((Value)message.get("time_stamp")).asNumberValue().toLong());
        MapValue mv = ((Value)message.get("location_info")).asMapValue();
        Assert.assertEquals(4, mv.size());
    }

}
