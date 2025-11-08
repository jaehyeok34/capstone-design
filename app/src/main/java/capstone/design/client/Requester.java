package capstone.design.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;

public class Requester {

    private final Producer producer;
    private final Consumer consumer;
    
    public Requester(Producer producer, Consumer consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    public Message request(Message produceMessage, Message consumeMessage, int timeout, TimeUnit unit) throws Exception {
        BlockingQueue<Message> notifiedQueue = new LinkedBlockingQueue<>();
        ExecutorService notifier = consumer.subscribe(consumeMessage, notifiedQueue, timeout, unit);
        if (notifier == null) {
            return null;
        }

        try {
            // 메시지 처리 요청
            Message produced = producer.syncProduce(produceMessage, timeout, unit);
            Byte ok = produced.optionAsByte(MessageOption.OK);
            if (ok == null || !ok.equals((byte) 1)) {
                throw new Exception("produce 실패");
            }

            // 메시지 처리자의 처리 완료 대기
            Message notified = notifiedQueue.poll(timeout, unit);
            if (notified == null) {
                throw new TimeoutException("토픽 업데이트 대기 시간 초과");
            }

            // 메시지 처리 결과 요청
            notified.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte());
            Message consumed = consumer.consume(notified, timeout, unit);
            if (consumed == null) {
                throw new TimeoutException("처리 결과 수신 대기 시간 초과");
            }

            return consumed;
        } catch (Exception e) {
            System.err.println("Requester.request(): " + e);
            return null;
        } finally {
            notifier.shutdownNow();
        }
    }
 }
