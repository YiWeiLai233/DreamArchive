package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.AdminOverview;
import com.yiweilai.DreamArchive.DTO.AdminOverviewRequest;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServiceCookieAuthTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminOverviewUsesAuthenticatedPrincipalWhenAuthorizationHeaderIsMissing() {
        AdminService service = new AdminService();
        TokenService tokenService = mock(TokenService.class);
        LoginMapper loginMapper = mock(LoginMapper.class);
        AdminMapper adminMapper = mock(AdminMapper.class);
        DreamContentMapper dreamContentMapper = mock(DreamContentMapper.class);
        SensitiveDataEncryptor sensitiveDataEncryptor = mock(SensitiveDataEncryptor.class);
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "loginMapper", loginMapper);
        ReflectionTestUtils.setField(service, "adminMapper", adminMapper);
        ReflectionTestUtils.setField(service, "dreamContentMapper", dreamContentMapper);
        ReflectionTestUtils.setField(service, "sensitiveDataEncryptor", sensitiveDataEncryptor);

        User admin = new User();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setDeleted(false);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        when(adminMapper.countUserSummaries(isNull(), anyBoolean())).thenReturn(0);
        when(adminMapper.countRecentDreams(isNull())).thenReturn(0);
        when(adminMapper.countUsers()).thenReturn(1);
        when(adminMapper.countAdminUsers()).thenReturn(1);
        when(adminMapper.countDreams()).thenReturn(0);
        when(adminMapper.countTodayDreams()).thenReturn(0);
        when(adminMapper.selectUserSummaries(isNull(), anyInt(), anyInt(), anyBoolean())).thenReturn(List.of());
        when(adminMapper.selectRecentDreams(isNull(), anyInt(), anyInt())).thenReturn(List.of());

        Result<AdminOverview> result = service.getOverview(null, new AdminOverviewRequest());

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTotalUsers()).isEqualTo(1);
    }
}
