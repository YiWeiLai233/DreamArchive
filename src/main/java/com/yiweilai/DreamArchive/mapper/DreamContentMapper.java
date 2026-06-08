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

    List<DreamContent> selectPageByUserId(@Param("userId") Integer userId,
                                          @Param("keyword") String keyword,
                                          @Param("emotion") String emotion,
                                          @Param("draftsOnly") boolean draftsOnly,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    long countByUserIdWithFilters(@Param("userId") Integer userId,
                                  @Param("keyword") String keyword,
                                  @Param("emotion") String emotion,
                                  @Param("draftsOnly") boolean draftsOnly);

    void updateTitle(@Param("id") String id, @Param("title") String title);

    int updateEditableDream(@Param("id") String id,
                            @Param("title") String title,
                            @Param("content") String content,
                            @Param("emotion") String emotion,
                            @Param("place") String place,
                            @Param("time") String time);

    void updateInterpretation(@Param("id") String id, @Param("interpretation") String interpretation);

    void updateAnalysisStatus(@Param("id") String id, @Param("status") String status, @Param("error") String error);

    int deleteById(@Param("id") String id);
}
