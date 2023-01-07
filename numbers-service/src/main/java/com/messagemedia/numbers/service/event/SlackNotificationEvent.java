/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service.event;

import com.messagemedia.numbers.service.billing.SlackMessage;

public final class SlackNotificationEvent {

    private final SlackMessage payload;
    private final String slackUrl;

    public SlackNotificationEvent(final SlackMessage payload, String slackUrl) {
        this.payload = payload;
        this.slackUrl = slackUrl;
    }

    public SlackMessage getPayload() {
        return this.payload;
    }

    public String getSlackUrl() {
        return this.slackUrl;
    }
}
