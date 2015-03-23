package loghub.log4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.log4j.spi.LoggingEvent;

public class JavaSerializer implements Serializer {

    @Override
    public byte[] objectToBytes(LoggingEvent event) throws IOException {
        // Done in org.apache.log4j.net.SocketAppender
        // Might be cargo cult
        event.getNDC();
        event.getThreadName();
        event.getMDCCopy();
        event.getRenderedMessage();
        event.getThrowableStrRep();
        event.getLocationInformation();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(buffer);
            oos.writeObject(event);
            oos.flush();
            byte bytes[] = buffer.toByteArray();
            oos.close();
            buffer.close();
            return bytes;
        }
        finally {
            if(oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                }
            }
            try {
                buffer.close();
            } catch (IOException e) {
            }
        }
    }

}
