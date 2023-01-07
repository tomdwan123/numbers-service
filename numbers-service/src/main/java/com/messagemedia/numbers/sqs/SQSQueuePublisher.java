/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.messagemedia.framework.json.JsonFastMapper;
import com.messagemedia.numbers.exception.EventNotificationPublishingFailedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class SQSQueuePublisher {

    private final AmazonSQS amazonSqs;
    private final String queueUrl;
    private JsonFastMapper mapper;

    @Autowired
    public SQSQueuePublisher(AmazonSQS amazonSqs, String queueUrl, JsonFastMapper mapper) {
        this.amazonSqs = amazonSqs;
        this.queueUrl = queueUrl;
        this.mapper = mapper;
    }

    public <T> void publish(T item) throws EventNotificationPublishingFailedException {
        try {
            amazonSqs.sendMessage(queueUrl, mapper.toJsonString(item));
        } catch (IOException e) {
            throw new EventNotificationPublishingFailedException("Unable to publish message to SQS queue: " + queueUrl, e);
        }
    }
}
