package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.AiProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiProviderMapper {

    List<AiProvider> selectAll();

    AiProvider selectByName(@Param("name") String name);

    int insert(AiProvider provider);

    int update(AiProvider provider);

    int deleteByName(@Param("name") String name);

    int clearVisionEnabledExcept(@Param("name") String name);
}
