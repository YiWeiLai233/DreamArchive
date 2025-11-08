package com.yiweilai.DreamArchive.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface resetPasswordMapper {
    String resetByUsername(String username);
    String resetByEmail(String email);
}
