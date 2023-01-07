/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.config;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.service.RestTemplateFactory;
import com.messagemedia.framework.service.config.RestUtilConfig;
import com.messagemedia.framework.web.config.BaseWebContext;
import com.messagemedia.numbers.repository.config.NumbersDbConfig;
import com.messagemedia.numbers.repository.mappers.AssignmentMapper;
import com.messagemedia.numbers.repository.mappers.NumberAssignmentMapper;
import com.messagemedia.numbers.repository.mappers.NumberForwardMapper;
import com.messagemedia.numbers.repository.mappers.NumbersMapper;
import com.messagemedia.numbers.service.client.models.AuditToken;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan(basePackages = {"com.messagemedia.numbers"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.messagemedia.numbers.service.client.config.*")})
@PropertySource("classpath:config.properties")
@Import({
        NumbersDbConfig.class,
        AmsSecureConfig.class,
        AuditConfig.class,
        SQSConfig.class,
        RestUtilConfig.class
})
@EnableAsync
public class WorkerContext extends BaseWebContext {

    @Bean
    public NumbersMapper numbersMapper() {
        return Mappers.getMapper(NumbersMapper.class);
    }

    @Bean
    public AssignmentMapper assignmentMapper() {
        return Mappers.getMapper(AssignmentMapper.class);
    }

    @Bean
    public NumberForwardMapper numberForwardMapper() {
        return Mappers.getMapper(NumberForwardMapper.class);
    }

    @Bean
    public NumberAssignmentMapper numberAssignmentMapper() {
        return Mappers.getMapper(NumberAssignmentMapper.class);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateFactory restTemplateFactory,
                                     @Value("${service.add-on-billing-manager.connectionTimeout}") int connectionTimeout,
                                     @Value("${service.add-on-billing-manager.readTimeout}") int readTimeout) {
        return restTemplateFactory.create(connectionTimeout, readTimeout);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, AuditToken>() {
            @Nullable
            @Override
            public AuditToken convert(String source) {
                return AuditToken.fromString(source);
            }
        });

        registry.addConverter(new Converter<String, VendorAccountId>() {
            @Nullable
            @Override
            public VendorAccountId convert(String source) {
                return VendorAccountId.fromColonString(source);
            }
        });

        DateTimeFormatterRegistrar dateTimeFormatterRegistrar = new DateTimeFormatterRegistrar();
        dateTimeFormatterRegistrar.setUseIsoFormat(true);
        dateTimeFormatterRegistrar.registerFormatters(registry);
    }
}
