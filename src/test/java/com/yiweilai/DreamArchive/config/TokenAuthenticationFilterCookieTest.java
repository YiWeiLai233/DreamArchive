package com.yiweilai.DreamArchive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.service.AuthCookieProperties;
import com.yiweilai.DreamArchive.service.AuthCookieService;
import com.yiweilai.DreamArchive.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenAuthenticationFilterCookieTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUserFromHttpOnlyCookieWhenAuthorizationHeaderIsMissing() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        LoginMapper loginMapper = mock(LoginMapper.class);
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setCookieName("dream_auth");
        AuthCookieService authCookieService = new AuthCookieService(properties);
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
                tokenService,
                loginMapper,
                new ObjectMapper(),
                authCookieService
        );
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setDeleted(false);
        when(tokenService.parseToken("cookie-token")).thenReturn(new TokenService.AuthenticatedUser(7, "USER"));
        when(loginMapper.selectById(7)).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dream/abc");
        request.setCookies(new Cookie("dream_auth", "cookie-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(user);
        verify(tokenService).parseToken("cookie-token");
    }
}
