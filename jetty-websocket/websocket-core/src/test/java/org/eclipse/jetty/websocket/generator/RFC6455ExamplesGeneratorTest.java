package org.eclipse.jetty.websocket.generator;


import java.nio.ByteBuffer;

import org.eclipse.jetty.io.StandardByteBufferPool;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.websocket.ByteBufferAssert;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.frames.BinaryFrame;
import org.eclipse.jetty.websocket.frames.PingFrame;
import org.eclipse.jetty.websocket.frames.PongFrame;
import org.eclipse.jetty.websocket.frames.TextFrame;
import org.junit.Test;

public class RFC6455ExamplesGeneratorTest
{
    StandardByteBufferPool bufferPool = new StandardByteBufferPool();


    @Test
    public void testFragmentedUnmaskedTextMessage()
    {

        TextFrame text1 = new TextFrame();
        TextFrame text2 = new TextFrame();

        text1.setFin(false);
        text2.setFin(true);
        text2.setContinuation(true);
        text1.setPayload("Hel");
        text2.setPayload("lo");

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);

        TextFrameGenerator generator = new TextFrameGenerator(bufferPool,policy);

        ByteBuffer actual1 = generator.generate(text1);
        ByteBuffer actual2 = generator.generate(text2);

        ByteBuffer expected1 = ByteBuffer.allocate(5);

        expected1.put(new byte[]
                { (byte)0x01, (byte)0x03, (byte)0x48, (byte)0x65, (byte)0x6c });

        ByteBuffer expected2 = ByteBuffer.allocate(4);

        expected2.put(new byte[]
                { (byte)0x80, (byte)0x02, (byte)0x6c, (byte)0x6f });

        expected1.flip();
        actual1.flip();
        expected2.flip();
        actual2.flip();

