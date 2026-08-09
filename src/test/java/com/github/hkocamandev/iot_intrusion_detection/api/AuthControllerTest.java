package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.model.AppUser;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import com.github.hkocamandev.iot_intrusion_detection.repository.UserRepository;
import com.github.hkocamandev.iot_intrusion_detection.security.IssuedToken;
import com.github.hkocamandev.iot_intrusion_detection.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserRepository users;
    @MockitoBean PasswordEncoder passwordEncoder;
    @MockitoBean JwtService jwtService;
    @MockitoBean AuthenticationManager authenticationManager;

    private static final String ALICE_JSON = "{\"username\":\"alice\",\"password\":\"correct-horse\"}";

    @Test
    void registersNewUserAsAnalyst() throws Exception {
        when(users.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse")).thenReturn("$2a$hash");
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(ALICE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    void ignoresRoleSuppliedByTheClient() throws Exception {
        when(users.existsByUsername("mallory")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse")).thenReturn("$2a$hash");
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"username\":\"mallory\",\"password\":\"correct-horse\","
                                + "\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    void rejectsDuplicateUsernameWith409() throws Exception {
        when(users.existsByUsername("alice")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(ALICE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("username_taken"));

        verify(users, never()).save(any());
    }

    @Test
    void rejectsShortPasswordWith400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void rejectsBlankUsernameWith400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"username\":\"\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.username").exists());
    }

    @Test
    void issuesTokenOnSuccessfulLogin() throws Exception {
        Instant expiry = Instant.parse("2026-08-08T12:00:00Z");
        when(users.findByUsername("alice")).thenReturn(Optional.of(
                new AppUser("alice", "$2a$hash", Role.ADMIN, Instant.now())));
        when(jwtService.issue("alice", Role.ADMIN)).thenReturn(new IssuedToken("tok-123", expiry));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json").content(ALICE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok-123"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-08T12:00:00Z"));
    }

    @Test
    void returnsGeneric401OnWrongPassword() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json").content(ALICE_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void returnsTheSame401ForUnknownUsername() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("no such user"));

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"nobody\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isUnauthorized())
                // Byte-identical to the wrong-password body: no username enumeration.
                .andExpect(jsonPath("$.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "alice", roles = "ADMIN")
    void reportsCurrentSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "bob", roles = "ANALYST")
    void reportsAnalystSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    void hashesThePasswordBeforeSaving() throws Exception {
        when(users.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse")).thenReturn("$2a$hash");
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(ALICE_JSON))
                .andExpect(status().isCreated());

        verify(passwordEncoder).encode(eq("correct-horse"));
    }
}
