package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.AdminDreamSummary;
import com.yiweilai.DreamArchive.DTO.AdminUserSummary;
import com.yiweilai.DreamArchive.DTO.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {
    int countUsers();

    int countAdminUsers();

    int countDreams();

    int countTodayDreams();

    int countUserSummaries(@Param("keyword") String keyword, @Param("excludeSuperAdmin") boolean excludeSuperAdmin);

    List<AdminUserSummary> selectUserSummaries(
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit,
            @Param("excludeSuperAdmin") boolean excludeSuperAdmin);

    int countRecentDreams(@Param("keyword") String keyword);

    List<AdminDreamSummary> selectRecentDreams(
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    User selectUserById(@Param("id") Integer id);

    int countByUsernameExceptId(@Param("username") String username, @Param("id") Integer id);

    int countByEmailExceptId(@Param("email") String email, @Param("id") Integer id);

    int insertUser(User user);

    int updateUser(User user);

    int softDeleteUser(@Param("id") Integer id);

    int updateUserStatus(@Param("id") Integer id, @Param("status") String status);
}
