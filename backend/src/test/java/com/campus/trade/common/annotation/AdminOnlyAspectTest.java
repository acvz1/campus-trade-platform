package com.campus.trade.common.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminOnlyAspectTest {
    private final AdminOnlyAspect aspect = new AdminOnlyAspect();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsNormalUser() {
        authenticateAs("ROLE_USER");
        assertThatThrownBy(aspect::verifyAdminRole).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void allowsAdminAndSuperAdmin() {
        authenticateAs("ROLE_ADMIN");
        assertThatCode(aspect::verifyAdminRole).doesNotThrowAnyException();

        authenticateAs("ROLE_SUPER_ADMIN");
        assertThatCode(aspect::verifyAdminRole).doesNotThrowAnyException();
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority(role))));
    }
}
