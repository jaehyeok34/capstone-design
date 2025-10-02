package org.example;

import org.junit.jupiter.api.Test;

public class MessageCodecTest {

    @Test
    public void producerToBroker() {
        // 형태: [type][request id][topic name length][topic name][payload]
        // Message m = Message.of("t1", "hello".getBytes(StandardCharsets.UTF_8));
        // MessageHeader mh = MessageHeader.of(Type.REQ_PUSH, 0);
        // ByteBuf buf = MessageFrame.wrap(mh, m);

        // MessageFrame mf = MessageCodec.decode(buf);
        // MessageHeader dmh = mf.header();
        // Message dm = mf.message();

        // assertEquals(Type.REQ_PUSH, dmh.getType());
        // assertEquals(0, dmh.getRequestIdValue());
        // assertEquals("t1", dm.getTopicName());
        // assertEquals("hello", dm.getPayload().toString(StandardCharsets.UTF_8));

        // MessageFrame.release(mh, m);
        // assertEquals(0, m.getPayload().refCnt());
        // assertEquals(0, mh.getRequestId().refCnt());

        // mf.release();
        // assertEquals(0, dm.getPayload().refCnt());
        // assertEquals(0, dmh.getRequestId().refCnt());
    }

    @Test
    public void consumerToBroker() {
        // 형태: [type][request id][topic name length][topic name]
        // Message m = Message.of("t1");
        // MessageHeader mh = MessageHeader.of(Type.REQ_PULL, 0);
        // ByteBuf buf = MessageFrame.wrap(mh, m);

        // MessageFrame mf = MessageCodec.decode(buf);
        // MessageHeader dmh = mf.header();
        // Message dm = mf.message();

        // assertEquals("t1", dm.getTopicName());
        // assertNull(dm.getPayload());
        // assertEquals(Type.REQ_PULL, dmh.getType());
        // assertEquals(0, dmh.getRequestIdValue());

        // MessageFrame.release(mh, m);
        // mf.release();
    }

    @Test
    public void brokerToConsumer() {
        // 형태: [type][request id][payload]
        // Message m = Message.of("hello".getBytes(StandardCharsets.UTF_8));
        // MessageHeader mh = MessageHeader.of(Type.RES_PULL, 0);
        // ByteBuf buf = MessageFrame.wrap(mh, m);

        // MessageFrame mf = MessageCodec.decode(buf);
        // MessageHeader dmh = mf.header();
        // Message dm = mf.message();

        // assertNull(dm.getTopicName());
        // assertEquals("hello", dm.getPayload().toString(StandardCharsets.UTF_8));
        // assertEquals(Type.RES_PULL, dmh.getType());
        // assertEquals(0, dmh.getRequestIdValue());

        // MessageFrame.release(mh, m);
        // mf.release();
    }
}
