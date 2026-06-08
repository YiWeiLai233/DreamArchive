package com.yiweilai.DreamArchive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.service.AuthCookieService;
import com.yiweilai.DreamArchive.service.TokenService;
import com.yiweilai.DreamArchive.util.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private static final String LOGIN_REQUIRED = "\u8bf7\u5148\u767b\u5f55";
    private static final String INVALID_TOKEN = "\u767b\u5f55\u72b6\u6001\u65e0\u6548\u6216\u5df2\u8fc7\u671f";
    private static final String ACCOUNT_UNAVAILABLE = "\u8d26\u53f7\u4e0d\u53ef\u7528\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55";
    private static final String ACCOUNT_BANNED = "\u8d26\u53f7\u5df2\u88ab\u5c01\u7981";

    private final TokenService tokenService;
    private final LoginMapper loginMapper;
    private final ObjectMapper objectMapper;
    private final AuthCookieService authCookieService;

    public TokenAuthenticationFilter(TokenService tokenService,
                                     LoginMapper loginMapper,
                                     ObjectMapper objectMapper,
                                     AuthCookieService authCookieService) {
        this.tokenService = tokenService;
        this.loginMapper = loginMapper;
        this.objectMapper = objectMapper;
        this.authCookieService = authCookieService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || SecurityPaths.isPublicPath(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String cookieToken = authCookieService.readTokenFromCookie(request).orElse(null);
        if ((authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))
                && (cookieToken == null || cookieToken.isBlank())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            TokenService.AuthenticatedUser authenticatedUser = authorizationHeader != null && authorizationHeader.startsWith("Bearer ")
                    ? tokenService.parseBearerToken(authorizationHeader)
                    : tokenService.parseToken(cookieToken);
            User user = loginMapper.selectById(authenticatedUser.getUserId());
            if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
                writeResult(response, HttpStatus.UNAUTHORIZED, ACCOUNT_UNAVAILABLE);
                return;
            }
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                writeResult(response, HttpStatus.FORBIDDEN, ACCOUNT_BANNED);
                return;
            }

            String role = user.getRole() == null || user.getRole().isBlank() ? "USER" : user.getRole().trim().toUpperCase();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            writeResult(response, HttpStatus.UNAUTHORIZED, INVALID_TOKEN);
        }
    }

    private void writeResult(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (message == null || message.isBlank()) {
            message = LOGIN_REQUIRED;
        }
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(status.value(), message));
    }
}
