package org.example.message;

import org.example.broker.topic.Topic;

import io.netty.buffer.ByteBuf;

/*
 * Netty Server가 수신한 메시지를 저장 혹은 제공하는 비즈니스 로직을 담당하는 인터페이스
 */
public interface MessageRepository {
    void push(String topicName, ByteBuf payload);
    Topic.Record pull(String topicName);
}
