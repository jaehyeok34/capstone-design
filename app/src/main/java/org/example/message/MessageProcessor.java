package org.example.message;

import org.jspecify.annotations.Nullable;

public interface MessageProcessor {
    
    Result process(MessageFrame frame);

    /*
     * MessageFrame을 처리한 결과
     * 기존에는 MessageHeader, Record를 받았으나, Record -> Object로 변경
     * 이유: REQ_PULL 요청에 대한 결과는 Record 이지만, 
     * REQ_PUSH 요청에 대한 결과(현재는 없지만 향후 필요할 수 있으므로)는 Record가 아닐 수 있기 때문
     */
    public static record Result(MessageHeader header, @Nullable Object message) {
        
        public Result {
            if (header == null) throw new IllegalArgumentException("header: null");
        }

        public static Result of(MessageHeader header, Object message) {
            return new Result(header, message);
        }

        public static Result of(MessageHeader header) {
            return new Result(header, null);
        }
    }
}
