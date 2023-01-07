/*
 * Copyright (c) Message4U Pty Ltd 2014-2021
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.model.dto;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static com.messagemedia.framework.test.AccessorAsserter.assertGetters;
import static com.messagemedia.framework.test.CanonicalAsserter.assertToString;
import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfile;
import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfileStep;
import static org.junit.Assert.assertEquals;

public class CallableNumberRoutingProfileTest {

    @Test
    public void testConstructor() {
        CallableNumberRoutingProfile original = randomCallableNumberRoutingProfile();
        CallableNumberRoutingProfile copy = new CallableNumberRoutingProfile(original.getId(),
                original.getName(), original.getSteps());
        assertEquals(original, copy);
    }

    @Test
    public void testGetters() throws Exception {
        assertGetters(randomCallableNumberRoutingProfile());
    }

    @Test
    public void testSetters() throws Exception {
        CallableNumberRoutingProfile callableNumberRoutingProfile = randomCallableNumberRoutingProfile();
        String expectedId = "new-id";
        String expectedName = "new-name";
        List<CallableNumberRoutingProfileStep> expectedSteps = ImmutableList.of(randomCallableNumberRoutingProfileStep());

        callableNumberRoutingProfile.setId(expectedId);
        callableNumberRoutingProfile.setName(expectedName);
        callableNumberRoutingProfile.setSteps(expectedSteps);

        assertEquals(expectedId, callableNumberRoutingProfile.getId());
        assertEquals(expectedName, callableNumberRoutingProfile.getName());
        assertEquals(expectedSteps, callableNumberRoutingProfile.getSteps());
    }

    @Test
    public void testToString() {
        assertToString(randomCallableNumberRoutingProfile());
    }
}
