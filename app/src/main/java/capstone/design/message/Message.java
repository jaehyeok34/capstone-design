package capstone.design.message;

import java.util.HashMap;
import java.util.Map;
import capstone.design.Utils;

public class Message {

    private final Map<String, Object> options = new HashMap<>();
    
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

    public Message removeOption(String key) {
        Utils.validate(key);

        options.remove(key);
        return this;
    }

    public <T> T option(String key, Class<T> type) {
        Utils.validate(key, type);

        Object value = options.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        
        return null;
    }

    public Object option(String key) {
        return options.get(key);
    }

    public Map<String, Object> options() {
        return options;
    }

    public void clearOptions() {
        options.clear();
    }
}
