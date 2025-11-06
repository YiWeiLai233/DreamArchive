package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface registerMapper {
    User newUser(User user); ;
}
