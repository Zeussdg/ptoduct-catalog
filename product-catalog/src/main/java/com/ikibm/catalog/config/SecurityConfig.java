package com.ikibm.catalog.config;

import com.ikibm.catalog.security.LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Node bcryptjs (strength 12) hash'leriyle uyumlu.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/urun/**", "/is-ortaklarimiz", "/teklif-sihirbazi", "/quote/pdf",
                        "/login", "/error", "/api/catalog/**",
                        "/css/**", "/js/**", "/images/**", "/logo/**", "/fonts/**",
                        "/favicon.svg", "/icons.svg", "/favicon.ico"
                ).permitAll()
                .requestMatchers("/admin/users/**", "/admin/audit-logs/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/account/**", "/cart/**", "/quotes/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("identifier")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );
        return http.build();
    }
}
