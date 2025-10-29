package capstone.design;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import io.netty.buffer.ByteBuf;

public class Utils {

    public static final int MAGIC = 0x6B3FA0FF;
    public static final String DEFAULT_OPTION_MAPPING_TABLE_FILE_PATH = "../option_mapping_table.properties";
    public static final String UNKNOWN_OPTION_TYPE = String.valueOf(-1);
    
    /**
     * 기본적으로 null 체크
     * 일부 타입은 추가 검증 수행
     * IllegalStateException 발생
     */
    public static void validate(Object... objects) {
        for (Object object : objects) {
            if (object == null) {
                throw new IllegalStateException("null");
            }

            if (object instanceof String string && string.isEmpty()) {
                throw new IllegalStateException("empty string");
            } else if (object instanceof ByteBuf buf && (buf.refCnt() <= 0 || buf.readableBytes() == 0)) {
                throw new IllegalStateException("released or empty buf");
            } else if (object instanceof byte[] arr && arr.length == 0) {
                throw new IllegalStateException("empty byte array");
            } else if (object instanceof Map<?, ?> map && map.isEmpty()) {
                throw new IllegalStateException("empty map");
            } else if (object instanceof List<?> iterable && !iterable.iterator().hasNext()) {
                throw new IllegalStateException("empty iterable");
            }
         }
    }

    public static void castAdd(Message message, String key, byte[] bytes) {
        castAdd(message, key, new String(bytes, StandardCharsets.UTF_8));
    }

    public static void castAdd(Message message, String key, String bytes) {
        switch (key) {
            case MessageOption.MESSAGE_TYPE, 
                MessageOption.SUCCESS -> {
                byte value = Byte.parseByte(bytes);
                message.addOption(key, value);
            }

            case MessageOption.CLIENT_ID, 
                MessageOption.TOPIC_NAME -> {
                message.addOption(key, bytes);
            }

            case MessageOption.PARTITION, 
                MessageOption.REQUEST_ID -> {
                try {
                    int value = Integer.parseInt(bytes);
                    message.addOption(key, value);
                } catch (NumberFormatException ignored) {}
            }

            case MessageOption.CURSOR, 
                MessageOption.OFFSET, 
                MessageOption.REMAINING_COUNT -> {
                try {
                    long value = Long.parseLong(bytes);
                    message.addOption(key, value);
                } catch (NumberFormatException ignored) {}
            } 

            default -> {
                message.addOption(key, bytes.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
