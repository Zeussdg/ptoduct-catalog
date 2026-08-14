package com.ikibm.catalog.security;

import com.ikibm.catalog.repository.UserRepository;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CatalogUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    public CatalogUserDetailsService(UserRepository userRepository, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (loginAttemptService.isBlocked(identifier)) {
            throw new LockedException("Çok fazla başarısız deneme. Lütfen 15 dakika sonra tekrar deneyin.");
        }
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .map(CatalogUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));
    }
}
