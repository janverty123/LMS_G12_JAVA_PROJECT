package com.apptitle.auth.security;

import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Bridges our User entity to Spring Security's UserDetails contract.
 * "Username" here is always the account's email — there's no separate
 * username concept in this app.
 *
 * Role is exposed as a "ROLE_X" authority (ROLE_TEACHER / ROLE_STUDENT) so
 * later phases can use hasRole("TEACHER")/hasRole("STUDENT") in
 * @PreAuthorize expressions.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + email));

        return details(user);
    }

    public UserDetails loadUserById(UUID userId) {
        return details(userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found")));
    }

    private UserDetails details(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isEnabled())
                .build();
    }
}
