package com.xjtutjc.context;

import com.alibaba.dashscope.common.Message;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
public class ChatContext {
    private List<Message> messages = new ArrayList<>();
}
