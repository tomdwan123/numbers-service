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
import static com.messagemedia.numbers.TestDataCreateCallableNumber.randomCallableNumberUpdateRequest;
import static org.junit.Assert.assertEquals;

public class CallableNumberUpdateRequestTest {

    @Test
    public void testConstructor() {
        CallableNumberUpdateRequest original = randomCallableNumberUpdateRequest();
        CallableNumberUpdateRequest copy = new CallableNumberUpdateRequest(original.getRoutingProfileId(), original.getName().get());
        assertEquals(original.getRoutingProfileId(), copy.getRoutingProfileId());
        assertEquals(original.getName().get(), copy.getName().get());
    }

    @Test
    public void testGetters() throws Exception {
        assertGetters(randomCallableNumberUpdateRequest());
    }

    @Test
    public void testToString() {
        assertToString(randomCallableNumberUpdateRequest());
    }
}
