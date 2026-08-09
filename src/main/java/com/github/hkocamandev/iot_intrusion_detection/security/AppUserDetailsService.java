package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.AppUser;
import com.github.hkocamandev.iot_intrusion_detection.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Loads accounts for password authentication at login. The request path does not use this. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = users.findByUsername(username)
                // The message is never surfaced to the client: AuthController converts every
                // authentication failure into one generic 401.
                .orElseThrow(() -> new UsernameNotFoundException("No such user"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().authority())))
                .build();
    }
}
