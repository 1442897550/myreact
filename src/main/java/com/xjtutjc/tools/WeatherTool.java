package com.xjtutjc.tools;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class WeatherTool extends AbstractLocalTool{

    public String getToolResult(String args){
        JSONObject arguments = JSONObject.parseObject(args);
        String location = arguments.getString("location");
        String date = arguments.getString("date");
        return location + date +"是多云";
    }

    public String getToolSchema(){
        return "{\"type\":\"object\",\"properties\":{\"location\":{\"type\":\"string\",\"description\":\"城市或县区，比如北京市、杭州市、余杭区等。\"},\"date\":{\"type\":\"string\",\"description\":\"用户描述的时间\"}},\"required\":[\"location\"]}";
    }
    @Override
    public String getToolDescription(){
        return "当你想查询指定城市的天气时非常有用。";
    }

    public String getToolName(){
        return "get_current_weather";
    }


}
