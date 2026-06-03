package com.yiweilai.DreamArchive.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    @Test
    void authCookieIsHttpOnlyAndUsesConfiguredSameSite() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        properties.setCookieSecure(true);
        properties.setCookieSameSite("Strict");
        properties.setTtlSeconds(1800L);

        AuthCookieService service = new AuthCookieService(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.addAuthCookie(response, "signed-token");

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(1);
        assertThat(cookies.get(0))
                .contains("dream_auth=signed-token")
                .contains("Path=/")
                .contains("Max-Age=1800")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict");
    }

    @Test
    void readsTokenFromConfiguredCookie() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        AuthCookieService service = new AuthCookieService(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "ignored"), new Cookie("dream_auth", "signed-token"));

        assertThat(service.readTokenFromCookie(request)).contains("signed-token");
    }

    @Test
    void clearingAuthCookieExpiresIt() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        properties.setCookieSameSite("Lax");

        AuthCookieService service = new AuthCookieService(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearAuthCookie(response);

        assertThat(response.getHeaders("Set-Cookie").get(0))
                .contains("dream_auth=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }
}
