/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.TestData;
import com.messagemedia.numbers.exception.InvalidAccountRelationshipException;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.AssignNumberRequest;
import com.messagemedia.numbers.service.client.models.Event;
import java.util.UUID;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReassignNumbersControllerTest extends AbstractControllerTest {

    @Test
    public void shouldReassignNumber() throws Exception {
        // Given
        NumberEntity numberEntity = TestData.randomAssignedNumberEntity();
        AssignNumberRequest assignNumberRequest = TestData.randomAssignNumberRequest();
        final AssignmentEntity assignmentEntity = getAssignmentMapper().toAssignmentEntity(assignNumberRequest);
        assignmentEntity.setId(UUID.randomUUID());
        assignmentEntity.setNumberEntity(numberEntity);
        when(getNumbersService().reassignNumber(Matchers.any(UUID.class), Matchers.any(AssignmentEntity.class))).thenReturn(assignmentEntity);

        // When and Then
        getMockMvc().perform(MockMvcRequestBuilders.put(String.format("/v1/numbers/%s/assignment", numberEntity.getId().toString()))
                .content(toJson(assignNumberRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numberId", is(numberEntity.getId().toString())))
            .andExpect(jsonPath("$.vendorId", is(assignmentEntity.getVendorId())))
            .andExpect(jsonPath("$.accountId", is(assignmentEntity.getAccountId())))
            .andExpect(jsonPath("$.callbackUrl", is(assignmentEntity.getCallbackUrl())))
            .andExpect(jsonPath("$.metadata", is(assignmentEntity.getExternalMetadata())))
            .andExpect(jsonPath("$.created", is(assignmentEntity.getCreated())));
        verify(getNumbersService()).reassignNumber(any(), any());
        verify(getNotificationService()).push(any(Event.class), any());
    }

    @Test
    public void shouldReassignNumberFailed() throws Exception {
        // Given
        NumberEntity numberEntity = TestData.randomAssignedNumberEntity();
        AssignNumberRequest assignNumberRequest = TestData.randomAssignNumberRequest();
        final AssignmentEntity assignmentEntity = getAssignmentMapper().toAssignmentEntity(assignNumberRequest);
        assignmentEntity.setId(UUID.randomUUID());
        assignmentEntity.setNumberEntity(numberEntity);
        when(getNumbersService().reassignNumber(Matchers.any(UUID.class), Matchers.any(AssignmentEntity.class))).thenThrow(new
            InvalidAccountRelationshipException(new VendorAccountId("test", "test")));

        // When and Then
        getMockMvc().perform(MockMvcRequestBuilders.put(String.format("/v1/numbers/%s/assignment", numberEntity.getId().toString()))
            .content(toJson(assignNumberRequest))
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().isConflict());
        verify(getNumbersService()).reassignNumber(any(), any());
        verifyZeroInteractions(getNotificationService());
    }

}
