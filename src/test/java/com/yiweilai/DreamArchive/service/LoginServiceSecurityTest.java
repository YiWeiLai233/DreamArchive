package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.LoginResponse;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.mapper.RegisterMapper;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.SensitiveDataEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginServiceSecurityTest {

    @Test
    void missingUserAndWrongPasswordUseSameMessage() {
        LoginMapper loginMapper = mock(LoginMapper.class);
        RegisterMapper registerMapper = mock(RegisterMapper.class);
        SensitiveDataEncryptor encryptor = mock(SensitiveDataEncryptor.class);
        TokenService tokenService = mock(TokenService.class);
        MinioService minioService = mock(MinioService.class);
        LoginService service = new LoginService();
        ReflectionTestUtils.setField(service, "loginMapper", loginMapper);
        ReflectionTestUtils.setField(service, "registerMapper", registerMapper);
        ReflectionTestUtils.setField(service, "sensitiveDataEncryptor", encryptor);
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "minioService", minioService);

        when(loginMapper.login("alice", "alice")).thenReturn(null);
        Result<LoginResponse> missing = service.login("alice", "bad");

        User user = new User();
        user.setPassword("hash");
        user.setStatus("ACTIVE");
        when(loginMapper.login("bob", "bob")).thenReturn(user);
        when(encryptor.matches("bad", "hash")).thenReturn(false);
        Result<LoginResponse> wrongPassword = service.login("bob", "bad");

        assertThat(missing.getCode()).isEqualTo(500);
        assertThat(wrongPassword.getCode()).isEqualTo(500);
        assertThat(missing.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThat(missing.getMessage()).isEqualTo("\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
    }
}
