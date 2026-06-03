package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.LoginResponse;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.AuthCookieProperties;
import com.yiweilai.DreamArchive.service.AuthCookieService;
import com.yiweilai.DreamArchive.service.CsrfTokenService;
import com.yiweilai.DreamArchive.service.LoginService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginControllerCookieTest {

    @Test
    void successfulPasswordLoginWritesAuthAndCsrfCookiesWithoutReturningTokenInBody() {
        LoginService loginService = mock(LoginService.class);
        VerificationCodeService verificationCodeService = mock(VerificationCodeService.class);
        CsrfTokenService csrfTokenService = mock(CsrfTokenService.class);
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        properties.setCsrfCookieName("XSRF-TOKEN");
        properties.setTtlSeconds(1800L);
        AuthCookieService authCookieService = new AuthCookieService(properties);
        LoginController controller = new LoginController();
        ReflectionTestUtils.setField(controller, "loginService", loginService);
        ReflectionTestUtils.setField(controller, "verificationCodeService", verificationCodeService);
        ReflectionTestUtils.setField(controller, "authCookieService", authCookieService);
        ReflectionTestUtils.setField(controller, "csrfTokenService", csrfTokenService);

        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        when(loginService.login("alice", "pw")).thenReturn(Result.success("登录成功", new LoginResponse(user, "signed-token")));
        when(csrfTokenService.generateToken()).thenReturn("csrf-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<LoginResponse> result = controller.login(Map.of("username", "alice", "password", "pw"), response);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getToken()).isNull();
        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).anySatisfy(cookie -> assertThat(cookie)
                .contains("dream_auth=signed-token")
                .contains("HttpOnly")
                .contains("Max-Age=1800"));
        assertThat(cookies).anySatisfy(cookie -> assertThat(cookie)
                .contains("XSRF-TOKEN=csrf-token")
                .doesNotContain("HttpOnly"));
    }
}
