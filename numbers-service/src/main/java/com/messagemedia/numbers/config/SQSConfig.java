/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.messagemedia.framework.json.JsonFastMapper;
import com.messagemedia.framework.queueing.sqs.bootstrap.EnableSQS;
import com.messagemedia.numbers.sqs.SQSQueuePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableSQS
public class SQSConfig {
    @Bean(name = "provisioningSQSQueuePublisher")
    public SQSQueuePublisher provisioningSQSQueuePublisher(AmazonSQS amazonSQS,
                                           @Value("${service.numbers-service.provisioning-queue.url}") String provisioningQueueUrl,
                                           JsonFastMapper mapper) {
        return new SQSQueuePublisher(amazonSQS, provisioningQueueUrl, mapper);
    }
}
