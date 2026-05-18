package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DreamContentMapper {

    void insertDreamContent(DreamContent dreamContent);

    DreamContent selectById(@Param("id") String id);

    List<DreamContent> selectByUserId(@Param("userId") Integer userId);

    void updateInterpretation(@Param("id") String id, @Param("interpretation") String interpretation);
}
