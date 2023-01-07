/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.config;

import com.messagemedia.framework.config.PlatformEnvironmentConfig;
import com.messagemedia.framework.service.RestTemplateFactory;
import com.messagemedia.framework.service.config.RestUtilConfig;
import com.messagemedia.numbers.repository.config.NumbersDbConfig;
import com.messagemedia.numbers.repository.mappers.AssignmentMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(value = {
        PlatformEnvironmentConfig.class,
        NumbersDbConfig.class,
        AmsSecureConfig.class,
        AuditConfig.class,
        SQSConfig.class,
        RestUtilConfig.class
})
@PropertySource(value = {"classpath:config.properties"})
@ComponentScan(basePackages = {"com.messagemedia.numbers.service"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.messagemedia.numbers.service.client.config.*")})
@PropertySource("classpath:config.properties")
public class ServiceTestConfig {

    @Bean
    public AssignmentMapper assignmentMapper() {
        return Mappers.getMapper(AssignmentMapper.class);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateFactory restTemplateFactory) {
        return restTemplateFactory.create(5000, 10000);
    }
}
