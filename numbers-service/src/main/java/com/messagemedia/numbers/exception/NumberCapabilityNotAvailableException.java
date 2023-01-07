/*
 * Copyright (c) Message4U Pty Ltd 2014-2021
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.exception;

import com.messagemedia.numbers.service.client.models.ServiceType;

import java.util.UUID;

public class NumberCapabilityNotAvailableException extends RuntimeException {

    public NumberCapabilityNotAvailableException(UUID numberId, ServiceType capability) {
        super(String.format("{%s} capability for number id {%s} is not available", capability, numberId));
    }
}
