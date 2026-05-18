package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.Dream;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DreamMapper {
    void insertDream(Dream dream);
    Dream selectDreamByID(String Id);
    List<Dream> selectByUserId(@Param("userId") Integer userId);
}
