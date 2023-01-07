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
import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfileStep;
import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfileStepDetail;
import static org.junit.Assert.assertEquals;

public class CallableNumberRoutingProfileStepTest {

    @Test
    public void testGetters() throws Exception {
        assertGetters(randomCallableNumberRoutingProfileStep());
    }

    @Test
    public void testSetters() throws Exception {
        CallableNumberRoutingProfileStep callableNumberRoutingProfileStep = randomCallableNumberRoutingProfileStep();
        String expectedRoutingType = "new-type";
        CallableNumberRoutingProfileStepDetail expectedStepDetail = randomCallableNumberRoutingProfileStepDetail();

        callableNumberRoutingProfileStep.setRoutingType(expectedRoutingType);
        callableNumberRoutingProfileStep.setDirectDetails(expectedStepDetail);

        assertEquals(expectedRoutingType, callableNumberRoutingProfileStep.getRoutingType());
        assertEquals(expectedStepDetail, callableNumberRoutingProfileStep.getDirectDetails());
    }

    @Test
    public void testToString() {
        assertToString(randomCallableNumberRoutingProfileStep());
    }
}
