package com.ikibm.catalog.security;

import com.ikibm.catalog.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CatalogUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CatalogUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .map(CatalogUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));
    }
}
