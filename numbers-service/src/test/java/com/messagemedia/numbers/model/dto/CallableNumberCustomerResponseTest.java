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
import static com.messagemedia.numbers.TestDataCreateCallableNumber.randomCallableNumberCustomerResponse;
import static org.junit.Assert.assertEquals;

public class CallableNumberCustomerResponseTest {

    @Test
    public void testConstructor() {
        CallableNumberCustomerResponse original = randomCallableNumberCustomerResponse();
        CallableNumberCustomerResponse copy = new CallableNumberCustomerResponse(
                original.getId(),
                original.getCompany(),
                original.getDefaultRoutingProfileId(),
                original.getCurrentMinutes()
        );
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getCompany(), copy.getCompany());
        assertEquals(original.getDefaultRoutingProfileId(), copy.getDefaultRoutingProfileId());
        assertEquals(original.getCurrentMinutes(), copy.getCurrentMinutes());
    }

    @Test
    public void testGetters() throws Exception {
        assertGetters(randomCallableNumberCustomerResponse());
    }

    @Test
    public void testToString() {
        assertToString(randomCallableNumberCustomerResponse());
    }
}
