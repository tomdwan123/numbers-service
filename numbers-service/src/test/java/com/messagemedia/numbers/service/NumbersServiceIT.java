/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.framework.jackson.core.valuewithnull.ValueWithNull;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.exception.DeleteAssignedNumberException;
import com.messagemedia.numbers.exception.NotUsTollFreeNumberException;
import com.messagemedia.numbers.exception.NumberAvailableAfterUpdateException;
import com.messagemedia.numbers.exception.NumberNotFoundException;
import com.messagemedia.numbers.exception.NumberUpdateRequestEmptyException;
import com.messagemedia.numbers.exception.TollFreeNumberUpdateStatusException;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberSearchRequest;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.messagemedia.numbers.service.client.models.UpdateNumberRequest;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static com.messagemedia.numbers.TestData.mockUpAmsEndpoint;
import static com.messagemedia.numbers.TestData.randomAssignmentEntityWithoutNumberEntity;
import static com.messagemedia.numbers.TestData.randomCapabilities;
import static com.messagemedia.numbers.TestData.randomDedicatedReceiver;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntityWithCapabilities;
import static com.messagemedia.numbers.TestData.randomUpdateNumberRequestWithAvailableAfter;
import static com.messagemedia.numbers.TestData.randomClassification;
import static com.messagemedia.numbers.TestData.randomUpdateNumberRequestWithoutAvailableAfter;
import static com.messagemedia.numbers.TestData.randomUpdateNumberRequestWithOnlyStatus;
import static com.messagemedia.numbers.TestData.unassignedUsTollFreeNumberEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(DataProviderSpringRunner.class)
public class NumbersServiceIT {

    private static final int AMS_API_PORT = 12345;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(AMS_API_PORT);

    @Autowired
    private NumbersService numbersService;

    @Autowired
    private NumbersRepository numbersRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Before
    public void setup() throws Exception {
        this.clean();
        mockUpAmsEndpoint();
    }

    @After
    public void clean() throws Exception {
        assignmentRepository.deleteAllInBatch();
        numbersRepository.deleteAllInBatch();
    }

