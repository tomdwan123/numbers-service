/*
 * Copyright (c) Message4U Pty Ltd 2014-2021
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.model.dto;

import org.junit.Test;


import static com.messagemedia.framework.test.AccessorAsserter.assertGetters;
import static com.messagemedia.framework.test.CanonicalAsserter.assertToString;
import static com.messagemedia.numbers.TestDataCreateCallableNumber.randomCallableNumberRoutingProfileCreateResponse;
import static org.junit.Assert.assertEquals;

public class CallableNumberRoutingProfileCreateResponseTest {

    @Test
    public void testConstructor() {
        CallableNumberRoutingProfileCreateResponse original = randomCallableNumberRoutingProfileCreateResponse();
        CallableNumberRoutingProfileCreateResponse copy = new CallableNumberRoutingProfileCreateResponse(
                original.getId(),
                original.getCustomerId(),
                original.getName(),
                original.getSteps()
        );
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getCustomerId(), copy.getCustomerId());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getSteps(), copy.getSteps());
    }

    @Test
    public void testGetters() throws Exception {
        assertGetters(randomCallableNumberRoutingProfileCreateResponse());
    }

    @Test
    public void testToString() {
        assertToString(randomCallableNumberRoutingProfileCreateResponse());
    }
}
