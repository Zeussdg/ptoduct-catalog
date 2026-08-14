package com.ikibm.catalog.security;

import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CatalogUserDetails implements UserDetails {

    private final User user;

    public CatalogUserDetails(User user) {
        this.user = user;
    }

    public User getUser() { return user; }

    public Integer getId() { return user.getId(); }

    public String getDisplayName() { return user.getDisplayName(); }

    public String getRole() { return user.getRole().name(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash() == null ? "" : user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return user.getStatus() != UserStatus.SUSPENDED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return user.getStatus() == UserStatus.ACTIVE; }
}
