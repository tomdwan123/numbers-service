/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import com.messagemedia.numbers.service.client.models.AssignmentAuditListResponse;
import com.messagemedia.numbers.service.client.models.AssignmentAuditSearchRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.OffsetDateTime;
import java.util.List;

import static com.messagemedia.numbers.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AuditServiceIT {

    @Autowired
    private AuditService auditService;

    @Autowired
    private NumbersService numbersService;

    @Test
    public void shouldFilterByIdWhenGetAssignmentAudits() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());
        AssignmentAuditSearchRequest assignmentAuditSearchRequest = new AssignmentAuditSearchRequest();
        assignmentAuditSearchRequest.setId(assignmentEntity.getId());
        AssignmentAuditListResponse response = auditService.getAssignmentAudits(assignmentAuditSearchRequest);

        assertEquals(1, response.getAssignments().size());
        assertDtoEquals(assignmentEntity, response.getAssignments().get(0));
        assertEquals(50, response.getPageMetadata().getPageSize());
        assertNull(response.getPageMetadata().getToken());
    }

    @Test
    public void shouldFilterByNumberIdWhenGetAssignmentAudits() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity createdAssignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());
        AssignmentEntity updatedAssignmentEntity = numbersService.updateAssignment(numberEntity.getId(), randomUpdateAssignmentRequest());
        AssignmentAuditSearchRequest assignmentAuditSearchRequest = new AssignmentAuditSearchRequest();
        assignmentAuditSearchRequest.setNumberId(numberEntity.getId());
        List<AssignmentDto> assignmentDtos = auditService.getAssignmentAudits(assignmentAuditSearchRequest).getAssignments();
        assertEquals(2, assignmentDtos.size());
        assertDtoEquals(updatedAssignmentEntity, assignmentDtos.get(0));
        assertDtoEquals(createdAssignmentEntity, assignmentDtos.get(1));
    }

    @Test
    public void shouldFilterByCreatedDateWhenGetAssignmentAudits() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());

        AssignmentAuditSearchRequest assignmentAuditSearchRequest = new AssignmentAuditSearchRequest();
        assignmentAuditSearchRequest.setNumberId(numberEntity.getId());
        assignmentAuditSearchRequest.setCreatedBefore(OffsetDateTime.now().minusSeconds(5));
        assignmentAuditSearchRequest.setCreatedBefore(OffsetDateTime.now().plusSeconds(5));

        List<AssignmentDto> assignmentDtos = auditService.getAssignmentAudits(assignmentAuditSearchRequest).getAssignments();
        assertEquals(1, assignmentDtos.size());
        assertDtoEquals(assignmentEntity, assignmentDtos.get(0));
    }

    @Test
    public void shouldFilterByDeletedDateWhenGetAssignmentAudits() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());
        AssignmentEntity assignmentEntity = numbersService.updateAssignment(numberEntity.getId(), randomUpdateAssignmentRequest());
        numbersService.disassociateAssignment(numberEntity.getId());

        AssignmentAuditSearchRequest assignmentAuditSearchRequest = new AssignmentAuditSearchRequest();
        assignmentAuditSearchRequest.setNumberId(numberEntity.getId());
        assignmentAuditSearchRequest.setDeletedAfter(OffsetDateTime.now().minusSeconds(5));
        assignmentAuditSearchRequest.setDeletedBefore(OffsetDateTime.now().plusSeconds(5));

        List<AssignmentDto> assignmentDtos = auditService.getAssignmentAudits(assignmentAuditSearchRequest).getAssignments();
        assertEquals(1, assignmentDtos.size());
        assertDtoEquals(assignmentEntity, assignmentDtos.get(0));
    }

    @Test
    public void shouldFilterByVendorAccountIdWhenGetAssignmentAudits() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());

        AssignmentAuditSearchRequest assignmentAuditSearchRequest = new AssignmentAuditSearchRequest();
        assignmentAuditSearchRequest.setVendorAccountId(new VendorAccountId(assignmentEntity.getVendorId(), assignmentEntity.getAccountId()));

        List<AssignmentDto> assignmentDtos = auditService.getAssignmentAudits(assignmentAuditSearchRequest).getAssignments();
        assertEquals(1, assignmentDtos.size());
        assertDtoEquals(assignmentEntity, assignmentDtos.get(0));
    }

    @Test
    public void shouldFilterByAllFieldsWhenGetAssignmentAudits() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());
        numbersService.disassociateAssignment(numberEntity.getId());

        AssignmentAuditSearchRequest assignmentAuditSearchRequest = new AssignmentAuditSearchRequest();
        assignmentAuditSearchRequest.setId(assignmentEntity.getId());
        assignmentAuditSearchRequest.setNumberId(numberEntity.getId());
        assignmentAuditSearchRequest.setDeletedAfter(OffsetDateTime.now().minusSeconds(5));
        assignmentAuditSearchRequest.setDeletedBefore(OffsetDateTime.now().plusSeconds(5));
        assignmentAuditSearchRequest.setCreatedBefore(OffsetDateTime.now().minusSeconds(5));
        assignmentAuditSearchRequest.setCreatedBefore(OffsetDateTime.now().plusSeconds(5));

        List<AssignmentDto> assignmentDtos = auditService.getAssignmentAudits(assignmentAuditSearchRequest).getAssignments();
        assertEquals(1, assignmentDtos.size());
        assertDtoEquals(assignmentEntity, assignmentDtos.get(0));
    }

    private void assertDtoEquals(AssignmentEntity expectedAssignmentEntity, AssignmentDto actualAssignmentDto) {
        assertEquals(expectedAssignmentEntity.getId(), actualAssignmentDto.getId());
        assertEquals(expectedAssignmentEntity.getNumberEntity().getId(), actualAssignmentDto.getNumberId());
        assertEquals(expectedAssignmentEntity.getAccountId(), actualAssignmentDto.getAccountId());
        assertEquals(expectedAssignmentEntity.getVendorId(), actualAssignmentDto.getVendorId());
        assertEquals(expectedAssignmentEntity.getCreated(), actualAssignmentDto.getCreated());
        assertEquals(expectedAssignmentEntity.getExternalMetadata(), actualAssignmentDto.getMetadata());
        assertEquals(expectedAssignmentEntity.getCallbackUrl(), actualAssignmentDto.getCallbackUrl());
    }
}
