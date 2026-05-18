package com.yiweilai.DreamArchive.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResetPasswordMapper {
    int resetByUsername(@Param("username") String username, @Param("newPassword") String newPassword);
    int resetByEmail(@Param("email") String email, @Param("newPassword") String newPassword);
}
