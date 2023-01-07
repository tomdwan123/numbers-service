/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.numbers.repository.entities.NumberEntity;

import java.util.List;
import java.util.UUID;

public class NumberListResult {
    private List<NumberEntity> numbers;
    private UUID token;
    public NumberListResult(List<NumberEntity> numbers, UUID token) {
        this.numbers = numbers;
        this.token = token;
    }

    public List<NumberEntity> getNumbers() {
        return numbers;
    }

    public UUID getToken() {
        return token;
    }
}
