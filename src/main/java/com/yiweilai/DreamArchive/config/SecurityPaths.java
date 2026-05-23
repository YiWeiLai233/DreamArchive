package com.yiweilai.DreamArchive.config;

final class SecurityPaths {
    static final String[] PUBLIC_ENDPOINTS = {
            "/api/login",
            "/api/login/**",
            "/api/register",
            "/api/register/**",
            "/api/reset-password",
            "/api/reset-password/**",
            "/api/hello",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    private SecurityPaths() {
    }

    static boolean isPublicPath(String path) {
        return "/api/login".equals(path)
                || path.startsWith("/api/login/")
                || "/api/register".equals(path)
                || path.startsWith("/api/register/")
                || "/api/reset-password".equals(path)
                || path.startsWith("/api/reset-password/")
                || "/api/hello".equals(path)
                || "/v3/api-docs".equals(path)
                || path.startsWith("/v3/api-docs/")
                || "/swagger-ui.html".equals(path)
                || path.startsWith("/swagger-ui/");
    }
}
