/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;


import com.google.common.collect.Lists;
import com.messagemedia.numbers.TestData;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.time.OffsetDateTime;
import java.util.Collections;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class AssignmentVerificationServiceTest {

    @Mock
    private AuditReader auditReader;

    @Mock
    private AuditQuery auditQuery;

    @Mock
    private AuditQueryCreator auditQueryCreator;

    @InjectMocks
    private AssignmentVerificationService assignmentVerificationService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.setMaxResults(1)).thenReturn(auditQuery);
        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(AssignmentEntity.class, true, true)).thenReturn(auditQuery);
    }

    @Test
    public void testValidAssignmentWhenTheNumberAvailableToLastOwner() {
        NumberEntity numberEntity = TestData.randomAssignedNumberEntity();
        numberEntity.setAvailableAfter(OffsetDateTime.now().plusDays(1));
        final AssignmentEntity assignedTo = numberEntity.getAssignedTo();
        when(auditQuery.getResultList()).thenReturn(Lists.newArrayList(assignedTo));
        assertTrue(assignmentVerificationService.isValidForNewAssignment(numberEntity, assignedTo.getVendorId(), assignedTo.getAccountId()));
    }

    @Test
    public void testValidAssignmentWhenTheNumberAvailableForEveryOne() {
        NumberEntity numberEntity = TestData.randomAssignedNumberEntity();
        numberEntity.setAvailableAfter(OffsetDateTime.now().minusDays(1));
        final AssignmentEntity assignedTo = numberEntity.getAssignedTo();
        when(auditQuery.getResultList()).thenReturn(Collections.emptyList());
        assertTrue(assignmentVerificationService.isValidForNewAssignment(numberEntity, assignedTo.getVendorId(), assignedTo.getAccountId()));
    }

    @Test
    public void testInvalidAssignmentBecauseAlreadyAssigned() {
        NumberEntity numberEntity = TestData.randomAssignedNumberEntity();
        numberEntity.setAvailableAfter(null);
        assertFalse(assignmentVerificationService.isValidForNewAssignment(numberEntity, "anyVendor", "anyAccount"));
    }

    @Test
    public void testInvalidAssignmentBecauseNotTheLastAssignmentAccount() {
        NumberEntity numberEntity = TestData.randomAssignedNumberEntity();
        numberEntity.setAvailableAfter(OffsetDateTime.now().plusDays(1));
        final AssignmentEntity assignedTo = numberEntity.getAssignedTo();
        when(auditQuery.getResultList()).thenReturn(Lists.newArrayList(TestData.randomAssignmentEntity()));
        assertFalse(assignmentVerificationService.isValidForNewAssignment(numberEntity, assignedTo.getVendorId(), assignedTo.getAccountId()));
    }
}
