/*
 * Copyright (c) Message4U Pty Ltd 2014-2020
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.entities;

import org.junit.Test;

import static com.messagemedia.framework.test.AccessorAsserter.assertGettersAndSetters;
import static com.messagemedia.framework.test.CanonicalAsserter.assertCanonical;
import static com.messagemedia.numbers.TestData.randomBillingRatePlanEntity;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;

public class BillingRatePlanEntityTest {

    @Test
    public void testAccessors() throws Exception {
        assertGettersAndSetters(randomBillingRatePlanEntity());
    }

    @Test
    public void testCanonical() {
        BillingRatePlanEntity entity = randomBillingRatePlanEntity();
        BillingRatePlanEntity duplicateEntity = randomBillingRatePlanEntity();
        duplicateEntity.setId(entity.getId());
        NumberEntity diffEntity = randomUnassignedNumberEntity();
        assertCanonical(entity, duplicateEntity, diffEntity);
    }
}
