package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.AppUser;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import com.github.hkocamandev.iot_intrusion_detection.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSeederTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private UserSeeder seeder(String username, String password) {
        return new UserSeeder(users, encoder, new SeedProperties(username, password));
    }

    @Test
    void seedsAdminWhenTableIsEmpty() throws Exception {
        when(users.count()).thenReturn(0L);
        when(encoder.encode("s3cret-password")).thenReturn("$2a$hash");

        seeder("admin", "s3cret-password").run(null);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() throws Exception {
        when(users.count()).thenReturn(3L);

        seeder("admin", "s3cret-password").run(null);

        verify(users, never()).save(any());
    }

    @Test
    void doesNothingWhenNoPasswordIsConfigured() throws Exception {
        seeder("admin", null).run(null);
        seeder("admin", "   ").run(null);

        verify(users, never()).save(any());
        // The count query is not even issued when there is nothing to seed with.
        verify(users, never()).count();
    }

    @Test
    void neverLogsOrReturnsThePlaintextPassword() throws Exception {
        when(users.count()).thenReturn(0L);
        when(encoder.encode("s3cret-password")).thenReturn("$2a$hash");

        seeder("admin", "s3cret-password").run(null);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("s3cret-password");
    }
}
