package com.yiweilai.DreamArchive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mysql.cj.protocol.Message;
import com.yiweilai.DreamArchive.DTO.AI;
import com.yiweilai.DreamArchive.DTO.messages;
import com.yiweilai.DreamArchive.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class jackson{
    public static void main(String[] args) throws JsonProcessingException {
        List<messages> ai = new ArrayList<messages>();
        messages m = new messages("user","test");
        ai.add(m);
        System.out.println(JsonUtil.toJSON(ai, "gpt5"));
    }
}