package capstone.design.message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import capstone.design.Utils;

public class Message {

    private final Map<String, Object> options = new HashMap<>();

    private Message() {}

    public static Message of() { return new Message(); }
    public static Message of(String key, Object value) { return new Message().addOption(key, value); }
    public static Message of(Map<String, Object> options) { return new Message().addOptions(options); }
    
    public Message addOption(String key, Object value) {
        Utils.validate(key, value);

        options.put(key, value);
        return this;
    }

    public Message addOptions(Map<String, Object> options) {
        Utils.validate(options);
        
        options.forEach(this::addOption);
        return this;
    }

    public Message removeOptions(String... keys) {
        for (String key : keys) {
            options.remove(key);
        }

        return this;
    }

    public Object option(String key) {
        return options.get(key);
    }

    public Integer optionAsInt(String key) {
        switch (options.get(key)) {
            case Integer i -> { return i; }
            case byte[] b -> { return Integer.parseInt(new String(b, StandardCharsets.UTF_8)); }
            case null, default -> { return null; }
        }
    }

    public Long optionAsLong(String key) {
        switch (options.get(key)) {
            case Long l -> { return l; }
            case byte[] b -> { return Long.parseLong(new String(b, StandardCharsets.UTF_8)); }
            case null, default -> { return null; }
        }
    }

    public String optionAsString(String key) {
        switch (options.get(key)) {
            case String s -> { return s; }
            case byte[] b -> { return new String(b, StandardCharsets.UTF_8); }
            case null, default -> { return null; }
        }
    }

    public Byte optionAsByte(String key) {
        switch (options.get(key)) {
            case Byte b -> { return b; }
            case byte[] b -> { return Byte.parseByte(new String(b, StandardCharsets.UTF_8)); }
            case null, default -> { return null; }
        }
    }

    public byte[] optionAsBytes(String key) {
        switch (options.get(key)) {
            case byte[] b -> { return b; }
            case null, default -> { return null; }
        }
    }

    public Map<String, Object> options() {
        return options;
    }

    public Message clear() {
        options.clear();
        
        return this;
    }

    public Message copy() {
        return Message.of(options);
    }

    @Override
    public String toString() {
        String str = "";
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            str += entry.getKey() + ": ";
            switch (entry.getValue()) {
                case byte[] b -> str += new String(b, StandardCharsets.UTF_8) + ", ";
                default -> str += entry.getValue() + ", ";
            }
        }

        return "Message{" + str + "}";
    }
}
