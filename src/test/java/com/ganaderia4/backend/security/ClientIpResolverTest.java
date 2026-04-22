package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void shouldUseRemoteAddressByDefault() {
        AbuseProtectionProperties properties = new AbuseProtectionProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.10");
    }

    @Test
    void shouldUseFirstForwardedAddressWhenTrusted() {
        AbuseProtectionProperties properties = new AbuseProtectionProperties();
        properties.getClientIp().setTrustForwardedHeaders(true);
        ClientIpResolver resolver = new ClientIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.10");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void shouldNormalizeUnsafeWhitespaceAndIpv4Port() {
        AbuseProtectionProperties properties = new AbuseProtectionProperties();
        properties.getClientIp().setTrustForwardedHeaders(true);
        ClientIpResolver resolver = new ClientIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5:443\r\n");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }
}
