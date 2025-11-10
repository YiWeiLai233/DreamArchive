package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

@Mapper
public interface registerMapper {
    int newUser(User user);
    @Select("select * from user where username= #{username}")
    User findByUsername(String username);
    @Select("select * from user where email= #{email}")
    User findByEmail(String email);
}
