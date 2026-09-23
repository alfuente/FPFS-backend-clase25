package com.shopeasy.security;

import com.shopeasy.model.Role;
import com.shopeasy.model.User;
import com.shopeasy.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class SecurityTest {
    JwtUtil jwt;
    @BeforeEach void setup() {
        SecurityContextHolder.clearContext();
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "test-only-signing-secret-0123456789abcdef0123456789abcdef0123456789");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void signedTokenPreservesEmail() {
        String token = jwt.generateToken("user@test.com");
        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.extractEmail(token)).isEqualTo("user@test.com");
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"invalid", "a.b.c", "Bearer token"})
    void malformedTokenIsRejected(String token) { assertThat(jwt.validateToken(token)).isFalse(); }
    @Test void expiredTokenIsRejectedWithoutSleeping() {
        ReflectionTestUtils.setField(jwt, "expiration", -60000L);
        assertThat(jwt.validateToken(jwt.generateToken("user@test.com"))).isFalse();
    }
    @Test void wrongSignatureIsRejected() {
        String token = jwt.generateToken("user@test.com");
        ReflectionTestUtils.setField(jwt, "secret", "different-signing-secret-0123456789abcdef0123456789abcdef0123456789");
        assertThat(jwt.validateToken(token)).isFalse();
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"Basic abc", "bearer abc", "Bearer invalid"})
    void filterContinuesWithoutAuthenticationForMissingOrInvalidBearer(String header) throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter();
        UserDetailsService details = mock(UserDetailsService.class);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", details);
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (header != null) request.addHeader("Authorization", header);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response); verifyNoInteractions(details);
    }
    @Test void filterAuthenticatesWithAuthoritiesAndDoesNotReplaceExistingAuthentication() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter();
        UserDetailsService details = mock(UserDetailsService.class);
        UserDetails principal = org.springframework.security.core.userdetails.User.withUsername("user@test.com")
                .password("encoded").roles("USER").build();
        when(details.loadUserByUsername("user@test.com")).thenReturn(principal);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", details);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.generateToken("user@test.com"));
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("user@test.com"); assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(auth.getDetails()).isNotNull();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(auth);
        verify(details, times(1)).loadUserByUsername("user@test.com");
        verify(chain, times(2)).doFilter(eq(request), any());
    }
    @Test void filterWithNullSubjectContinuesWithoutUserLookup() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class); when(mockJwt.validateToken("token")).thenReturn(true);
        JwtAuthFilter filter = new JwtAuthFilter(); UserDetailsService details = mock(UserDetailsService.class);
        ReflectionTestUtils.setField(filter, "jwtUtil", mockJwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", details);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer token");
        FilterChain chain = mock(FilterChain.class); MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verifyNoInteractions(details); verify(chain).doFilter(request, response);
    }
    @Test void userDetailsMapsPasswordRolesAndDisabledFlag() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User(); user.setEmail("user@test.com"); user.setPassword("encoded"); user.setEnabled(false);
        user.setRoles(Set.of(new Role(1L, "ROLE_ADMIN"), new Role(2L, "ROLE_USER")));
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(); ReflectionTestUtils.setField(service, "userRepository", repository);
        UserDetails details = service.loadUserByUsername(user.getEmail());
        assertThat(details.getUsername()).isEqualTo(user.getEmail()); assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.isEnabled()).isFalse();
        assertThat(details.getAuthorities()).extracting("authority").containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        assertThatThrownBy(() -> service.loadUserByUsername("missing")).isInstanceOf(UsernameNotFoundException.class);
    }
}
