package com.tailorshop.metric.security;

import com.tailorshop.metric.entity.User;
import com.tailorshop.metric.entity.UserRole;
import com.tailorshop.metric.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation
 * Loads user details from database for Spring Security
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRoles(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .map(UserRole::getName)
            .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()))
            .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(user.getIsLocked())
            .credentialsExpired(false)
            .disabled(!user.getIsActive())
            .build();
    }

}
