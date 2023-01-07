/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service.event;

import com.messagemedia.framework.test.logging.TestLoggingAppender;
import com.messagemedia.numbers.service.billing.SlackMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SlackNotificationEventListenerTest {

    private static final String SLACK_URL = "http://localhost:4200/def/123";

    private EventListener sut;

    @Mock
    private RestTemplate restTemplate;

    private final TestLoggingAppender appender = new TestLoggingAppender();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sut = new EventListener(restTemplate);
    }

    @Test
    public void onEvent() {
        // Given.
        SlackMessage message = new SlackMessage("Toll-free number was assigned.");
        final SlackNotificationEvent event = new SlackNotificationEvent(message, SLACK_URL);

        // When.
        sut.onEvent(event);

        // Then.
        verify(restTemplate).postForObject(SLACK_URL, message, String.class);
        verifyNoMoreInteractions(restTemplate);
    }

    @Test
    public void onEventSendSlackNotificationFails() {
        // Given.
        SlackMessage message = new SlackMessage("Toll-free number was assigned.");
        final SlackNotificationEvent event = new SlackNotificationEvent(message, SLACK_URL);
        when(restTemplate.postForObject(SLACK_URL, message, String.class))
                .thenThrow(new RuntimeException("exception"));

        // When.
        sut.onEvent(event);

        // Then.
        verify(restTemplate).postForObject(SLACK_URL, message, String.class);
        verifyNoMoreInteractions(restTemplate);
        appender.addInfo(String.format("Failed to post assigned number notification to slack"));
    }
}
