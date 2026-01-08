package com.bntech.lexai.gateway.config;

import com.bntech.lexai.gateway.config.converter.StringToCasePartyRoleConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToCasePartyRoleConverter());
    }
}
