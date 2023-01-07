/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.config;

import com.messagemedia.service.accountmanagement.client.config.ServiceAccountManagementClientConfig;
import com.messagemedia.service.accountmanagement.client.model.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    ServiceAccountManagementClientConfig.class
})
public class AmsSecureConfig {

    @Bean
    public Credentials amsCredentials(@Value("${service.accountmanagement.api.login.vendor}") String vendor,
        @Value("${service.accountmanagement.api.login.username}") String username,
        @Value("${service.accountmanagement.api.login.password}") String password) {
        return new Credentials(username, password, vendor);
    }

}
