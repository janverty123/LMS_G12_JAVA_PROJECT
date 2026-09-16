package com.apptitle.auth.security;

import com.apptitle.config.JwtService;
import com.apptitle.user.entity.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = new JwtService("test-key-with-at-least-thirty-two-bytes-for-signing", 60000);
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void authenticate_afterEmailChangeUsesOriginalAccountId() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.generateToken(id, "old@example.com", Role.STUDENT));
        when(users.loadUserById(id)).thenReturn(User.withUsername("new@example.com").password("unused").roles("STUDENT").build());
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertEquals("new@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(users, never()).loadUserByUsername(anyString());
    }

    @Test
    void authenticate_disabledAccountHasNoPrincipal() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.generateToken(id, "old@example.com", Role.STUDENT));
        when(users.loadUserById(id)).thenReturn(User.withUsername("old@example.com").password("unused").roles("STUDENT").disabled(true).build());
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
