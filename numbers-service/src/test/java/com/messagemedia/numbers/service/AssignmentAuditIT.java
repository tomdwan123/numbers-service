/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.UpdateAssignmentRequest;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import java.util.List;

import static com.messagemedia.numbers.TestData.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AssignmentAuditIT {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    @Autowired
    private NumbersService numbersService;

    @Test
    public void shouldSaveAssignmentAuditCorrectly() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());

        AuditReader reader = AuditReaderFactory.get(entityManager);
        AuditQuery auditQuery = reader.createQuery().forRevisionsOfEntity(AssignmentEntity.class, true, true);
        auditQuery.add(AuditEntity.id().eq(assignmentEntity.getId()));
        List<AssignmentEntity> auditAssignments = auditQuery.getResultList();
        assertThat(auditAssignments.size(), is(1));

        UpdateAssignmentRequest updateAssignmentRequest = randomUpdateAssignmentRequest();
        numbersService.updateAssignment(numberEntity.getId(), updateAssignmentRequest);
        auditAssignments = auditQuery.getResultList();
        assertThat(auditAssignments.size(), is(2));
        assertThat(updateAssignmentRequest.getCallbackUrlValue(), is(auditAssignments.get(1).getCallbackUrl()));
        assertThat(updateAssignmentRequest.getMetadataValue(), is(auditAssignments.get(1).getExternalMetadata()));
        assertThat(updateAssignmentRequest.getLabelValue(), is(auditAssignments.get(1).getLabel()));

        numbersService.disassociateAssignment(numberEntity.getId());
        auditAssignments = auditQuery.getResultList();
        assertThat(auditAssignments.size(), is(3));
        assertThat(updateAssignmentRequest.getCallbackUrlValue(), is(auditAssignments.get(2).getCallbackUrl()));
        assertThat(updateAssignmentRequest.getMetadataValue(), is(auditAssignments.get(2).getExternalMetadata()));
    }
}
