package com.xjtutjc.dto.response;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SseMessage {
    private String type;  // content: 内容块，error: 错误，done: 完成
    private String data;

    public SseMessage() {}

    public SseMessage(String type, String data) {
        this.type = type;
        this.data = data;
    }

    public static SseMessage content(String data) {
        SseMessage message = new SseMessage();
        message.setType("content");
        message.setData(data);
        return message;
    }

    public static SseMessage error(String errorMessage) {
        SseMessage message = new SseMessage();
        message.setType("error");
        message.setData(errorMessage);
        return message;
    }

    public static SseMessage done() {
        SseMessage message = new SseMessage();
        message.setType("done");
        message.setData(null);
        return message;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
