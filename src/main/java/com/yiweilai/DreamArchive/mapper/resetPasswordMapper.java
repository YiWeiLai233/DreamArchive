package com.yiweilai.DreamArchive.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResetPasswordMapper {
    int resetById(@Param("id") Integer id, @Param("newPassword") String newPassword);
    int resetByUsername(@Param("username") String username, @Param("newPassword") String newPassword);
    int resetByEmail(@Param("email") String email, @Param("newPassword") String newPassword);
}
