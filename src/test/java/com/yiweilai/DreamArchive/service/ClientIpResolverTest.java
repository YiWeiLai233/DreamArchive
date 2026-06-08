package com.yiweilai.DreamArchive.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void usesFirstForwardedForAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.5");
        request.setRemoteAddr("127.0.0.1");

        assertThat(new ClientIpResolver().resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToRemoteAddressWhenProxyHeadersAreMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.8");

        assertThat(new ClientIpResolver().resolve(request)).isEqualTo("198.51.100.8");
    }
}
