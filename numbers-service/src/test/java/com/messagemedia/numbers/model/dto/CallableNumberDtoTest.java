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
import static com.messagemedia.numbers.TestData.randomCallableNumberDto;
import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfile;
import static org.junit.Assert.assertEquals;

public class CallableNumberDtoTest {

    @Test
    public void testConstructor() {
        CallableNumberDto original = randomCallableNumberDto();
        CallableNumberDto copy = new CallableNumberDto(original.getId(),
                original.getName(), original.getNumber(), original.getRoutingProfile());
        assertEquals(original, copy);
    }

    @Test
    public void testGetters() throws Exception {
        assertGetters(randomCallableNumberDto());
    }

    @Test
    public void testSetters() throws Exception {
        CallableNumberDto callableNumberDto = randomCallableNumberDto();
        CallableNumberRoutingProfile expectedCallableNumberRoutingProfile = randomCallableNumberRoutingProfile();
        String expectedId = "new-id";
        String expectedName = "new-name";
        String expectedNumber = "1234567890";
        callableNumberDto.setId(expectedId);
        callableNumberDto.setName(expectedName);
        callableNumberDto.setNumber(expectedNumber);
        callableNumberDto.setRoutingProfile(expectedCallableNumberRoutingProfile);
        assertEquals(expectedId, callableNumberDto.getId());
        assertEquals(expectedName, callableNumberDto.getName());
        assertEquals(expectedNumber, callableNumberDto.getNumber());
        assertEquals(expectedCallableNumberRoutingProfile, callableNumberDto.getRoutingProfile());
    }

    @Test
    public void testToString() {
        assertToString(randomCallableNumberDto());
    }
}
