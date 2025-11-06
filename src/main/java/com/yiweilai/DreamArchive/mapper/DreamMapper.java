package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.Dream;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DreamMapper {
    void insertDream(Dream dream);
    Dream selectDreamByID(String Id);
}
