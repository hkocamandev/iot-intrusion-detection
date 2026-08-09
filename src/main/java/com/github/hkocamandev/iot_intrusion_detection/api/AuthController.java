package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.AuthResponse;
import com.github.hkocamandev.iot_intrusion_detection.dto.LoginRequest;
import com.github.hkocamandev.iot_intrusion_detection.dto.RegisterRequest;
import com.github.hkocamandev.iot_intrusion_detection.dto.UserInfo;
import com.github.hkocamandev.iot_intrusion_detection.model.AppUser;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import com.github.hkocamandev.iot_intrusion_detection.repository.UserRepository;
import com.github.hkocamandev.iot_intrusion_detection.security.IssuedToken;
import com.github.hkocamandev.iot_intrusion_detection.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Stream;

/** Registration, login and session introspection. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Creates an account. The role is fixed to ANALYST here and cannot be influenced by the
     * request: handing out ADMIN from an open endpoint on a security console is not acceptable.
     * ADMIN accounts come only from UserSeeder.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserInfo register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new UsernameTakenException();
        }
        AppUser saved = users.save(new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.ANALYST,
                Instant.now()));
        return new UserInfo(saved.getUsername(), saved.getRole());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.username(), request.password()));
        } catch (AuthenticationException e) {
            // Every failure mode collapses into one exception so the response cannot be used
            // to distinguish "no such user" from "wrong password".
            throw new InvalidCredentialsException();
        }
        AppUser user = users.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        IssuedToken issued = jwtService.issue(user.getUsername(), user.getRole());
        return new AuthResponse(issued.token(), user.getUsername(), user.getRole(),
                issued.expiresAt());
    }

    /**
     * Lets the dashboard validate its stored token against the server on load, instead of
     * trusting localStorage, and supplies the authoritative username and role for the UI.
     *
     * <p>Reads the SecurityContextHolder directly rather than accepting {@code Authentication}
     * as a method parameter: the latter is resolved from the servlet request's user principal,
     * which is populated only by a filter (e.g. SecurityContextHolderAwareRequestFilter) that is
     * present on the real filter chain but absent from a {@code @WebMvcTest} slice.
     */
    @GetMapping("/me")
    public UserInfo me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new UserInfo(authentication.getName(), roleOf(authentication));
    }

    /**
     * Defensive: {@code /api/auth/me} is {@code authenticated()} in {@code SecurityConfig}, so
     * a null or anonymous {@code Authentication} should never reach here (pinned by
     * {@code SecurityAuthorizationIT.anonymousMeIsRejectedBeforeReachingTheController}). The
     * guard exists so correctness does not depend on that reachability: any authentication this
     * method cannot map to a known {@link Role} becomes a clean 401 instead of a 500.
     */
    private static Role roleOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialsException();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .flatMap(a -> {
                    try {
                        return Stream.of(Role.valueOf(a.substring(ROLE_PREFIX.length())));
                    } catch (IllegalArgumentException e) {
                        return Stream.empty();
                    }
                })
                .findFirst()
                .orElseThrow(InvalidCredentialsException::new);
    }

    /** Registration conflict — 409. */
    public static class UsernameTakenException extends RuntimeException { }

    /** Login failure of any kind — 401, always with the same body. */
    public static class InvalidCredentialsException extends RuntimeException { }
}
