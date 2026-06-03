package com.yiweilai.DreamArchive.config;

import com.yiweilai.DreamArchive.service.AuthCookieProperties;
import com.yiweilai.DreamArchive.service.AuthCookieService;
import com.yiweilai.DreamArchive.service.CsrfTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final AuthCookieProperties properties;
    private final AuthCookieService authCookieService;
    private final CsrfTokenService csrfTokenService;

    public CsrfProtectionFilter(AuthCookieProperties properties, CsrfTokenService csrfTokenService) {
        this(properties, new AuthCookieService(properties), csrfTokenService);
    }

    @Autowired
    public CsrfProtectionFilter(AuthCookieProperties properties,
                                AuthCookieService authCookieService,
                                CsrfTokenService csrfTokenService) {
        this.properties = properties;
        this.authCookieService = authCookieService;
        this.csrfTokenService = csrfTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!requiresCsrf(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfCookie = authCookieService.readCsrfFromCookie(request).orElse(null);
        String csrfHeader = request.getHeader(properties.getCsrfHeaderName());
        if (!csrfTokenService.matches(csrfCookie, csrfHeader)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF token invalid\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresCsrf(HttpServletRequest request) {
        if (!Boolean.TRUE.equals(properties.getCsrfEnabled())) {
            return false;
        }
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            return false;
        }
        return authCookieService.hasAuthCookie(request);
    }
}
