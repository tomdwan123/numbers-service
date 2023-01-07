/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service.event;

import com.messagemedia.framework.logging.Logger;
import com.messagemedia.framework.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

@Component
public class EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);

    // Can't have blank SSM property values so we default to 'NONE' when no URL supplied
    private static final String NO_SLACK_URL_VALUE = "NONE";
    private final RestTemplate restTemplate;

    @Autowired
    public EventListener(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    @TransactionalEventListener
    public void onEvent(final @NonNull SlackNotificationEvent event) {
        String slackNotificationUrl = event.getSlackUrl();
        if (StringUtils.isNotBlank(slackNotificationUrl) && !NO_SLACK_URL_VALUE.equals(slackNotificationUrl)) {
            try {
                restTemplate.postForObject(slackNotificationUrl, event.getPayload(), String.class);
            } catch (Exception e) {
                LOGGER.warnWithReason("Failed to post assigned number notification to slack", e.getMessage(), e);
            }
        }
    }
}
