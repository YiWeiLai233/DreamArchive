package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RegisterMapper {
    int newUser(User user);

    @Select("select * from user where username = #{username}")
    User findByUsername(String username);

    @Select("select * from user where email = #{email}")
    User findByEmail(String email);

    @Update("UPDATE user SET username = #{username}, password = #{password} WHERE id = #{id}")
    int updateUsernameAndPassword(@Param("id") int id, @Param("username") String username, @Param("password") String password);
}
