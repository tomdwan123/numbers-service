/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.numbers.service.client.models.Event;
import com.messagemedia.numbers.service.client.models.EventNotification;
import com.messagemedia.numbers.sqs.SQSQueuePublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberDto;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {

    @Mock
    private SQSQueuePublisher provisioningQueue;

    @InjectMocks
    private NotificationService notificationService;

    @Before
    public void setup()  {
        notificationService = new NotificationService(provisioningQueue);
    }

    @Test
    public void testPush() {
        notificationService.push(Event.NUMBER_UPDATED, randomUnassignedNumberDto());
        verify(provisioningQueue).publish(any(EventNotification.class));
        verifyNoMoreInteractions(provisioningQueue);
    }
}
