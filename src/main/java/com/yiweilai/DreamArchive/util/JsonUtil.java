package com.yiweilai.DreamArchive.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.DreamArchive.DTO.AI;
import com.yiweilai.DreamArchive.DTO.Message;

import java.util.List;

public class JsonUtil {
    public static ObjectMapper mapper = new ObjectMapper();
        public static String toJSON(List<Message> messages,String model) {
        //这里将对象转换成json
        AI ai=new AI("0.5",model,messages);
        try{
            return mapper.writeValueAsString(ai);
        }catch(JsonProcessingException e){
            throw new  RuntimeException("JSON初始化错误",e);
        }
    }
}
