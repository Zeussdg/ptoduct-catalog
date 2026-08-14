package com.ikibm.catalog.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Spring Security auth event'lerini dinleyip login deneme sayacını günceller:
 * yanlış şifre → başarısızlık kaydı; başarılı giriş → sıfırla.
 */
@Component
public class AuthAttemptListener {

    private final LoginAttemptService loginAttemptService;

    public AuthAttemptListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptService.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.reset(event.getAuthentication().getName());
    }
}
