package com.yiweilai.DreamArchive.config;

import com.yiweilai.DreamArchive.service.AuthCookieProperties;
import com.yiweilai.DreamArchive.service.CsrfTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfProtectionFilterTest {

    @Test
    void rejectsUnsafeRequestWithAuthCookieWhenCsrfHeaderIsMissing() throws ServletException, IOException {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        properties.setCsrfCookieName("XSRF-TOKEN");
        properties.setCsrfHeaderName("X-XSRF-TOKEN");
        CsrfProtectionFilter filter = new CsrfProtectionFilter(properties, new CsrfTokenService());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/dreams/save-and-analyze");
        request.setCookies(new Cookie("dream_auth", "signed-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CSRF");
    }

    @Test
    void allowsUnsafeRequestWithMatchingCsrfCookieAndHeader() throws ServletException, IOException {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        properties.setCsrfCookieName("XSRF-TOKEN");
        properties.setCsrfHeaderName("X-XSRF-TOKEN");
        CsrfProtectionFilter filter = new CsrfProtectionFilter(properties, new CsrfTokenService());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/dreams/save-and-analyze");
        request.setCookies(
                new Cookie("dream_auth", "signed-token"),
                new Cookie("XSRF-TOKEN", "csrf-token")
        );
        request.addHeader("X-XSRF-TOKEN", "csrf-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsPublicLoginWithoutCsrfHeader() throws ServletException, IOException {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        properties.setCsrfCookieName("XSRF-TOKEN");
        properties.setCsrfHeaderName("X-XSRF-TOKEN");
        CsrfProtectionFilter filter = new CsrfProtectionFilter(properties, new CsrfTokenService());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
