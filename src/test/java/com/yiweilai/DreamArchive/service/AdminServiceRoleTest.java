package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.AdminMapper;
import com.yiweilai.DreamArchive.mapper.DreamContentMapper;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.SensitiveDataEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * AdminService 角色权限测试
 */
class AdminServiceRoleTest {

    private AdminService adminService;
    private LoginMapper loginMapper;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService();
        loginMapper = mock(LoginMapper.class);
        tokenService = mock(TokenService.class);

        ReflectionTestUtils.setField(adminService, "loginMapper", loginMapper);
        ReflectionTestUtils.setField(adminService, "tokenService", tokenService);
        ReflectionTestUtils.setField(adminService, "adminMapper", mock(AdminMapper.class));
        ReflectionTestUtils.setField(adminService, "dreamContentMapper", mock(DreamContentMapper.class));
        ReflectionTestUtils.setField(adminService, "sensitiveDataEncryptor", mock(SensitiveDataEncryptor.class));

        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser(Integer userId, String role) {
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(role);
        user.setDeleted(false);
        user.setStatus("ACTIVE");

        when(loginMapper.selectById(userId)).thenReturn(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void requireSuperAdminReturnsSuccessForSuperAdmin() {
        mockCurrentUser(1, "SUPER_ADMIN");

        Result<User> result = adminService.requireSuperAdmin("Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void requireSuperAdminReturnsErrorForAdmin() {
        mockCurrentUser(2, "ADMIN");

        Result<User> result = adminService.requireSuperAdmin("Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("需要超级管理员权限");
    }

    @Test
    void requireSuperAdminReturnsErrorForUser() {
        mockCurrentUser(3, "USER");

        Result<User> result = adminService.requireSuperAdmin("Bearer valid-token");

        // USER角色先被requireAdmin拦截，返回"需要管理员权限"
        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("需要管理员权限");
    }

    @Test
    void requireSuperAdminReturnsErrorForNullToken() {
        when(tokenService.parseBearerToken(null)).thenThrow(new IllegalArgumentException("请先登录"));

        Result<User> result = adminService.requireSuperAdmin(null);

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("请先登录");
    }

    @Test
    void requireSuperAdminReturnsErrorForEmptyToken() {
        when(tokenService.parseBearerToken("")).thenThrow(new IllegalArgumentException("请先登录"));

        Result<User> result = adminService.requireSuperAdmin("");

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("请先登录");
    }

    @Test
    void requireSuperAdminReturnsErrorForDeletedUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("deleted");
        user.setRole("SUPER_ADMIN");
        user.setDeleted(true);
        user.setStatus("ACTIVE");

        when(loginMapper.selectById(1)).thenReturn(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        Result<User> result = adminService.requireSuperAdmin("Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("登录用户不存在");
    }

    @Test
    void requireSuperAdminReturnsErrorForBannedUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("banned");
        user.setRole("SUPER_ADMIN");
        user.setDeleted(false);
        user.setStatus("BANNED");

        when(loginMapper.selectById(1)).thenReturn(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        Result<User> result = adminService.requireSuperAdmin("Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("账号已被封禁");
    }

    @Test
    void requireAdminReturnsSuccessForAdmin() {
        mockCurrentUser(2, "ADMIN");

        Result<User> result = ReflectionTestUtils.invokeMethod(adminService, "requireAdmin", "Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void requireAdminReturnsSuccessForSuperAdmin() {
        mockCurrentUser(1, "SUPER_ADMIN");

        Result<User> result = ReflectionTestUtils.invokeMethod(adminService, "requireAdmin", "Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void requireAdminReturnsErrorForUser() {
        mockCurrentUser(3, "USER");

        Result<User> result = ReflectionTestUtils.invokeMethod(adminService, "requireAdmin", "Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("需要管理员权限");
    }
}
