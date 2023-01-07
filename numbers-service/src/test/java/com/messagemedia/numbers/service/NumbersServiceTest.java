/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.exception.*;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import com.messagemedia.numbers.service.client.models.NumberSearchRequest;
import com.messagemedia.numbers.service.client.models.UpdateAssignmentRequest;
import com.messagemedia.numbers.service.client.models.UpdateNumberRequest;
import com.messagemedia.service.accountmanagement.client.exception.ServiceAccountManagementNotFoundException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.service.client.models.NumberSearchRequest.NumberSearchRequestBuilder.aNumberSearchRequestBuilder;
import static com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest.NumberAssignmentSearchRequestBuilder;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class NumbersServiceTest {

    @Mock
    private NumbersRepository numbersRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentVerificationService assignmentVerificationService;

    @Mock
    private AccountReassignVerificationService accountReassignVerificationService;

    @InjectMocks
    private NumbersService numbersService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldRegisterNumber() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        when(numbersRepository.save(numberEntity)).thenReturn(numberEntity);
        assertEquals(numberEntity, numbersService.registerNumber(numberEntity));
    }

    @Test
    public void shouldAssignNumberSuccess() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity.setAvailableAfter(OffsetDateTime.now().minusSeconds(10));
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        when(assignmentRepository.save(assignmentEntity)).thenReturn(assignmentEntity);
        when(assignmentVerificationService.isValidForNewAssignment(any(), any(), any())).thenReturn(true);
        AssignmentEntity dbAssignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), assignmentEntity);
        assertAssignmentsEquals(assignmentEntity, dbAssignmentEntity);
    }

    @Test
    public void shouldReassignNumberSuccess() throws Exception {
        NumberEntity numberEntity = randomAssignedNumberEntity();
        AssignmentEntity newAssignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        newAssignmentEntity.setNumberEntity(numberEntity);
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        when(accountReassignVerificationService.verifyAccountRelationship(any(), any())).thenReturn(true);
        when(assignmentRepository.save(newAssignmentEntity)).thenReturn(newAssignmentEntity);
        final AssignmentEntity assignmentEntity = numbersService.reassignNumber(numberEntity.getId(), newAssignmentEntity);
        assertAssignmentsEquals(newAssignmentEntity, assignmentEntity);
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldReassignNumberThrowExceptionWhenNumberNotFound() throws Exception {
        NumberEntity numberEntity = randomAssignedNumberEntity();
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.empty());
        numbersService.reassignNumber(numberEntity.getId(), assignmentEntity);
    }

    @Test(expected = NumberNotAssignedException.class)
    public void shouldReassignNumberThrowExceptionWhenNumberUnassigned() throws Exception {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.ofNullable(numberEntity));
        numbersService.reassignNumber(numberEntity.getId(), assignmentEntity);
    }

    @Test(expected = InvalidAccountRelationshipException.class)
    public void shouldReassignNumberThrowExceptionWhenInapplicableRelationship() throws Exception {
        NumberEntity numberEntity = randomAssignedNumberEntity();
        AssignmentEntity newAssignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        newAssignmentEntity.setNumberEntity(numberEntity);
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        when(accountReassignVerificationService.verifyAccountRelationship(any(), any())).thenReturn(false);
        when(assignmentRepository.save(newAssignmentEntity)).thenReturn(newAssignmentEntity);
        numbersService.reassignNumber(numberEntity.getId(), newAssignmentEntity);
    }

    @Test(expected = ServiceAccountManagementNotFoundException.class)
    public void shouldReassignNumberThrowExceptionWhenAccountNotFound() throws Exception {
        NumberEntity numberEntity = randomAssignedNumberEntity();
        AssignmentEntity newAssignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        newAssignmentEntity.setNumberEntity(numberEntity);
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        when(accountReassignVerificationService.verifyAccountRelationship(any(), any()))
                .thenThrow(new ServiceAccountManagementNotFoundException("not found"));
        numbersService.reassignNumber(numberEntity.getId(), newAssignmentEntity);
    }

    @Test(expected = NumberAlreadyAssignedException.class)
    @Transactional
    public void shouldThrowNumberAlreadyAssignedExceptionWhenAssignNumber() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity.setAvailableAfter(OffsetDateTime.now().minusSeconds(10));
        numberEntity.setAssignedTo(randomAssignmentEntityWithoutNumberEntity());

        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();

        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        numbersService.assignNumberToAccount(numberEntity.getId(), assignmentEntity);
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenAssignNumber() {
        when(numbersRepository.findById(any())).thenReturn(Optional.empty());
        numbersService.assignNumberToAccount(UUID.randomUUID(), randomAssignmentEntityWithoutNumberEntity());
    }

    @Test(expected = NumberNotAvailableException.class)
    public void shouldThrowNumberNotAvailableExceptionWhenAssignNumber() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        when(assignmentVerificationService.isValidForNewAssignment(any(), any(), any())).thenReturn(false);
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());
    }

    @Test
    public void shouldLoadAssignmentDetails() {
        AssignmentEntity assignmentEntity = randomAssignmentEntity();
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity.setAssignedTo(assignmentEntity);

        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        AssignmentEntity dbAssignmentEntity = numbersService.loadAssignmentDetailsByNumberId(numberEntity.getId());

        assertAssignmentsEquals(dbAssignmentEntity, assignmentEntity);
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenLoadAssignment() {
        when(numbersRepository.findById(any())).thenReturn(Optional.empty());
        numbersService.loadAssignmentDetailsByNumberId(UUID.randomUUID());
    }

    @Test(expected = NumberNotAssignedException.class)
    public void shouldThrowNumberNotAssignedExceptionWhenLoadAssignment() {
        when(numbersRepository.findById(any())).thenReturn(Optional.of(randomUnassignedNumberEntity()));
        numbersService.loadAssignmentDetailsByNumberId(UUID.randomUUID());
    }

    @Test
    public void shouldGetNumber() {
        NumberEntity numberEntity = randomAssignedNumberEntity();
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        assertEquals(numberEntity, numbersService.getNumber(numberEntity.getId()));
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenGetNumber() {
        when(numbersRepository.findById(any())).thenReturn(Optional.empty());
        numbersService.getNumber(UUID.randomUUID());
    }

    @Test
    public void shouldGetNumberList() {
        NumberEntity numberEntity0 = randomAssignedNumberEntity();
        NumberEntity numberEntity1 = randomUnassignedNumberEntity();
        NumberEntity numberEntity2 = randomAssignedNumberEntity();

        NumberSearchRequest numberSearchRequest =
                aNumberSearchRequestBuilder().withPageSize(40).withToken(UUID.randomUUID()).withCountry("AU").build();
        Page<NumberEntity> numberEntities = Mockito.mock(Page.class);
        List<NumberEntity> expected = Arrays.asList(numberEntity0, numberEntity1, numberEntity2);
        when(numberEntities.getContent()).thenReturn(expected);

        when(numbersRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(numberEntities);

        NumberListResult result = numbersService.getNumbers(numberSearchRequest);
        assertEquals(expected, result.getNumbers());
        assertNull(result.getToken());
    }

    @DataProvider
    public static Object[][] updateNumberSuccessData() {
        return new Object[][]{
                {randomUnassignedNumberEntity(), randomUpdateNumberRequestWithAvailableAfter()},
                {randomAssignedNumberEntity(), randomUpdateNumberRequestWithoutAvailableAfter()}
        };
    }

    @Test
    @UseDataProvider("updateNumberSuccessData")
    public void shouldUpdateNumberSuccess(NumberEntity numberEntity, UpdateNumberRequest numberRequest) {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(numberEntity));
        when(numbersRepository.save(any(NumberEntity.class))).thenReturn(numberEntity);

        NumberEntity updatedNumberEntity = numbersService.updateNumber(numberEntity.getId(), numberRequest);
        assertEquals(numberEntity, updatedNumberEntity);
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenUpdateNumber() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        numbersService.updateNumber(UUID.randomUUID(), randomUpdateNumberRequestWithAvailableAfter());
    }

    @Test(expected = NumberAvailableAfterUpdateException.class)
    public void shouldThrowNumberAvailableAfterUpdateExceptionWhenUpdateNumber() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(randomAssignedNumberEntity()));
        numbersService.updateNumber(UUID.randomUUID(), randomUpdateNumberRequestWithAvailableAfter());
    }

    @Test(expected = NumberUpdateRequestEmptyException.class)
    public void shouldThrowNumberUpdateRequestEmptyExceptionWhenUpdateNumber() {
        numbersService.updateNumber(UUID.randomUUID(), new UpdateNumberRequest(null, null, null, null, null, null));
    }

    @Test
    public void shouldDisassociateAssignment() {
        NumberEntity assignedNumberEntity = randomAssignedNumberEntity();
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(assignedNumberEntity));
        when(numbersRepository.save(any(NumberEntity.class))).thenReturn(assignedNumberEntity);

        numbersService.disassociateAssignment(assignedNumberEntity.getId());

        verify(assignmentRepository).delete(any(AssignmentEntity.class));
        verify(numbersRepository).save(any(NumberEntity.class));
    }

    @Test
    public void shouldSetDedicatedReceiverFalseWhenDisassociatingAssignment() {
        NumberEntity assignedNumberEntity = randomAssignedNumberEntity();
        assignedNumberEntity.setDedicatedReceiver(TRUE);
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(assignedNumberEntity));
        when(numbersRepository.save(any(NumberEntity.class))).thenReturn(assignedNumberEntity);

        numbersService.disassociateAssignment(assignedNumberEntity.getId());

        assertThat(assignedNumberEntity.isDedicatedReceiver(), equalTo(false));
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenDisassociateAssignment() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        numbersService.disassociateAssignment(UUID.randomUUID());
    }

    @Test(expected = NumberNotAssignedException.class)
    public void shouldThrowNumberNotAssignedExceptionWhenDisassociateAssignment() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(randomUnassignedNumberEntity()));
        numbersService.disassociateAssignment(UUID.randomUUID());
    }

    @Test
    public void shouldUpdateAssignmentSuccess() {
        AssignmentEntity assignmentEntity = randomAssignmentEntity();
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(randomAssignedNumberEntity()));
        when(assignmentRepository.save(any(AssignmentEntity.class))).thenReturn(assignmentEntity);

        AssignmentEntity updatedAssignmentEntity = numbersService.updateAssignment(assignmentEntity.getId(), randomUpdateAssignmentRequest());
        assertEquals(assignmentEntity, updatedAssignmentEntity);
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenUpdateAssignment() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        numbersService.updateAssignment(UUID.randomUUID(), randomUpdateAssignmentRequest());
    }

    @Test(expected = AssignmentUpdateRequestEmptyException.class)
    public void shouldThrowAssignmentUpdateRequestEmptyExceptionWhenUpdateAssignment() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(randomUnassignedNumberEntity()));
        numbersService.updateAssignment(UUID.randomUUID(), new UpdateAssignmentRequest(null, null, null));
    }

    @Test(expected = NumberNotAssignedException.class)
    public void shouldThrowNumberNotAssignedExceptionWhenUpdateAssignment() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(randomUnassignedNumberEntity()));
        numbersService.updateAssignment(UUID.randomUUID(), randomUpdateAssignmentRequest());
    }

    @Test
    public void shouldUpdateNumberForTfnStatusSuccess() {
        NumberEntity numberEntity = assignedUsTollFreeNumberEntity();
        UpdateNumberRequest numberRequest = randomUpdateNumberRequestWithOnlyStatus();
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(numberEntity));
        when(numbersRepository.save(any(NumberEntity.class))).thenReturn(numberEntity);

        NumberEntity updatedNumberEntity = numbersService.updateNumber(numberEntity.getId(), numberRequest);
        assertEquals(numberEntity, updatedNumberEntity);
    }

    @Test(expected = NotUsTollFreeNumberException.class)
    public void shouldThrowNotUsTollFreeNumberExceptionWhenUpdateNumberForTfnStatus() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(randomAssignedNumberEntity()));
        numbersService.updateNumber(UUID.randomUUID(), randomUpdateNumberRequestWithOnlyStatus());
    }

    @Test(expected = TollFreeNumberUpdateStatusException.class)
    public void shouldThrowExceptionWhenUpdateNumberForUnassignedTfnStatus() {
        NumberEntity numberEntity = unassignedUsTollFreeNumberEntity();
        UpdateNumberRequest numberRequest = randomUpdateNumberRequestWithOnlyStatus();
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(numberEntity));
        numbersService.updateNumber(UUID.randomUUID(), numberRequest);
    }

    public void shouldDeleteNumberSuccess() {
        UUID numberId = UUID.randomUUID();
        when(numbersRepository.findById(eq(numberId))).thenReturn(Optional.of(randomUnassignedNumberEntity()));
        numbersService.deleteNumber(numberId);
        verify(numbersRepository).deleteById(eq(numberId));
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenDeleteNumber() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        numbersService.deleteNumber(UUID.randomUUID());

    }

    @Test(expected = DeleteAssignedNumberException.class)
    public void shouldThrowDeleteAssignedNumberExceptionWhenDeleteNumber() {
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(randomAssignedNumberEntity()));
        numbersService.deleteNumber(UUID.randomUUID());
    }

    @Test
    public void shouldGetNumberAssignmentList() throws Exception {
        VendorAccountId vendorAccountId = new VendorAccountId("vendorId", "accountId");
        NumberEntity numberEntity0 = randomAssignedNumberEntity(vendorAccountId);
        NumberEntity numberEntity1 = randomAssignedNumberEntity(vendorAccountId);
        NumberEntity numberEntity2 = randomAssignedNumberEntity(vendorAccountId);

        NumberAssignmentSearchRequest searchRequest = NumberAssignmentSearchRequestBuilder.aNumberAssignmentSearchRequestBuilder()
                .withVendorId(vendorAccountId.getVendorId().getVendorId())
                .withAccountId(vendorAccountId.getAccountId().getAccountId())
                .withPageSize(5)
                .withToken(UUID.randomUUID())
                .build();

        Page<NumberEntity> numberEntities = Mockito.mock(Page.class);

        List<NumberEntity> expected = Arrays.asList(numberEntity0, numberEntity1, numberEntity2);
        when(numberEntities.getContent()).thenReturn(expected);

        when(numbersRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(numberEntities);

        NumberListResult result = numbersService.getNumberAssignments(searchRequest);
        assertThat(result.getNumbers(), equalTo(expected));
        assertNull(result.getToken());
    }

    @Test(expected = VendorAccountRequiredException.class)
    public void shouldThrowExceptionWhenGetNumberAssignmentListWithNoVendorAccount() throws Exception {
        numbersService.getNumberAssignments(new NumberAssignmentSearchRequest());
    }

    private void assertAssignmentsEquals(AssignmentEntity assignmentEntity, AssignmentEntity comparingAssignmentEntity) {
        assertNotNull(assignmentEntity);
        assertEquals(assignmentEntity, comparingAssignmentEntity);
        assertEquals(assignmentEntity.getId(), comparingAssignmentEntity.getId());
        assertEquals(assignmentEntity.getNumberEntity().getId(), comparingAssignmentEntity.getNumberEntity().getId());
        assertEquals(assignmentEntity.getAccountId(), comparingAssignmentEntity.getAccountId());
        assertEquals(assignmentEntity.getVendorId(), comparingAssignmentEntity.getVendorId());
        assertEquals(assignmentEntity.getCallbackUrl(), comparingAssignmentEntity.getCallbackUrl());
        assertEquals(assignmentEntity.getExternalMetadata(), comparingAssignmentEntity.getExternalMetadata());
    }
}
