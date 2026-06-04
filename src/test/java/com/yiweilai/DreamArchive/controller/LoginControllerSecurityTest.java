package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.LoginResponse;
import com.yiweilai.DreamArchive.service.AuthCookieService;
import com.yiweilai.DreamArchive.service.ClientIpResolver;
import com.yiweilai.DreamArchive.service.CsrfTokenService;
import com.yiweilai.DreamArchive.service.LoginService;
import com.yiweilai.DreamArchive.service.SecurityRateLimitService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerSecurityTest {

    @Test
    void passwordLoginIsRejectedBeforePasswordCheckWhenRateLimited() {
        LoginService loginService = mock(LoginService.class);
        VerificationCodeService verificationCodeService = mock(VerificationCodeService.class);
        AuthCookieService authCookieService = mock(AuthCookieService.class);
        CsrfTokenService csrfTokenService = mock(CsrfTokenService.class);
        SecurityRateLimitService rateLimitService = mock(SecurityRateLimitService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        LoginController controller = new LoginController();
        ReflectionTestUtils.setField(controller, "loginService", loginService);
        ReflectionTestUtils.setField(controller, "verificationCodeService", verificationCodeService);
        ReflectionTestUtils.setField(controller, "authCookieService", authCookieService);
        ReflectionTestUtils.setField(controller, "csrfTokenService", csrfTokenService);
        ReflectionTestUtils.setField(controller, "rateLimitService", rateLimitService);
        ReflectionTestUtils.setField(controller, "clientIpResolver", clientIpResolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
        when(rateLimitService.isPasswordLoginBlocked("203.0.113.10", "alice")).thenReturn(true);

        Result<LoginResponse> result = controller.login(Map.of("username", "alice", "password", "pw"), response, request);

        assertThat(result.getCode()).isEqualTo(429);
        verify(loginService, never()).login("alice", "pw");
    }
}
