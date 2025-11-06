package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.Dream;
import com.yiweilai.DreamArchive.mapper.DreamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DreamService {
    @Autowired
    DreamMapper dreamMapper;
    public Dream setDream(String time ,String Place,String emotion,String Content,String Interpretation){
        Dream dream = new Dream();
        dream.setId(UUID.randomUUID().toString());
        dream.setTime(time);
        dream.setPlace(Place);
        dream.setEmotion(emotion);
        dream.setContent(Content);
        dream.setInterpretation(Interpretation);
        dreamMapper.insertDream(dream);
        return dream;
    }
    public  String getInterpretation(){
        return "1";
    }
    //这里的id是前端传进来的id,要与后台数据库做id对比
    public Dream getDreamById(String id){

        return dreamMapper.selectDreamByID(id);
    }
}
