package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginMapper {
    User login(@Param("username") String username, @Param("email") String email);

    User selectByUsername(@Param("username") String username);

    User selectByEmail(@Param("email") String email);
}
