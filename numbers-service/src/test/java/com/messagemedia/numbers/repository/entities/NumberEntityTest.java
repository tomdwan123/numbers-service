/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.entities;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.messagemedia.framework.test.AccessorAsserter.assertGettersAndSetters;
import static com.messagemedia.framework.test.AccessorAsserter.registerTestInstanceFor;
import static com.messagemedia.framework.test.CanonicalAsserter.assertCanonical;
import static com.messagemedia.framework.test.CanonicalAsserter.assertToString;
import static com.messagemedia.numbers.TestData.randomAssignedNumberEntity;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;
import static org.junit.Assert.assertTrue;

public class NumberEntityTest {

    @Test
    public void testAccessors() throws Exception {
        registerTestInstanceFor(OffsetDateTime.class, OffsetDateTime.now());
        assertGettersAndSetters(randomUnassignedNumberEntity());
    }

    @Test
    public void testToString() {
        assertToString(randomAssignedNumberEntity());
    }

    @Test
    public void testCanonical() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        NumberEntity duplicateNumberEntity = randomUnassignedNumberEntity();
        numberEntity.setId(UUID.randomUUID());
        duplicateNumberEntity.setId(numberEntity.getId());
        NumberEntity diffNumberEntiy = randomUnassignedNumberEntity();
        assertCanonical(numberEntity, duplicateNumberEntity, diffNumberEntiy);
    }

    @Test
    public void testPreUpdate() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
        numberEntity.setUpdated(before);
        numberEntity.onPreUpdate();
        assertTrue(numberEntity.getUpdated().isAfter(before));
    }
}
