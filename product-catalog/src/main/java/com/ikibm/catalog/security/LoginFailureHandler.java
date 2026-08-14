package com.ikibm.catalog.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Login başarısızlığında: identifier rate-limit ile engellendiyse
 * `/login?blocked`, aksi halde `/login?error`.
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String identifier = request.getParameter("identifier");
        setDefaultFailureUrl(loginAttemptService.isBlocked(identifier) ? "/login?blocked" : "/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }
}
