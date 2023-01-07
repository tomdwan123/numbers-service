/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.exception;

import java.util.UUID;

import com.messagemedia.numbers.service.client.models.Status;

public class TollFreeNumberUpdateStatusException extends RuntimeException {

    public TollFreeNumberUpdateStatusException(UUID numberId, Status status) {
        super(String.format("Number id {%s} can not update {%s} status", numberId, status));
    }
}
