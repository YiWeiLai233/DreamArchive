package com.yiweilai.DreamArchive.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface resetPasswordMapper {
    String resetByUsername(String newPassword);
    String resetByEmail(String newPassword);
}
