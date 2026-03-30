package com.xjtutjc.tool;

import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class WeatherTool {

    private ToolFunction toolFunction;

    @PostConstruct
    public void buildFunction(){
        this.toolFunction = ToolFunction.builder().function(FunctionDefinition.builder()
                .name(getToolName()) // 工具的唯一标识名，必须与本地实现对应。
                .description("当你想查询指定城市的天气时非常有用。") // 清晰的描述能帮助模型更好地决定何时使用该工具。
                .parameters(JsonUtils.parseString(getToolSchema()).getAsJsonObject())
                .build()).build();
    }

    public ToolFunction getToolFunction(){
        return this.toolFunction;
    }

    public String getWeather(String args){
        JSONObject arguments = JSONObject.parseObject(args);
        String location = arguments.getString("location");
        String date = arguments.getString("date");
        return location + date +"是多云";
    }

    public String getToolSchema(){
        return "{\"type\":\"object\",\"properties\":{\"location\":{\"type\":\"string\",\"description\":\"城市或县区，比如北京市、杭州市、余杭区等。\"},\"date\":{\"type\":\"string\",\"description\":\"用户描述的时间\"}},\"required\":[\"location\"]}";
    }

    public String getToolName(){
        return "get_current_weather";
    }


}
