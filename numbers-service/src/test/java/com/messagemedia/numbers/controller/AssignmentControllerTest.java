/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.exception.AssignmentUpdateRequestEmptyException;
import com.messagemedia.numbers.exception.NumberNotAssignedException;
import com.messagemedia.numbers.exception.NumberNotFoundException;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.Event;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.messagemedia.numbers.service.client.models.UpdateAssignmentRequest;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static com.messagemedia.numbers.TestData.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AssignmentControllerTest extends AbstractControllerTest {

    @Test
    public void shouldDisassociateAssignmentSuccess() throws Exception {
        // Given
        UUID numberId = UUID.randomUUID();
        AssignmentEntity assignmentEntity = randomAssignmentEntity();
        NumberEntity numberEntity = randomAssignedNumberEntity();
        when(getNumbersService().disassociateAssignment(any(UUID.class))).thenReturn(assignmentEntity);
        when(getNumbersService().getNumber(any(UUID.class))).thenReturn(numberEntity);
        doNothing().when(getNotificationService()).push(any(Event.class), any());

        // When
        getMockMvc().perform(MockMvcRequestBuilders
                .delete(String.format(ASSIGNMENT_URL_FORMAT, numberId)))
                .andExpect(status().isNoContent());

        // Then
        verify(getNumbersService()).disassociateAssignment(eq(numberId));
        verify(getNotificationService()).push(any(Event.class), any());
        verify(getBillingNotificationService()).sendRemoveNotification(new VendorAccountId(assignmentEntity.getVendorId(),
                assignmentEntity.getAccountId()), numberEntity);
    }

    @DataProvider
    public static Object[][] disassociateAssignmentExceptionData() {
        return new Object[][]{
                {new NumberNotFoundException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
                {new NumberNotAssignedException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
        };
    }

    @Test
    @UseDataProvider("disassociateAssignmentExceptionData")
    public void shouldDisassociateAssignmentFail(Exception ex, HttpStatus httpStatus) throws Exception {
        // Given
        doThrow(ex)
                .when(getNumbersService())
                .disassociateAssignment(any(UUID.class));

        // When
        getMockMvc().perform(MockMvcRequestBuilders
                .delete(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Then
        verify(getNumbersService()).disassociateAssignment(any(UUID.class));
        verifyZeroInteractions(getNotificationService());
        verifyZeroInteractions(getBillingNotificationService());
    }

    @Test
    public void shouldReturnBadRequestWhenDisassociateAssignmentWithInvalidNumberId() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders
                .delete(String.format(ASSIGNMENT_URL_FORMAT, "invalidId"))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateAssignmentSuccess() throws Exception {
        AssignmentEntity assignmentEntity = randomAssignmentEntity();
        UpdateAssignmentRequest assignmentRequest = randomUpdateAssignmentRequest();
        when(getNumbersService().updateAssignment(any(UUID.class), any(UpdateAssignmentRequest.class))).thenReturn(assignmentEntity);
        when(getNumbersService().getNumber(any(UUID.class))).thenReturn(randomAssignedNumberEntity());
        doNothing().when(getNotificationService()).push(any(Event.class), any());
        getMockMvc().perform(MockMvcRequestBuilders
                .patch(String.format(ASSIGNMENT_URL_FORMAT, assignmentEntity.getNumberEntity().getId().toString()))
                .content(toJson(assignmentRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callbackUrl").value(assignmentEntity.getCallbackUrl()))
                .andExpect(jsonPath("$.metadata").value(assignmentEntity.getExternalMetadata()));
        verify(getNotificationService()).push(any(Event.class), any());
    }

    @DataProvider
    public static Object[][] updateAssignmentExceptionData() {
        return new Object[][] {
                {new RuntimeException("test exception"), HttpStatus.INTERNAL_SERVER_ERROR},
                {new NumberNotFoundException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
                {new NumberNotAssignedException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
                {new AssignmentUpdateRequestEmptyException("test exception"), HttpStatus.BAD_REQUEST}
        };
    }

    @Test
    @UseDataProvider("updateAssignmentExceptionData")
    public void shouldUpdateAssignmentFail(Exception ex, HttpStatus httpStatus) throws Exception {
        doThrow(ex)
                .when(getNumbersService())
                .updateAssignment(any(UUID.class), any(UpdateAssignmentRequest.class));

        getMockMvc().perform(MockMvcRequestBuilders
                .patch(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .content(toJson(randomUpdateAssignmentRequest()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(getNumbersService()).updateAssignment(any(UUID.class), any(UpdateAssignmentRequest.class));
        verifyZeroInteractions(getNotificationService());
    }

    @Test
    public void shouldReturnBadRequestWhenUpdateAssignmentWithInvalidNumberId() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders
                .patch(String.format(ASSIGNMENT_URL_FORMAT, "invalidId"))
                .content(toJson(randomUpdateAssignmentRequest()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldDeleteCallableNumberWhenDisassociateAssignment() throws Exception {
        NumberEntity assignedNumber = randomAssignedNumberEntity();
        assignedNumber.getCapabilities().add(ServiceType.CALL);

        when(getNumbersService().disassociateAssignment(any(UUID.class))).thenReturn(assignedNumber.getAssignedTo());
        when(getNumbersService().getNumber(any(UUID.class))).thenReturn(assignedNumber);
        doNothing().when(getCallableNumbersService()).deleteCallableNumber(any(UUID.class));
        getMockMvc().perform(MockMvcRequestBuilders
                .delete(String.format(ASSIGNMENT_URL_FORMAT, assignedNumber.getId().toString())))
                .andExpect(status().isNoContent());
        verify(getCallableNumbersService()).deleteCallableNumber(any(UUID.class));
    }
}
