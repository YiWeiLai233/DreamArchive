package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.service.ClientIpResolver;
import com.yiweilai.DreamArchive.service.ResetPasswordService;
import com.yiweilai.DreamArchive.service.SecurityRateLimitService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResetPasswordControllerSecurityTest {

    @Test
    void sendCodeUsesSamePublicResponseWhenUserIsMissing() {
        ResetPasswordService resetPasswordService = mock(ResetPasswordService.class);
        VerificationCodeService verificationCodeService = mock(VerificationCodeService.class);
        LoginMapper loginMapper = mock(LoginMapper.class);
        SecurityRateLimitService rateLimitService = mock(SecurityRateLimitService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        ResetPasswordController controller = new ResetPasswordController();
        ReflectionTestUtils.setField(controller, "resetPasswordService", resetPasswordService);
        ReflectionTestUtils.setField(controller, "verificationCodeService", verificationCodeService);
        ReflectionTestUtils.setField(controller, "loginMapper", loginMapper);
        ReflectionTestUtils.setField(controller, "rateLimitService", rateLimitService);
        ReflectionTestUtils.setField(controller, "clientIpResolver", clientIpResolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
        when(loginMapper.selectByUsername("missing")).thenReturn(null);
        Result<String> missing = controller.sendCode(Map.of("identifier", "missing"), request);

        User user = new User();
        user.setEmail("alice@example.com");
        when(loginMapper.selectByUsername("alice")).thenReturn(user);
        Result<String> existing = controller.sendCode(Map.of("identifier", "alice"), request);

        assertThat(missing.getCode()).isEqualTo(200);
        assertThat(existing.getCode()).isEqualTo(200);
        assertThat(missing.getMessage()).isEqualTo(existing.getMessage());
        verify(verificationCodeService).sendCode("reset-password", "alice", "alice@example.com");
    }
}
