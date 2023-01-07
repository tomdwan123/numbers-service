/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.mappers;

import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.service.client.models.AssignNumberRequest;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

import static com.messagemedia.numbers.TestData.randomAssignNumberRequest;
import static com.messagemedia.numbers.TestData.randomAssignmentEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AssignmentMapperTest {

    private AssignmentMapper assignmentMapper = Mappers.getMapper(AssignmentMapper.class);

    @Test
    public void testNulls() {
        assertNull(assignmentMapper.toAssignmentDto(null));
        assertNull(assignmentMapper.toAssignmentEntity(null));
        assertNull(assignmentMapper.toAssignmentDtoList(null));
    }

    @Test
    public void shouldMapAssignNumberRequestToAssignmentEntity() {
        AssignNumberRequest assignNumberRequest = randomAssignNumberRequest();
        AssignmentEntity assignmentEntity = assignmentMapper.toAssignmentEntity(assignNumberRequest);
        assertEquals(assignNumberRequest.getAccountId(), assignmentEntity.getAccountId());
        assertEquals(assignNumberRequest.getVendorId(), assignmentEntity.getVendorId());
        assertEquals(assignNumberRequest.getCallbackUrl(), assignmentEntity.getCallbackUrl());
        assertEquals(assignNumberRequest.getMetadata(), assignmentEntity.getExternalMetadata());
    }

    @Test
    public void shouldMapAssignmentEntityToAssignmentDto() {
        AssignmentEntity assignmentEntity = randomAssignmentEntity();
        AssignmentDto assignmentDto = assignmentMapper.toAssignmentDto(assignmentEntity);
        assertAssignmentEquals(assignmentDto, assignmentEntity);
    }

    @Test
    public void shouldMapToAssignmentDtoList() {
        List<AssignmentEntity> assignmentEntityList = new ArrayList<>();
        assignmentEntityList.add(randomAssignmentEntity());
        assignmentEntityList.add(randomAssignmentEntity());
        List<AssignmentDto> assignmentAudits = assignmentMapper.toAssignmentDtoList(assignmentEntityList);
        for (int i = 0; i < assignmentAudits.size(); i++) {
            assertAssignmentEquals(assignmentAudits.get(i), assignmentEntityList.get(i));
        }
    }

    private void assertAssignmentEquals(AssignmentDto assignmentAuditDto, AssignmentEntity assignmentEntity) {
        assertEquals(assignmentEntity.getId(), assignmentAuditDto.getId());
        assertEquals(assignmentEntity.getNumberEntity().getId(), assignmentAuditDto.getNumberId());
        assertEquals(assignmentEntity.getAccountId(), assignmentAuditDto.getAccountId());
        assertEquals(assignmentEntity.getVendorId(), assignmentAuditDto.getVendorId());
        assertEquals(assignmentEntity.getCreated(), assignmentAuditDto.getCreated());
        assertEquals(assignmentEntity.getExternalMetadata(), assignmentAuditDto.getMetadata());
        assertEquals(assignmentEntity.getCallbackUrl(), assignmentAuditDto.getCallbackUrl());
    }
}