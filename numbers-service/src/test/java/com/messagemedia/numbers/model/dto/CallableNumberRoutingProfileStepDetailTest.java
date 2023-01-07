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
import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfileStepDetail;
import static org.junit.Assert.assertEquals;

public class CallableNumberRoutingProfileStepDetailTest {

    @Test
    public void testGetters() throws Exception {
        assertGetters(randomCallableNumberRoutingProfileStepDetail());
    }

    @Test
    public void testSetters() throws Exception {
        CallableNumberRoutingProfileStepDetail callableNumberRoutingProfileStepDetail = randomCallableNumberRoutingProfileStepDetail();
        String expectedNumber = "new-number";

        callableNumberRoutingProfileStepDetail.setNumber(expectedNumber);

        assertEquals(expectedNumber, callableNumberRoutingProfileStepDetail.getNumber());
    }

    @Test
    public void testToString() {
        assertToString(randomCallableNumberRoutingProfileStepDetail());
    }
}
