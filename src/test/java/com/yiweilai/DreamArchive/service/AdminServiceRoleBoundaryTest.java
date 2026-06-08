package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.AdminOverview;
import com.yiweilai.DreamArchive.DTO.AdminUserActionRequest;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.AdminMapper;
import com.yiweilai.DreamArchive.mapper.DreamContentMapper;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.SensitiveDataEncryptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceRoleBoundaryTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ordinaryAdminCannotCreateAnotherAdmin() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        SensitiveDataEncryptor sensitiveDataEncryptor = mock(SensitiveDataEncryptor.class);
        AdminService service = service(adminMapper, sensitiveDataEncryptor);
        authenticate(user(1, "manager", "ADMIN"));
        when(adminMapper.countByUsernameExceptId("newadmin", null)).thenReturn(0);
        when(adminMapper.countByEmailExceptId("newadmin@example.com", null)).thenReturn(0);
        when(sensitiveDataEncryptor.encrypt("secret1")).thenReturn("hash");
        when(adminMapper.insertUser(any(User.class))).thenReturn(1);

        Result<AdminOverview> result = service.handleUserAction(null, createRequest("newadmin", "newadmin@example.com", "ADMIN"));

        assertThat(result.getCode()).isEqualTo(403);
        verify(adminMapper, never()).insertUser(any(User.class));
    }

    @Test
    void ordinaryAdminCannotCreateSuperAdmin() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        SensitiveDataEncryptor sensitiveDataEncryptor = mock(SensitiveDataEncryptor.class);
        AdminService service = service(adminMapper, sensitiveDataEncryptor);
        authenticate(user(1, "manager", "ADMIN"));
        when(adminMapper.countByUsernameExceptId("root2", null)).thenReturn(0);
        when(adminMapper.countByEmailExceptId("root2@example.com", null)).thenReturn(0);
        when(sensitiveDataEncryptor.encrypt("secret1")).thenReturn("hash");
        when(adminMapper.insertUser(any(User.class))).thenReturn(1);

        Result<AdminOverview> result = service.handleUserAction(null, createRequest("root2", "root2@example.com", "SUPER_ADMIN"));

        assertThat(result.getCode()).isEqualTo(403);
        verify(adminMapper, never()).insertUser(any(User.class));
    }

    @Test
    void ordinaryAdminCannotPromoteUserToAdmin() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        SensitiveDataEncryptor sensitiveDataEncryptor = mock(SensitiveDataEncryptor.class);
        AdminService service = service(adminMapper, sensitiveDataEncryptor);
        authenticate(user(1, "manager", "ADMIN"));
        User target = user(2, "alice", "USER");
        target.setEmail("alice@example.com");
        when(adminMapper.selectUserById(2)).thenReturn(target);
        when(adminMapper.countByUsernameExceptId("alice", 2)).thenReturn(0);
        when(adminMapper.countByEmailExceptId("alice@example.com", 2)).thenReturn(0);
        when(adminMapper.updateUser(any(User.class))).thenReturn(1);

        AdminUserActionRequest request = updateRequest(2, "alice", "alice@example.com", "ADMIN");
        Result<AdminOverview> result = service.handleUserAction(null, request);

        assertThat(result.getCode()).isEqualTo(403);
        verify(adminMapper, never()).updateUser(any(User.class));
    }

    @Test
    void ordinaryAdminCanUpdateOwnProfileWithoutChangingRole() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        SensitiveDataEncryptor sensitiveDataEncryptor = mock(SensitiveDataEncryptor.class);
        AdminService service = service(adminMapper, sensitiveDataEncryptor);
        User currentAdmin = user(1, "manager", "ADMIN");
        authenticate(currentAdmin);
        when(adminMapper.selectUserById(1)).thenReturn(currentAdmin);
        when(adminMapper.countByUsernameExceptId("manager2", 1)).thenReturn(0);
        when(adminMapper.countByEmailExceptId("manager2@example.com", 1)).thenReturn(0);
        when(adminMapper.updateUser(any(User.class))).thenReturn(1);

        AdminUserActionRequest request = updateRequest(1, "manager2", "manager2@example.com", "ADMIN");
        Result<AdminOverview> result = service.handleUserAction(null, request);

        assertThat(result.getCode()).isEqualTo(200);
        verify(adminMapper).updateUser(any(User.class));
    }

    @Test
    void superAdminCanCreateAdmin() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        SensitiveDataEncryptor sensitiveDataEncryptor = mock(SensitiveDataEncryptor.class);
        AdminService service = service(adminMapper, sensitiveDataEncryptor);
        authenticate(user(1, "root", "SUPER_ADMIN"));
        when(adminMapper.countByUsernameExceptId("newadmin", null)).thenReturn(0);
        when(adminMapper.countByEmailExceptId("newadmin@example.com", null)).thenReturn(0);
        when(sensitiveDataEncryptor.encrypt("secret1")).thenReturn("hash");
        when(adminMapper.insertUser(any(User.class))).thenReturn(1);

        Result<AdminOverview> result = service.handleUserAction(null, createRequest("newadmin", "newadmin@example.com", "ADMIN"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(adminMapper).insertUser(any(User.class));
    }

    private AdminService service(AdminMapper adminMapper, SensitiveDataEncryptor sensitiveDataEncryptor) {
        AdminService service = new AdminService();
        ReflectionTestUtils.setField(service, "tokenService", mock(TokenService.class));
        ReflectionTestUtils.setField(service, "loginMapper", mock(LoginMapper.class));
        ReflectionTestUtils.setField(service, "adminMapper", adminMapper);
        ReflectionTestUtils.setField(service, "dreamContentMapper", mock(DreamContentMapper.class));
        ReflectionTestUtils.setField(service, "sensitiveDataEncryptor", sensitiveDataEncryptor);
        return service;
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        ));
    }

    private User user(int id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setDeleted(false);
        return user;
    }

    private AdminUserActionRequest createRequest(String username, String email, String role) {
        AdminUserActionRequest request = new AdminUserActionRequest();
        request.setAction("CREATE");
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("secret1");
        request.setRole(role);
        request.setStatus("ACTIVE");
        return request;
    }

    private AdminUserActionRequest updateRequest(Integer id, String username, String email, String role) {
        AdminUserActionRequest request = new AdminUserActionRequest();
        request.setAction("UPDATE");
        request.setId(id);
        request.setUsername(username);
        request.setEmail(email);
        request.setRole(role);
        request.setStatus("ACTIVE");
        return request;
    }
}
