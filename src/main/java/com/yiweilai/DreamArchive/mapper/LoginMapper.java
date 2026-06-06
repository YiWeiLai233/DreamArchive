package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginMapper {
    User login(@Param("username") String username, @Param("email") String email);

    User selectById(@Param("id") Integer id);

    User selectByUsername(@Param("username") String username);

    User selectByEmail(@Param("email") String email);

    void updateAvatarUrl(@Param("id") Integer id, @Param("avatarUrl") String avatarUrl);
}
