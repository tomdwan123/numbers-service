/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.controller;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.exception.*;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.NumberListResult;
import com.messagemedia.numbers.service.client.models.*;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hibernate.HibernateException;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;
import static com.messagemedia.numbers.repository.entities.builders.AssignmentEntityBuilder.anAssignmentEntity;
import static com.messagemedia.numbers.repository.entities.builders.NumberEntityBuilder.aNumberEntity;
import static java.lang.Boolean.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NumbersControllerTest extends AbstractControllerTest {

    @DataProvider
    public static Object[] providePageSize() {
        return new Object[] {2, 15, 59};
    }

    @Test
    @UseDataProvider("providePageSize")
    public void shouldGetNumberListOk(int pageSize) throws Exception {
        List<NumberEntity> numbers = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            numbers.add(randomUnassignedNumberEntity());
        }
        UUID token = UUID.randomUUID();
        NumberListResult numberListResult = new NumberListResult(numbers, token);
        when(getNumbersService().getNumbers(any(NumberSearchRequest.class))).thenReturn(numberListResult);

        checkNumberListWithPageSize(pageSize, numbers, token);
    }

    private void checkNumberListWithPageSize(int pageSize, List<NumberEntity> numbers, UUID token) throws Exception {
        ResultActions resultActions = getMockMvc().perform(MockMvcRequestBuilders.get(NUMBERS_SERVICE_URL)
                .param("pageSize", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numbers", hasSize(pageSize)))
                .andExpect(jsonPath("$.pageMetadata.pageSize", is(pageSize)))
                .andExpect(jsonPath("$.pageMetadata.token", is(token.toString())));

        for (int i = 0; i < pageSize; i++) {
            checkNumberInArrayResult(resultActions, i, numbers.get(i));
        }
    }

    private void checkNumberInArrayResult(ResultActions resultActions, int index, NumberEntity numberEntity) throws Exception {
        String prefix = String.format("$.numbers[%d]", index);
        checkNumber(resultActions, numberEntity, prefix);
        if (numberEntity.getAssignedTo() == null) {
            resultActions.andExpect(jsonPath(prefix + ".assignedTo", nullValue()));
        } else {
            checkAssignment(resultActions, numberEntity.getAssignedTo(), prefix + ".assignedTo");
        }
    }

    protected void checkAssignment(ResultActions resultActions, AssignmentEntity assignmentEntity, String prefix) throws Exception {
        resultActions.andExpect(jsonPath(prefix, notNullValue()));
        resultActions.andExpect(jsonPath(prefix + ".id", is(assignmentEntity.getId().toString())));
        resultActions.andExpect(jsonPath(prefix + ".accountId", is(assignmentEntity.getAccountId())));
        resultActions.andExpect(jsonPath(prefix + ".callbackUrl", is(assignmentEntity.getCallbackUrl())));
        resultActions.andExpect(jsonPath(prefix + ".metadata", is(assignmentEntity.getExternalMetadata())));
        resultActions.andExpect(jsonPath(prefix + ".vendorId", is(assignmentEntity.getVendorId())));
    }

    protected void checkNumber(ResultActions resultActions, NumberEntity numberEntity, String prefix) throws Exception {
        String[] caps = numberEntity.getCapabilities().stream().map(Enum::name).toArray(String[]::new);
        resultActions.andExpect(jsonPath(prefix + ".id", is(numberEntity.getId().toString())));
        resultActions.andExpect(jsonPath(prefix + ".country", is(numberEntity.getCountry())));
        resultActions.andExpect(jsonPath(prefix + ".phoneNumber", is(numberEntity.getPhoneNumber())));
        resultActions.andExpect(jsonPath(prefix + ".type", is(numberEntity.getType().toString())));
        resultActions.andExpect(jsonPath(prefix + ".providerId", is(numberEntity.getProviderId().toString())));
        resultActions.andExpect(jsonPath(prefix + ".classification", is(numberEntity.getClassification().toString())));
        resultActions.andExpect(jsonPath(prefix + ".capabilities", hasItems(caps)));
    }

    @DataProvider
    public static Object[][] provideBadRequest() {
        return new Object[][]{
                {"token", "invalid token"},
                {"pageSize", "not number"},
                {"serviceTypes", "WRONG_TYPE"},
                {"assigned", "wrong"},
                {"availableBy", "wrong-date"}
        };
    }

    @Test
    @UseDataProvider("provideBadRequest")
    public void shouldGetNumberListFailBadRequest(String name, String value) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders
                .get(NUMBERS_SERVICE_URL)
                .param(name, value)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    public void shouldReturnEmptyNumberList() throws Exception {
        when(getNumbersService().getNumbers(any(NumberSearchRequest.class)))
                .thenReturn(new NumberListResult(Collections.emptyList(), null));

        getMockMvc().perform(MockMvcRequestBuilders
                .get(NUMBERS_SERVICE_URL)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.numbers").isEmpty());
        verify(getNumbersService()).getNumbers(any(NumberSearchRequest.class));
    }

    @DataProvider
    public static Object[][] registerNumberSuccessData() {
        return new Object[][]{
                {"+12", "AU", NumberType.MOBILE, Classification.BRONZE, Sets.newSet(ServiceType.SMS), TRUE},
                {"+123", "VN", NumberType.LANDLINE, Classification.GOLD, Sets.newSet(ServiceType.MMS, ServiceType.SMS), FALSE},
                {"+1234567890", "US", NumberType.SHORT_CODE, Classification.SILVER, Sets.newSet(ServiceType.values()), null},
                {"+123456789012340", "GB", NumberType.TOLL_FREE, Classification.BRONZE, Sets.newSet(ServiceType.TTS, ServiceType.SMS), FALSE}
        };
    }

    @Test
    @UseDataProvider("registerNumberSuccessData")
    public void shouldRegisterNumberOk(String phoneNumber, String countryCode, NumberType numberType,
                                       Classification classification,
                                       Set<ServiceType> capabilities,
                                       Boolean dedicatedReceiver) throws Exception {
        RegisterNumberRequest numberRequest = new RegisterNumberRequest(phoneNumber, UUID.randomUUID(), countryCode,
                numberType, classification, capabilities, dedicatedReceiver);

        OffsetDateTime now = OffsetDateTime.now();
        NumberEntity numberEntity = aNumberEntity()
                .withProviderId(numberRequest.getProviderId())
                .withCountry(numberRequest.getCountry())
                .withPhoneNumber(numberRequest.getPhoneNumber())
                .withType(numberRequest.getType())
                .withClassification(numberRequest.getClassification())
                .withCapabilities(numberRequest.getCapabilities())
                .withDedicatedReceiver(numberRequest.getDedicatedReceiver())
                .build();
        numberEntity.setId(UUID.randomUUID());
        numberEntity.setCreated(now);
        numberEntity.setUpdated(now);
        numberEntity.setAvailableAfter(now);

        when(getNumbersService().registerNumber(any(NumberEntity.class)))
                .thenReturn(numberEntity);

        getMockMvc().perform(MockMvcRequestBuilders.post(NUMBERS_SERVICE_URL)
                .content(toJson(numberRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.providerId").value(numberRequest.getProviderId().toString()))
                .andExpect(jsonPath("$.country").value(countryCode))
                .andExpect(jsonPath("$.type").value(numberType.name()))
                .andExpect(jsonPath("$.classification").value(classification.name()))
                .andExpect(jsonPath("$.created").isNotEmpty())
                .andExpect(jsonPath("$.updated").isNotEmpty())
                .andExpect(jsonPath("$.availableAfter").isNotEmpty())
                .andExpect(jsonPath("$.capabilities").value(hasSize(capabilities.size())))
                .andExpect(jsonPath("$.dedicatedReceiver").value(Boolean.TRUE.equals(numberRequest.getDedicatedReceiver())));

        verify(getNumbersService()).registerNumber(any(NumberEntity.class));
    }

    @DataProvider
    public static Object[][] exceptionData() {
        return new Object[][]{
                {new RuntimeException("test exception"), HttpStatus.INTERNAL_SERVER_ERROR},
                {new EventNotificationPublishingFailedException("test", new IOException("")), HttpStatus.INTERNAL_SERVER_ERROR},
                {new DataIntegrityViolationException("spring exception",
                        new HibernateException("hibernate exception", new HibernateException("constraint exception"))), HttpStatus.CONFLICT}
        };
    }

    @Test
    @UseDataProvider("exceptionData")
    public void shouldRegisterNumberFail(Exception ex, HttpStatus httpStatus) throws Exception {
        doThrow(ex).when(getNumbersService()).registerNumber(any(NumberEntity.class));

        getMockMvc().perform(MockMvcRequestBuilders.post(NUMBERS_SERVICE_URL)
                .content(toJson(randomRegisterNumberRequest()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(getNumbersService()).registerNumber(any(NumberEntity.class));
    }

    @Test
    public void shouldAssignNumberOk() throws Exception {
        AssignNumberRequest assignNumberRequest = randomAssignNumberRequest();

        AssignmentEntity assignmentEntity = anAssignmentEntity()
                .withAccountId(assignNumberRequest.getAccountId())
                .withVendorId(assignNumberRequest.getVendorId())
                .withNumberEntity(randomUnassignedNumberEntity())
                .withCallbackUrl(assignNumberRequest.getCallbackUrl())
                .withExternalMetadata(assignNumberRequest.getMetadata())
                .build();

        assignmentEntity.setCreated(OffsetDateTime.now());
        NumberEntity numberEntity = assignmentEntity.getNumberEntity();

        when(getNumbersService().assignNumberToAccount(eq(numberEntity.getId()), any(AssignmentEntity.class))).thenReturn(assignmentEntity);
        doNothing().when(getNotificationService()).push(any(Event.class), any());
        NumberEntity assignedNumber = randomAssignedNumberEntity();
        when(getNumbersService().getNumber(any(UUID.class))).thenReturn(assignedNumber);
        getMockMvc().perform(MockMvcRequestBuilders
                .post(String.format(ASSIGNMENT_URL_FORMAT, numberEntity.getId()))
                .content(toJson(assignNumberRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(assignmentEntity.getId().toString()))
                .andExpect(jsonPath("$.numberId").value(numberEntity.getId().toString()))
                .andExpect(jsonPath("$.vendorId").value(assignNumberRequest.getVendorId()))
                .andExpect(jsonPath("$.accountId").value(assignNumberRequest.getAccountId()))
                .andExpect(jsonPath("$.callbackUrl").value(assignNumberRequest.getCallbackUrl()))
                .andExpect(jsonPath("$.created").isNotEmpty());

        verify(getNumbersService()).assignNumberToAccount(eq(numberEntity.getId()), any(AssignmentEntity.class));
        verify(getNotificationService()).push(any(Event.class), any());
        verify(getBillingNotificationService()).sendAddNotification(new VendorAccountId(assignNumberRequest.getVendorId(),
                assignNumberRequest.getAccountId()), assignedNumber);
    }

    @DataProvider
    public static Object[][] assignNumberExceptionData() {
        return new Object[][]{
                {new RuntimeException("test exception"), HttpStatus.INTERNAL_SERVER_ERROR},
                {new NumberAlreadyAssignedException(UUID.randomUUID()), HttpStatus.CONFLICT},
                {new NumberNotAvailableException(UUID.randomUUID()), HttpStatus.CONFLICT},
                {new NumberNotFoundException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
        };
    }

    @Test
    @UseDataProvider("assignNumberExceptionData")
    public void shouldAssignNumberFail(Exception ex, HttpStatus httpStatus) throws Exception {
        doThrow(ex)
                .when(getNumbersService())
                .assignNumberToAccount(any(UUID.class), any(AssignmentEntity.class));

        getMockMvc().perform(MockMvcRequestBuilders
                .post(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .content(toJson(randomAssignNumberRequest()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(httpStatus.value()));

        verifyNoMoreInteractions(getNotificationService());
    }

    @Test
    public void shouldLoadAssignmentSuccess() throws Exception {
        AssignmentEntity assignmentEntity = randomAssignmentEntity();
        assignmentEntity.setCreated(OffsetDateTime.now());

        when(getNumbersService().loadAssignmentDetailsByNumberId(eq(assignmentEntity.getNumberEntity().getId())))
                .thenReturn(assignmentEntity);

        getMockMvc().perform(MockMvcRequestBuilders
                .get(String.format(ASSIGNMENT_URL_FORMAT, assignmentEntity.getNumberEntity().getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignmentEntity.getId().toString()))
                .andExpect(jsonPath("$.numberId").value(assignmentEntity.getNumberEntity().getId().toString()))
                .andExpect(jsonPath("$.vendorId").value(assignmentEntity.getVendorId()))
                .andExpect(jsonPath("$.accountId").value(assignmentEntity.getAccountId()))
                .andExpect(jsonPath("$.callbackUrl").value(assignmentEntity.getCallbackUrl()))
                .andExpect(jsonPath("$.created").isNotEmpty())
                .andExpect(jsonPath("$.metadata").value(assignmentEntity.getExternalMetadata()));

        verify(getNumbersService()).loadAssignmentDetailsByNumberId(eq(assignmentEntity.getNumberEntity().getId()));
    }

    @DataProvider
    public static Object[][] loadAssignmentDetailsExceptionData() {
        return new Object[][]{
                {new RuntimeException("test exception"), HttpStatus.INTERNAL_SERVER_ERROR},
                {new NumberNotFoundException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
                {new NumberNotAssignedException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
        };
    }

    @Test
    @UseDataProvider("loadAssignmentDetailsExceptionData")
    public void shouldLoadAssignmentDetailsFail(Exception ex, HttpStatus httpStatus) throws Exception {
        doThrow(ex).when(getNumbersService())
                .loadAssignmentDetailsByNumberId(any(UUID.class));

        getMockMvc().perform(MockMvcRequestBuilders
                .get(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(getNumbersService()).loadAssignmentDetailsByNumberId(any(UUID.class));
    }

    @Test
    public void shouldGetUnassignedNumberOk() throws Exception {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        when(getNumbersService().getNumber(eq(numberEntity.getId()))).thenReturn(numberEntity);

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders
                .get(String.format(GET_NUMBER_URL_FORMAT, numberEntity.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );

        result.andExpect(status().isOk());
        expectNumberEntityFields(result, numberEntity)
                .andExpect(jsonPath("$.assignedTo").doesNotExist());
    }

    @Test
    public void shouldGetAssignedNumberOk() throws Exception {
        NumberEntity numberEntity = randomAssignedNumberEntity();
        when(getNumbersService().getNumber(eq(numberEntity.getId()))).thenReturn(numberEntity);

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders
                .get(String.format(GET_NUMBER_URL_FORMAT, numberEntity.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );

        result.andExpect(status().isOk());
        expectNumberEntityFields(result, numberEntity)
                .andExpect(jsonPath("$.assignedTo.id").value(numberEntity.getAssignedTo().getId().toString()))
                .andExpect(jsonPath("$.assignedTo.vendorId").value(numberEntity.getAssignedTo().getVendorId()))
                .andExpect(jsonPath("$.assignedTo.accountId").value(numberEntity.getAssignedTo().getAccountId()))
                .andExpect(jsonPath("$.assignedTo.callbackUrl").value(numberEntity.getAssignedTo().getCallbackUrl()))
                .andExpect(jsonPath("$.assignedTo.created").value(numberEntity.getAssignedTo().getCreated()))
                .andExpect(jsonPath("$.assignedTo.metadata").value(numberEntity.getAssignedTo().getExternalMetadata()));
    }

    @Test
    public void shouldGetNumberNotFound() throws Exception {
        UUID numberId = UUID.randomUUID();
        doThrow(new NumberNotFoundException(numberId))
                .when(getNumbersService())
                .getNumber(eq(numberId));

        getMockMvc().perform(MockMvcRequestBuilders
                .get(String.format(GET_NUMBER_URL_FORMAT, numberId))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWhenGetNumberWithInvalidId() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders
                .get(String.format(GET_NUMBER_URL_FORMAT, "invalidId"))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateUnassignedNumberSuccess() throws Exception {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        when(getNumbersService().updateNumber(any(UUID.class), any(UpdateNumberRequest.class))).thenReturn(numberEntity);

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberEntity.getId()))
                .content(toJson(randomUpdateNumberRequestWithAvailableAfter()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );

        result.andExpect(status().isOk());
        expectNumberEntityFields(result, numberEntity).andExpect(jsonPath("$.assignedTo").doesNotExist());
        verify(getNotificationService()).push(any(Event.class), any());
        verifyNoMoreInteractions(getNotificationService());
    }

    @Test
    public void shouldUpdateAssignedNumberSuccess() throws Exception {
        NumberEntity numberEntity = randomAssignedNumberEntity();
        when(getNumbersService().updateNumber(any(UUID.class), any(UpdateNumberRequest.class))).thenReturn(numberEntity);

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberEntity.getId()))
                .content(toJson(randomUpdateNumberRequestWithAvailableAfter()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );

        result.andExpect(status().isOk());
        expectNumberEntityFields(result, numberEntity)
                .andExpect(jsonPath("$.assignedTo.id").value(numberEntity.getAssignedTo().getId().toString()))
                .andExpect(jsonPath("$.assignedTo.vendorId").value(numberEntity.getAssignedTo().getVendorId()))
                .andExpect(jsonPath("$.assignedTo.accountId").value(numberEntity.getAssignedTo().getAccountId()))
                .andExpect(jsonPath("$.assignedTo.callbackUrl").value(numberEntity.getAssignedTo().getCallbackUrl()))
                .andExpect(jsonPath("$.assignedTo.created").value(numberEntity.getAssignedTo().getCreated()))
                .andExpect(jsonPath("$.assignedTo.metadata").value(numberEntity.getAssignedTo().getExternalMetadata()));
        verify(getNotificationService()).push(any(Event.class), any());
    }

    @Test
    public void shouldReturnBadRequestWhenUpdateNumberWithInvalidId() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", "invalid-uuid"))
                .content(toJson(randomUpdateNumberRequestWithAvailableAfter()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest());
    }

    @DataProvider
    public static Object[][] updateNumberExceptionData() {
        return new Object[][]{
                {new RuntimeException("test exception"), HttpStatus.INTERNAL_SERVER_ERROR},
                {new NumberNotFoundException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
                {new NumberAvailableAfterUpdateException(UUID.randomUUID()), HttpStatus.CONFLICT},
                {new NumberUpdateRequestEmptyException("test exception"), HttpStatus.BAD_REQUEST},
                {new NotUsTollFreeNumberException(UUID.randomUUID()), HttpStatus.BAD_REQUEST},
                {new TollFreeNumberUpdateStatusException(UUID.randomUUID(), randomStatus()), HttpStatus.BAD_REQUEST}
        };
    }

    @Test
    @UseDataProvider("updateNumberExceptionData")
    public void shouldUpdateNumberFail(Exception ex, HttpStatus httpStatus) throws Exception {
        doThrow(ex).when(getNumbersService()).updateNumber(any(UUID.class), any(UpdateNumberRequest.class));

        getMockMvc().perform(MockMvcRequestBuilders
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", UUID.randomUUID()))
                .content(toJson(randomUpdateNumberRequestWithAvailableAfter()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoMoreInteractions(getNotificationService());
    }

    @Test
    public void shouldDeleteNumberSuccess() throws Exception {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        when(getNumbersService().deleteNumber(any(UUID.class))).thenReturn(numberEntity);
        doNothing().when(getNotificationService()).push(any(Event.class), any());

        getMockMvc().perform(MockMvcRequestBuilders
                .delete(NUMBERS_SERVICE_URL + "/" + numberEntity.getId()))
                .andExpect(status().isNoContent());

        verify(getNumbersService()).deleteNumber(eq(numberEntity.getId()));
    }

    @DataProvider
    public static Object[][] deleteNumberExceptionData() {
        return new Object[][]{
                {new NumberNotFoundException(UUID.randomUUID()), HttpStatus.NOT_FOUND},
                {new DeleteAssignedNumberException(UUID.randomUUID()), HttpStatus.CONFLICT},
        };
    }

    @Test
    @UseDataProvider("deleteNumberExceptionData")
    public void shouldDeleteNumberFail(Exception ex, HttpStatus httpStatus) throws Exception {
        doThrow(ex).when(getNumbersService()).deleteNumber(any(UUID.class));

        getMockMvc().perform(MockMvcRequestBuilders
                .delete(NUMBERS_SERVICE_URL + "/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(getNumbersService()).deleteNumber(any(UUID.class));
    }

    @Test
    public void shouldReturnBadRequestWhenDeleteNumberWithInvalidId() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders
                .delete(NUMBERS_SERVICE_URL + "/" + "invalid-id")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest());
    }
}
