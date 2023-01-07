/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.collect.ImmutableMap;
import com.messagemedia.framework.config.JsonConfig;
import com.messagemedia.framework.json.JsonFastMapper;
import com.messagemedia.numbers.exception.EventNotificationPublishingFailedException;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(DataProviderRunner.class)
public class SQSQueuePublisherTest {

    private static final String QUEUE_URL = "http://localhost:9324/queue/testQueue";
    private SQSQueuePublisher queuePublisher;
    private JsonFastMapper mapper = new JsonConfig().fastMapper();
    private Map<String, String> message = ImmutableMap.of("foo1", "bar1", "foo2", "bar2");

    @Mock
    private AmazonSQS amazonSqs;

    @Mock
    private JsonFastMapper mapperMock;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(amazonSqs.sendMessage(anyString(), anyString())).thenReturn(null);
        this.queuePublisher = new SQSQueuePublisher(amazonSqs, QUEUE_URL, mapper);
    }

    @Test
    public void shouldPublishMessageSuccessful() throws Exception {
        this.queuePublisher.publish(message);
        verify(amazonSqs, times(1)).sendMessage(QUEUE_URL, mapper.toJsonString(message));
    }

    @Test(expected = EventNotificationPublishingFailedException.class)
    public void shouldLogErrorMessageForPublishingFail() throws Exception {
        SQSQueuePublisher publisher = new SQSQueuePublisher(amazonSqs, QUEUE_URL, mapperMock);
        doThrow(new IOException("test")).when(mapperMock).toJsonString(anyObject());
        publisher.publish(message);
    }
}
