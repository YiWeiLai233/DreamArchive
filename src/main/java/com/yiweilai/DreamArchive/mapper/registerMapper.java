package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegisterMapper {
    int newUser(User user);

    @Select("select * from user where username = #{username}")
    User findByUsername(String username);

    @Select("select * from user where email = #{email}")
    User findByEmail(String email);
}
