package com.github.hkocamandev.iot_intrusion_detection.config;

import com.github.hkocamandev.iot_intrusion_detection.security.AppUserDetailsService;
import com.github.hkocamandev.iot_intrusion_detection.security.JwtAuthenticationFilter;
import com.github.hkocamandev.iot_intrusion_detection.security.JwtProperties;
import com.github.hkocamandev.iot_intrusion_detection.security.JwtService;
import com.github.hkocamandev.iot_intrusion_detection.security.RestAccessDeniedHandler;
import com.github.hkocamandev.iot_intrusion_detection.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security. Static pages are permitAll because a browser cannot attach an
 * Authorization header to a navigation or a <script src> — protection is on the data, not the
 * page, and the dashboard's own JavaScript redirects to /login.html when it has no session.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // JwtProperties and SeedProperties are registered by the @ConfigurationPropertiesScan on
    // IotIntrusionDetectionApplication, same as LlmProperties and MlProperties. Do not add
    // @EnableConfigurationProperties here — it would duplicate that scan.

    @Bean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties.secret(), properties.ttl(), properties.issuer());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Built explicitly rather than via AuthenticationConfiguration, so the wiring is visible and
     * does not depend on which helper the current Spring Security version favours.
     */
    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http
                // No cookies are used, so there is no CSRF vector to protect.
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/login.html",
                                "/*.js", "/*.css", "/favicon.ico", "/error").permitAll()
                        // SockJS handshake is open; the token is checked on the STOMP CONNECT frame.
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login")
                        .permitAll()
                        // LLM calls cost money and quota, so they are ADMIN-only.
                        .requestMatchers("/api/chat", "/api/llm/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