        ByteBufferAssert.assertEquals("t1 buffers are not equal", expected1, actual1);
        ByteBufferAssert.assertEquals("t2 buffers are not equal", expected2, actual2);
    }

    @Test
    public void testSingleMaskedPongRequest()
    {
        PongFrame pong = new PongFrame();
        pong.setMask(new byte[]
                { 0x37, (byte)0xfa, 0x21, 0x3d });

        byte msg[] = "Hello".getBytes(StringUtil.__UTF8_CHARSET);
        ByteBuffer payload = ByteBuffer.allocate(msg.length);
        payload.put(msg);
        pong.setPayload(payload);

        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        PongFrameGenerator gen = new PongFrameGenerator(bufferPool,policy);

        ByteBuffer actual = gen.generate(pong);
        actual.flip(); // make readable

        ByteBuffer expected = ByteBuffer.allocate(11);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // Unmasked Pong request
        expected.put(new byte[]
                { (byte)0x8a, (byte)0x85, 0x37, (byte)0xfa, 0x21, 0x3d, 0x7f, (byte)0x9f, 0x4d, 0x51, 0x58 });
        expected.flip(); // make readable

        ByteBufferAssert.assertEquals("pong buffers are not equal",expected,actual);
    }

    @Test
    public void testSingleMaskedTextMessage()
    {
        TextFrame text = new TextFrame();
        text.setPayload("Hello");
        text.setFin(true);
        text.setMask(new byte[]
                { 0x37, (byte)0xfa, 0x21, 0x3d });

        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();

        TextFrameGenerator gen = new TextFrameGenerator(bufferPool,policy);

        ByteBuffer actual = gen.generate(text);
        actual.flip(); // make readable

        ByteBuffer expected = ByteBuffer.allocate(11);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // A single-frame masked text message
        expected.put(new byte[]
                { (byte)0x81, (byte)0x85, 0x37, (byte)0xfa, 0x21, 0x3d, 0x7f, (byte)0x9f, 0x4d, 0x51, 0x58 });
        expected.flip(); // make readable

        ByteBufferAssert.assertEquals("masked text buffers are not equal",expected,actual);
    }




    @Test
    public void testSingleUnmaskedPingRequest() throws Exception
    {
        PingFrame ping = new PingFrame();

        byte msg[] = "Hello".getBytes(StringUtil.__UTF8_CHARSET);
        ByteBuffer payload = ByteBuffer.allocate(msg.length);
        payload.put(msg);
        ping.setPayload(payload);

        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();

        PingFrameGenerator gen = new PingFrameGenerator(bufferPool,policy);
        ByteBuffer actual = gen.generate(ping);
        actual.flip(); // make readable

        ByteBuffer expected = ByteBuffer.allocate(10);
        expected.put(new byte[]
                { (byte)0x89, (byte)0x05, (byte)0x48, (byte)0x65, (byte)0x6c, (byte)0x6c, (byte)0x6f });
        expected.flip(); // make readable

        ByteBufferAssert.assertEquals("Ping buffers",expected,actual);
    }

    @Test
    public void testSingleUnmaskedTextMessage()
    {
        TextFrame text = new TextFrame("Hello");
        text.setFin(true);

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);

        TextFrameGenerator generator = new TextFrameGenerator(bufferPool,policy);

        ByteBuffer actual = generator.generate(text);

        ByteBuffer expected = ByteBuffer.allocate(10);

        expected.put(new byte[]
                { (byte)0x81, (byte)0x05, (byte)0x48, (byte)0x65, (byte)0x6c, (byte)0x6c, (byte)0x6f });

        expected.flip();
        actual.flip();

        ByteBufferAssert.assertEquals("t1 buffers are not equal", expected, actual);
    }
    
    @Test
    public void testSingleUnmasked256ByteBinaryMessage()
    {
        int dataSize = 256;

        BinaryFrame binary = new BinaryFrame();
        binary.setFin(true);
        ByteBuffer payload = ByteBuffer.allocate(dataSize);
        for (int i = 0; i < dataSize; i++)
        {
            payload.put((byte)0x44);
        }
        binary.setPayload(payload);

        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        BinaryFrameGenerator gen = new BinaryFrameGenerator(bufferPool,policy);

        ByteBuffer actual = gen.generate(binary);

        ByteBuffer expected = ByteBuffer.allocate(dataSize + 10);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // 256 bytes binary message in a single unmasked frame
        expected.put(new byte[]
                { (byte)0x82, (byte)0x7E });
        expected.putShort((short)0x01_00);

        for (int i = 0; i < dataSize; i++)
        {
            expected.put((byte)0x44);
        }

        actual.flip();
        expected.flip();

        // System.out.println(binary);

        // System.out.println(BufferUtil.toDetailString(expected));
        // System.out.println(BufferUtil.toDetailString(actual));

        // for (int i = 0; i < 20; ++i)
        // {
        // System.out.printf("a [%2d] 0x%02x%n",i,actual.get());
        // System.out.printf("e [%2d] 0x%02x%n",i,expected.get());
        // }

        ByteBufferAssert.assertEquals("binary buffers are not equal", expected, actual);
    }

    
    @Test
    public void testSingleUnmasked64KBinaryMessage()
    {
        int dataSize = 1024 * 64;

        BinaryFrame binary = new BinaryFrame();
        binary.setFin(true);
        ByteBuffer payload = ByteBuffer.allocate(dataSize);
        for (int i = 0; i < dataSize; i++)
        {
            payload.put((byte)0x44);
        }
        binary.setPayload(payload);

        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        BinaryFrameGenerator gen = new BinaryFrameGenerator(bufferPool,policy);

        ByteBuffer actual = gen.generate(binary);

        ByteBuffer expected = ByteBuffer.allocate(dataSize + 10);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // 256 bytes binary message in a single unmasked frame
        expected.put(new byte[]
                { (byte)0x82, (byte)0x7F });
        expected.putInt(0x0000000000010000);

        for (int i = 0; i < dataSize; i++)
        {
            expected.put((byte)0x44);
        }

        actual.flip();
        expected.flip();

        // System.out.println(binary);

        // System.out.println(BufferUtil.toDetailString(expected));
        // System.out.println(BufferUtil.toDetailString(actual));

        // for (int i = 0; i < 20; ++i)
        // {
        // System.out.printf("a [%2d] 0x%02x%n",i,actual.get());
        // System.out.printf("e [%2d] 0x%02x%n",i,expected.get());
        // }

        ByteBufferAssert.assertEquals("binary buffers are not equal", expected, actual);
    
    }
}

