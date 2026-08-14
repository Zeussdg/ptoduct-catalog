package com.ikibm.catalog.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
public class WebConfig {

    /**
     * MultipartFilter'ı Spring Security zincirinden ÖNCE çalıştırır; böylece
     * multipart form POST'larındaki CSRF token'ı (gizli alan) CsrfFilter body
     * parse edilmeden okuyabilir. Aksi halde görsel yüklemeleri 403 alır.
     */
    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilterRegistration() {
        FilterRegistrationBean<MultipartFilter> reg = new FilterRegistrationBean<>(new MultipartFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}
