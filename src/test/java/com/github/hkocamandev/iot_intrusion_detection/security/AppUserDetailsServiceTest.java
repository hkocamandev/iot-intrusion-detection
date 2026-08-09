package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.AppUser;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import com.github.hkocamandev.iot_intrusion_detection.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppUserDetailsServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final AppUserDetailsService service = new AppUserDetailsService(repository);

    @Test
    void mapsUserToUserDetailsWithRoleAuthority() {
        when(repository.findByUsername("alice")).thenReturn(Optional.of(
                new AppUser("alice", "$2a$hash", Role.ADMIN, Instant.now())));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("$2a$hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void mapsAnalystRole() {
        when(repository.findByUsername("bob")).thenReturn(Optional.of(
                new AppUser("bob", "$2a$hash", Role.ANALYST, Instant.now())));

        assertThat(service.loadUserByUsername("bob").getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ANALYST");
    }

    @Test
    void throwsForUnknownUsername() {
        when(repository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
