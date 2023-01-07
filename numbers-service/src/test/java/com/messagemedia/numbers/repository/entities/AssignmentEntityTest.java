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
import static com.messagemedia.numbers.TestData.randomAssignmentEntity;

public class AssignmentEntityTest {

    @Test
    public void testAccessors() throws Exception {
        registerTestInstanceFor(OffsetDateTime.class, OffsetDateTime.now());
        assertGettersAndSetters(randomAssignmentEntity());
    }

    @Test
    public void testToString() {
        assertToString(randomAssignmentEntity());
    }

    @Test
    public void testCanonical() {
        AssignmentEntity assignment = randomAssignmentEntity();
        AssignmentEntity dupAssignment = randomAssignmentEntity();
        assignment.setId(UUID.randomUUID());
        dupAssignment.setId(assignment.getId());
        AssignmentEntity diffAssignment = randomAssignmentEntity();
        assertCanonical(assignment, dupAssignment, diffAssignment);
    }
}