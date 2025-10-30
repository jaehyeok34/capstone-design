package capstone.design.api_gateway.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import capstone.design.client.Consumer;
import capstone.design.client.Producer;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import io.netty.buffer.ByteBuf;


@Service
public class ApiGatewayService {

    private final Producer producer;
    private final Consumer consumer;

    public ApiGatewayService(Producer producer, Consumer consumer) {
        this.producer = producer;
        this.consumer = consumer;   
    }
    
    public String home() {
        return "api gateway";
    }

    public byte[] convertFile(MultipartFile file) throws IOException {
        return convertFiles(new MultipartFile[]{file}).get(0);
    }

    public List<byte[]> convertFiles(MultipartFile[] files) throws IOException {
        String topicName = "convert_file";
        List<byte[]> convertedFiles = new ArrayList<>();
        
        for (var file : files) {
            String datas = new String(file.getBytes(), StandardCharsets.UTF_8);
            var produced = producer.syncProduce(topicName, 0, datas.getBytes(StandardCharsets.UTF_8));
            var cursor = produced.option(MessageOption.CURSOR, Long.class);
            cursor = (cursor != null) ? cursor : -1L;

            var consumed = consumer.consume(topicName, 1, cursor);
            if (consumed != null && consumed.option(MessageOption.PAYLOAD) instanceof ByteBuf payload) {
                byte[] buf = new byte[payload.readableBytes()];
                payload.readBytes(buf);

                convertedFiles.add(buf);
            }
        }

        return convertedFiles;
    }

    public Map<?, ?> findJoinKeys(MultipartFile[] files) throws IOException {
        System.out.println("[debug] findJoinKeys called");
        String topicName = "find_join_keys";
        
        BlockingQueue<Message> out = new LinkedBlockingQueue<>();
        var executor = consumer.subscribeAndConsume(true, topicName, 1, out);

        for (var i = 0; i < 2; i++) {
            var file = files[i];
            String datas = new String(file.getBytes(), StandardCharsets.UTF_8);
            producer.syncProduce(topicName, 0, datas.getBytes(StandardCharsets.UTF_8));
        }

        Map<?, ?> resultMap = null;
        while (resultMap == null) {
            try {
                var msg = out.take();
                if (msg.option(MessageOption.PAYLOAD) instanceof ByteBuf payload) {
                    byte[] buf = new byte[payload.readableBytes()];
                    payload.readBytes(buf);

                    String result = new String(buf, StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    resultMap = mapper.readValue(result, Map.class);
                    resultMap = resultMap.isEmpty() ? null : resultMap;
                }
            } catch (Exception e) {
                System.err.println("findJoinKeys error: " + e.getMessage());
            }
        }

        executor.shutdown();

        return resultMap;
    }
}