    @Test
    @Transactional
    public void shouldRegisterNumber() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        NumberEntity dbEntity = numbersService.registerNumber(numberEntity);
        assertNotNull(dbEntity);
        assertEquals(numberEntity, dbEntity);
        assertEquals(numberEntity.getId(), dbEntity.getId());
        assertEquals(numberEntity.getCountry(), dbEntity.getCountry());
        assertEquals(numberEntity.getPhoneNumber(), dbEntity.getPhoneNumber());
        assertEquals(numberEntity.getProviderId(), dbEntity.getProviderId());
        assertEquals(numberEntity.getType(), dbEntity.getType());
        assertEquals(numberEntity.getCapabilities(), dbEntity.getCapabilities());
        assertNotNull(dbEntity.getCreated());
        assertNotNull(dbEntity.getUpdated());
        assertNotNull(dbEntity.getAvailableAfter());
    }

    @Test
    @Transactional
    public void shouldGetNumberWithExactCapability() {
        Set<ServiceType> caps1 = new HashSet<>();
        caps1.add(ServiceType.SMS);
        NumberEntity numberEntity1 = randomUnassignedNumberEntityWithCapabilities(caps1);
        numbersService.registerNumber(numberEntity1);

        Set<ServiceType> caps2 = new HashSet<>();
        caps2.add(ServiceType.MMS);
        NumberEntity numberEntity2 = randomUnassignedNumberEntityWithCapabilities(caps2);
        numbersService.registerNumber(numberEntity2);

        NumberSearchRequest request = new NumberSearchRequest();
        request.setExactServiceTypes(Boolean.TRUE);
        ServiceType[] capsForSearch = new ServiceType[1];
        capsForSearch[0] = ServiceType.SMS;

        request.setServiceTypes(capsForSearch);

        NumberListResult resultList = numbersService.getNumbers(request);
        assertNotNull(resultList);
        assertTrue(resultList.getNumbers().size() > 0);
        assertEquals(1, resultList.getNumbers().get(0).getCapabilities().size());
        assertTrue(resultList.getNumbers().get(0).getCapabilities().contains(ServiceType.SMS));
    }

    @Test
    @Transactional
    public void shouldGetNumberWithoutExactCapability() {
        Set<ServiceType> caps1 = new HashSet<>();
        caps1.add(ServiceType.SMS);
        NumberEntity numberEntity1 = randomUnassignedNumberEntityWithCapabilities(caps1);
        numbersService.registerNumber(numberEntity1);

        Set<ServiceType> caps2 = new HashSet<>();
        caps2.add(ServiceType.MMS);
        caps2.add(ServiceType.SMS);
        NumberEntity numberEntity2 = randomUnassignedNumberEntityWithCapabilities(caps2);
        numbersService.registerNumber(numberEntity2);

        NumberSearchRequest request = new NumberSearchRequest();
        request.setExactServiceTypes(Boolean.FALSE);
        ServiceType[] capsForSearch = new ServiceType[1];
        capsForSearch[0] = ServiceType.SMS;

        request.setServiceTypes(capsForSearch);

        NumberListResult resultList = numbersService.getNumbers(request);
        assertNotNull(resultList);
        assertTrue(resultList.getNumbers().size() > 1);

        resultList.getNumbers().forEach(n -> {
            assertTrue(n.getCapabilities().contains(ServiceType.SMS) || n.getCapabilities().contains(ServiceType.MMS));
        });
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void shouldFailWhenRegisterDuplicateNumber() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        NumberEntity otherEntity = randomUnassignedNumberEntity();
        otherEntity.setPhoneNumber(numberEntity.getPhoneNumber());
        numbersService.registerNumber(numberEntity);
        numbersService.registerNumber(otherEntity);
    }

    @DataProvider
    public static Object[][] updateNumberFullSuccessData() {
        return new Object[][]{
                {randomUnassignedNumberEntity(), null, randomUpdateNumberRequestWithAvailableAfter()},
                {randomUnassignedNumberEntity(), randomAssignmentEntityWithoutNumberEntity(), randomUpdateNumberRequestWithoutAvailableAfter()}
        };
    }

    @Test
    @UseDataProvider("updateNumberFullSuccessData")
    public void shouldUpdateNumberFullSuccess(NumberEntity numberEntity, AssignmentEntity assignmentEntity, UpdateNumberRequest numberRequest) {
        NumberEntity registeredNumber = numbersService.registerNumber(numberEntity);
        if (assignmentEntity != null) {
            numbersService.assignNumberToAccount(registeredNumber.getId(), assignmentEntity);
        }
        NumberEntity updatedNumberEntity = numbersService.updateNumber(registeredNumber.getId(), numberRequest);
        expectNumberEntityBasicFields(updatedNumberEntity, registeredNumber);
        assertEquals(updatedNumberEntity.getAssignedTo(), assignmentEntity);
        assertEquals(updatedNumberEntity.getClassification(), numberRequest.getClassification());
        assertEquals(updatedNumberEntity.getCapabilities(), numberRequest.getCapabilities());
        assertEquals(updatedNumberEntity.getAvailableAfter(),
                Optional.ofNullable(numberRequest.getAvailableAfter()).map(ValueWithNull::get).orElse(null));
        assertEquals(updatedNumberEntity.isDedicatedReceiver(), numberRequest.getDedicatedReceiver() == null ? numberEntity.isDedicatedReceiver()
                : numberRequest.getDedicatedReceiver());
    }

    @DataProvider
    public static Object[][] updateNumberNotFullSuccessData() {
        NumberEntity number1 = randomUnassignedNumberEntity();
        UpdateNumberRequest request1 = new UpdateNumberRequest(randomClassification(), null, null, null, null, null);

        NumberEntity number2 = randomUnassignedNumberEntity();
        UpdateNumberRequest request2 = new UpdateNumberRequest(null, randomCapabilities(), null, null, null, null);

        NumberEntity number3 = randomUnassignedNumberEntity();
        UpdateNumberRequest request3 = new UpdateNumberRequest(null, null, ValueWithNull.of(OffsetDateTime.now()), null, null, null);

        NumberEntity number4 = randomUnassignedNumberEntity();
        UpdateNumberRequest request4 = new UpdateNumberRequest(randomClassification(), randomCapabilities(), null, null, null, null);

        NumberEntity number5 = randomUnassignedNumberEntity();
        UpdateNumberRequest request5 = new UpdateNumberRequest(randomClassification(), null, ValueWithNull.of(OffsetDateTime.now()),
                randomDedicatedReceiver(), null, null);

        NumberEntity number6 = randomUnassignedNumberEntity();
        UpdateNumberRequest request6 = new UpdateNumberRequest(null, randomCapabilities(), ValueWithNull.of(OffsetDateTime.now()),
                randomDedicatedReceiver(), null, null);

        return new Object[][]{
                {number1, request1, request1.getClassification(), number1.getCapabilities(), number1.getAvailableAfter()},
                {number2, request2, number2.getClassification(), request2.getCapabilities(), number2.getAvailableAfter()},
                {number3, request3, number3.getClassification(), number3.getCapabilities(), request3.getAvailableAfter().get()},
                {number4, request4, request4.getClassification(), request4.getCapabilities(), number4.getAvailableAfter()},
                {number5, request5, request5.getClassification(), number5.getCapabilities(), request5.getAvailableAfter().get()},
                {number6, request6, number6.getClassification(), request6.getCapabilities(), request6.getAvailableAfter().get()}
        };
    }

    @Test
    @UseDataProvider("updateNumberNotFullSuccessData")
    public void shouldUpdateNumberNotFullSuccess(NumberEntity numberEntity, UpdateNumberRequest updateNumberRequest,
                                                 Classification classification, Set<ServiceType> capabilities, OffsetDateTime availableAfter) {
        NumberEntity registeredNumber = numbersService.registerNumber(numberEntity);
        NumberEntity updatedNumberEntity = numbersService.updateNumber(registeredNumber.getId(), updateNumberRequest);
        expectNumberEntityBasicFields(updatedNumberEntity, registeredNumber);
        expectUpdateNumberFields(updatedNumberEntity, classification, capabilities,
                availableAfter != null ? availableAfter : registeredNumber.getAvailableAfter());
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenUpdateNumber() {
        numbersService.updateNumber(UUID.randomUUID(), randomUpdateNumberRequestWithAvailableAfter());
    }

    @Test(expected = NumberAvailableAfterUpdateException.class)
    public void shouldThrowNumberAvailableAfterUpdateExceptionWhenUpdateNumber() {
        NumberEntity registeredNumber = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.assignNumberToAccount(registeredNumber.getId(), randomAssignmentEntityWithoutNumberEntity());
        numbersService.updateNumber(registeredNumber.getId(), randomUpdateNumberRequestWithAvailableAfter());
    }

    @Test(expected = NumberUpdateRequestEmptyException.class)
    public void shouldThrowNumberUpdateRequestEmptyExceptionWhenUpdateNumber() {
        NumberEntity registeredNumber = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.updateNumber(registeredNumber.getId(), new UpdateNumberRequest(null, null, null, null, null, null));
    }

    @Test
    public void shouldUpdateNumberForTfnStatusSuccess() {
        UpdateNumberRequest numberRequest = randomUpdateNumberRequestWithOnlyStatus();
        NumberEntity number = unassignedUsTollFreeNumberEntity();
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        NumberEntity registeredNumber = numbersService.registerNumber(number);
        numbersService.assignNumberToAccount(registeredNumber.getId(), assignmentEntity);
        NumberEntity updatedNumberEntity = numbersService.updateNumber(registeredNumber.getId(), numberRequest);
        expectNumberEntityBasicFields(updatedNumberEntity, registeredNumber);
        assertEquals(updatedNumberEntity.getAssignedTo(), assignmentEntity);
        assertEquals(updatedNumberEntity.getStatus(), numberRequest.getStatus());
    }

    @Test(expected = NotUsTollFreeNumberException.class)
    public void shouldThrowNotUsTollFreeNumberExceptionWhenUpdateNumberForTfnStatus() {
        NumberEntity registeredNumber = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.assignNumberToAccount(registeredNumber.getId(), randomAssignmentEntityWithoutNumberEntity());
        numbersService.updateNumber(registeredNumber.getId(), randomUpdateNumberRequestWithOnlyStatus());
    }

    @Test(expected = TollFreeNumberUpdateStatusException.class)
    public void shouldThrowExceptionWhenUpdateNumberForUnassignedTfnStatus() {
        NumberEntity registeredNumber = numbersService.registerNumber(unassignedUsTollFreeNumberEntity());
        UpdateNumberRequest numberRequest = randomUpdateNumberRequestWithOnlyStatus();
        numbersService.updateNumber(registeredNumber.getId(), numberRequest);
    }

    @Test
    public void shouldDeleteNumberSuccess() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.deleteNumber(numberEntity.getId());
        assertFalse(numbersRepository.findById(numberEntity.getId()).isPresent());
    }

    @Test
    public void shouldDeleteNumberAndRecreateSuccess() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numbersService.registerNumber(numberEntity);
        numbersService.deleteNumber(numberEntity.getId());
        assertFalse(numbersRepository.findById(numberEntity.getId()).isPresent());

        NumberEntity otherNumberEntity = randomUnassignedNumberEntity();
        otherNumberEntity.setPhoneNumber(numberEntity.getPhoneNumber());
        numbersService.registerNumber(otherNumberEntity);
        assertTrue(numbersRepository.findById(otherNumberEntity.getId()).isPresent());
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenDeleteNumber() {
        numbersService.deleteNumber(UUID.randomUUID());
    }

    @Test(expected = DeleteAssignedNumberException.class)
    public void shouldThrowDeleteAssignedNumberExceptionWhenDeleteNumber() {
        NumberEntity registeredNumber = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.assignNumberToAccount(registeredNumber.getId(), randomAssignmentEntityWithoutNumberEntity());
        numbersService.deleteNumber(registeredNumber.getId());
    }

    private void expectNumberEntityBasicFields(NumberEntity numberEntityAfter, NumberEntity numberEntityBefore) {
        assertNotNull(numberEntityAfter);
        assertEquals(numberEntityAfter.getId(), numberEntityBefore.getId());
        assertEquals(numberEntityAfter.getCreated(), numberEntityBefore.getCreated());
        assertEquals(numberEntityAfter.getCountry(), numberEntityBefore.getCountry());
        assertEquals(numberEntityAfter.getPhoneNumber(), numberEntityBefore.getPhoneNumber());
        assertEquals(numberEntityAfter.getProviderId(), numberEntityBefore.getProviderId());
        assertEquals(numberEntityAfter.getType(), numberEntityBefore.getType());
        assertNotNull(numberEntityAfter.getCreated());
        assertNotNull(numberEntityAfter.getUpdated());
    }

    private void expectUpdateNumberFields(NumberEntity numberEntity,
                                          Classification classification, Set<ServiceType> capabilities, OffsetDateTime availableAfter) {
        assertEquals(numberEntity.getClassification(), classification);
        assertEquals(numberEntity.getCapabilities(), capabilities);
        assertEquals(numberEntity.getAvailableAfter(), availableAfter);
    }
}
