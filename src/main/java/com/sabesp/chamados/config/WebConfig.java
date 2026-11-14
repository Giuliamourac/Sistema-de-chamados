package com.sabesp.chamados.config;

import com.sabesp.chamados.security.AdminInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;

    public WebConfig(AdminInterceptor adminInterceptor) {
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Toda a área administrativa passa pela checagem de perfil no servidor
        registry.addInterceptor(adminInterceptor).addPathPatterns("/admin", "/admin/**");
    }
}
