package com.ikibm.catalog.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Giriş sonrası yönlendirme: ADMIN/SUPER_ADMIN → /admin, CUSTOMER → kayıtlı
 * hedef (varsa) ya da ana sayfa.
 */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_SUPER_ADMIN"));

        if (isAdmin) {
            getRedirectStrategy().sendRedirect(request, response, "/admin");
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
