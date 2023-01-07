/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.numbers.exception.EventNotificationPublishingFailedException;
import com.messagemedia.numbers.service.client.models.Event;
import com.messagemedia.numbers.service.client.models.EventNotification;
import com.messagemedia.numbers.service.client.models.NumberDto;
import com.messagemedia.numbers.sqs.SQSQueuePublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SQSQueuePublisher provisioningSQSQueuePublisher;

    @Autowired
    public NotificationService(@Qualifier("provisioningSQSQueuePublisher") SQSQueuePublisher provisioningSQSQueuePublisher) {
        this.provisioningSQSQueuePublisher = provisioningSQSQueuePublisher;
    }

    public void push(Event event, NumberDto numberDto) throws EventNotificationPublishingFailedException {
        EventNotification eventNotification = new EventNotification(event, convertToNumberWithoutAssignment(numberDto), numberDto.getAssignedTo());
        provisioningSQSQueuePublisher.publish(eventNotification);
    }

    private NumberDto convertToNumberWithoutAssignment(NumberDto numberDto) {
        NumberDto numberDtoClone = new NumberDto(numberDto);
        numberDtoClone.setAssignedTo(null);
        return numberDtoClone;
    }
}
