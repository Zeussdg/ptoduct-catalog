package com.ikibm.catalog.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

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

    /** Yüklenen görselleri /uploads/** altından yerel disk klasöründen servis et. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(appProperties.getUpload().getDir())
                .toAbsolutePath().normalize().toUri().toString(); // file:///.../uploads/
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
