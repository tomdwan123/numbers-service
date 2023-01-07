/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.messagemedia.domainmodels.accounts.AccountId;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.framework.jackson.core.valuewithnull.ValueWithNull;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.framework.test.WaitUtils;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.exception.AssignmentUpdateRequestEmptyException;
import com.messagemedia.numbers.exception.InvalidAccountRelationshipException;
import com.messagemedia.numbers.exception.NumberAlreadyAssignedException;
import com.messagemedia.numbers.exception.NumberNotAssignedException;
import com.messagemedia.numbers.exception.NumberNotAvailableException;
import com.messagemedia.numbers.exception.NumberNotFoundException;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import com.messagemedia.numbers.service.client.models.UpdateAssignmentRequest;
import com.messagemedia.service.accountmanagement.client.ServiceAccountManagementClient;
import com.messagemedia.service.accountmanagement.client.model.account.Account;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.messagemedia.numbers.TestData.createAssignmentEntityfromAccount;
import static com.messagemedia.numbers.TestData.mockUpAmsEndpoint;
import static com.messagemedia.numbers.TestData.randomAssignmentEntity;
import static com.messagemedia.numbers.TestData.randomAssignmentEntityWithoutNumberEntity;
import static com.messagemedia.numbers.TestData.randomCallbackUrl;
import static com.messagemedia.numbers.TestData.randomHashMap;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;
import static com.messagemedia.numbers.TestData.randomUpdateAssignmentRequest;
import static com.messagemedia.numbers.TestData.randomValidLabel;
import static com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest.NumberAssignmentSearchRequestBuilder;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(DataProviderSpringRunner.class)
public class NumbersServiceNumberAssignmentsIT {

    private static final int AMS_API_PORT = 12345;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(AMS_API_PORT);

    @Autowired
    private NumbersService numbersService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NumbersRepository numbersRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ServiceAccountManagementClient serviceAccountManagementClient;

    @Value("${numbers.service.availability.graceperiod.days}")
    private int extendAvailableAfterDays;

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
    public void shouldGetNumberAssignmentList() throws Exception {
        VendorAccountId vendorAccountId = new VendorAccountId("vendorId", "accountId");

        NumberEntity numberEntity0 = numbersService.registerNumber(randomUnassignedNumberEntity());
        NumberEntity numberEntity1 = numbersService.registerNumber(randomUnassignedNumberEntity());
        NumberEntity numberEntity2 = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.registerNumber(randomUnassignedNumberEntity());
        NumberEntity numberEntityAssignedToDifferentAccount = numbersService.registerNumber(randomUnassignedNumberEntity());

        numbersService.assignNumberToAccount(numberEntity0.getId(), createAssignmentEntityfromAccount(vendorAccountId));
        numbersService.assignNumberToAccount(numberEntity1.getId(), createAssignmentEntityfromAccount(vendorAccountId));
        numbersService.assignNumberToAccount(numberEntity2.getId(), createAssignmentEntityfromAccount(vendorAccountId));
        numbersService.assignNumberToAccount(numberEntityAssignedToDifferentAccount.getId(), randomAssignmentEntity());

        NumberAssignmentSearchRequest searchRequest = NumberAssignmentSearchRequestBuilder.aNumberAssignmentSearchRequestBuilder()
                .withVendorId(vendorAccountId.getVendorId().getVendorId())
                .withAccountId(vendorAccountId.getAccountId().getAccountId())
                .build();

        List<NumberEntity> expected = ImmutableList.of(numberEntity0, numberEntity1, numberEntity2).stream()
                .map(e -> numbersService.getNumber(e.getId())).collect(Collectors.toList());

        NumberListResult result = numbersService.getNumberAssignments(searchRequest);
        expected.stream().forEach(e -> assertTrue(result.getNumbers().contains(e)));
        assertThat(result.getNumbers().size(), equalTo(3));
    }

    @Test
    @Transactional
    public void shouldAssignNumber() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        NumberEntity dbNumberEntity = numbersService.registerNumber(numberEntity);
        WaitUtils.waitFor(1_000, 1_00, () -> dbNumberEntity.getAvailableAfter().isBefore(OffsetDateTime.now()));

        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        AssignmentEntity dbAssignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), assignmentEntity);

        assertThat(dbAssignmentEntity, is(notNullValue()));
        assertThat(dbAssignmentEntity, is(assignmentEntity));
        assertThat(dbAssignmentEntity.getId(), is(assignmentEntity.getId()));
        assertThat(dbAssignmentEntity.getNumberEntity(), is(numberEntity));
        assertThat(dbAssignmentEntity.getAccountId(), is(assignmentEntity.getAccountId()));
        assertThat(dbAssignmentEntity.getVendorId(), is(assignmentEntity.getVendorId()));
        assertThat(dbAssignmentEntity.getCallbackUrl(), is(assignmentEntity.getCallbackUrl()));
        assertThat(dbAssignmentEntity.getExternalMetadata(), is(assignmentEntity.getExternalMetadata()));
        assertThat(numbersRepository.getOne(numberEntity.getId()).getAvailableAfter(), is(nullValue()));
        assertThat(dbAssignmentEntity.getLabel(), is(assignmentEntity.getLabel()));
    }

    @Test(expected = NumberAlreadyAssignedException.class)
    @Transactional
    public void shouldThrowNumberAlreadyAssignedExceptionWhenAssignNumber() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        WaitUtils.waitFor(1_000, 1_00, () -> numberEntity.getAvailableAfter().isBefore(OffsetDateTime.now()));
        numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());

        //bidirectional not update immediately in a transaction, need to refresh
        entityManager.flush();
        entityManager.refresh(numberEntity);
        numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenAssignNumber() {
        numbersService.assignNumberToAccount(UUID.randomUUID(), randomAssignmentEntityWithoutNumberEntity());
    }

    @Test(expected = NumberNotAvailableException.class)
    public void shouldThrowNumberNotAvailableExceptionWhenAssignNumber() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity = numbersService.registerNumber(numberEntity);

        //update AvailableAfter to the future
        numberEntity.setAvailableAfter(OffsetDateTime.now().plusSeconds(10));
        numbersRepository.save(numberEntity);

        numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());
    }

    @Test
    public void shouldLoadAssignmentDetails() {
        // register number
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numbersService.registerNumber(numberEntity);

        // assign an account to a number
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        AssignmentEntity dbAssignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), assignmentEntity);

        // get assignment detail
        AssignmentEntity dbAssignmentDetails = numbersService.loadAssignmentDetailsByNumberId(numberEntity.getId());

        assertThat(dbAssignmentDetails, is(notNullValue()));
        assertThat(dbAssignmentDetails, is(dbAssignmentEntity));
        assertThat(dbAssignmentDetails.getId(), is(dbAssignmentEntity.getId()));
        assertThat(dbAssignmentDetails.getNumberEntity(), is(numberEntity));
        assertThat(dbAssignmentDetails.getAccountId(), is(dbAssignmentEntity.getAccountId()));
        assertThat(dbAssignmentDetails.getVendorId(), is(dbAssignmentEntity.getVendorId()));
        assertThat(dbAssignmentDetails.getCallbackUrl(), is(dbAssignmentEntity.getCallbackUrl()));
        assertThat(dbAssignmentDetails.getExternalMetadata(), is(dbAssignmentEntity.getExternalMetadata()));
        assertThat(dbAssignmentDetails.getLabel(), is(dbAssignmentEntity.getLabel()));
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenLoadAssignment() {
        numbersService.loadAssignmentDetailsByNumberId(UUID.randomUUID());
    }

    @Test(expected = NumberNotAssignedException.class)
    public void shouldThrowNumberNotAssignedExceptionWhenLoadAssignment() {
        // register number
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numbersService.registerNumber(numberEntity);
        numbersService.loadAssignmentDetailsByNumberId(numberEntity.getId());
    }

    @Test
    public void shouldDisassociateAssignment() {
        //init number & allocate number
        NumberEntity registeredNumber = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.assignNumberToAccount(registeredNumber.getId(), randomAssignmentEntityWithoutNumberEntity());

        //disassociate
        numbersService.disassociateAssignment(registeredNumber.getId());
        NumberEntity updatedNumberEntity = numbersService.getNumber(registeredNumber.getId());

        //verify
        assertNull(updatedNumberEntity.getAssignedTo());
        assertEquals(ChronoUnit.DAYS.between(registeredNumber.getAvailableAfter(), updatedNumberEntity.getAvailableAfter()),
                extendAvailableAfterDays);
    }

    @Test
    public void shouldSetDedicatedReceiverFalseWhenDisassociatingAssignment() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity.setDedicatedReceiver(TRUE);
        NumberEntity registeredNumber = numbersService.registerNumber(numberEntity);
        numbersService.assignNumberToAccount(registeredNumber.getId(), randomAssignmentEntityWithoutNumberEntity());

        numbersService.disassociateAssignment(registeredNumber.getId());
        NumberEntity updatedNumberEntity = numbersService.getNumber(registeredNumber.getId());

        assertThat(updatedNumberEntity.isDedicatedReceiver(), equalTo(false));
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenDisassociateAssignment() {
        numbersService.disassociateAssignment(UUID.randomUUID());
    }

    @Test(expected = NumberNotAssignedException.class)
    public void shouldThrowNumberNotAssignedExceptionWhenDisassociateAssignment() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.disassociateAssignment(numberEntity.getId());
    }

    @DataProvider
    public static Object[] updateAssignmentSuccessData() {
        return new Object[]{
                randomUpdateAssignmentRequest(),
                new UpdateAssignmentRequest(null, ValueWithNull.of(randomHashMap()), null),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()), null, null),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()), ValueWithNull.explicitNull(), null),
                new UpdateAssignmentRequest(ValueWithNull.explicitNull(), ValueWithNull.of(randomHashMap()), null),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()), ValueWithNull.of(randomHashMap()), null),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()), ValueWithNull.of(randomHashMap()),
                        ValueWithNull.of(randomValidLabel())),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()), ValueWithNull.of(randomHashMap()),
                        ValueWithNull.explicitNull()),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()), ValueWithNull.explicitNull(), ValueWithNull.explicitNull()),
                new UpdateAssignmentRequest(ValueWithNull.explicitNull(), ValueWithNull.of(randomHashMap()), ValueWithNull.explicitNull()),
                new UpdateAssignmentRequest(ValueWithNull.explicitNull(), ValueWithNull.explicitNull(), ValueWithNull.explicitNull()),
        };
    }

    @Test
    @UseDataProvider("updateAssignmentSuccessData")
    public void shouldUpdateAssignmentSuccess(UpdateAssignmentRequest assignmentRequest) {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());
        AssignmentEntity updatedAssignmentEntity = numbersService.updateAssignment(numberEntity.getId(), assignmentRequest);

        assertThat(updatedAssignmentEntity.getCallbackUrl(),
                is(assignmentRequest.getCallbackUrl() != null ? assignmentRequest.getCallbackUrlValue() : assignmentEntity.getCallbackUrl()));
        assertThat(updatedAssignmentEntity.getExternalMetadata(),
                is(assignmentRequest.getMetadata() != null ? assignmentRequest.getMetadataValue() : assignmentEntity.getExternalMetadata()));
        assertThat(updatedAssignmentEntity.getLabel(),
                is(assignmentRequest.getLabel() != null ? assignmentRequest.getLabelValue() : assignmentEntity.getLabel()));

        assertThat(updatedAssignmentEntity.getAccountId(), is(assignmentEntity.getAccountId()));
        assertThat(updatedAssignmentEntity.getVendorId(), is(assignmentEntity.getVendorId()));
        assertThat(updatedAssignmentEntity.getCreated(), is(assignmentEntity.getCreated()));
        assertThat(updatedAssignmentEntity.getId(), is(assignmentEntity.getId()));
        assertThat(updatedAssignmentEntity.getDeleted(), is(nullValue()));
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionWhenUpdateAssignment() {
        numbersService.updateAssignment(UUID.randomUUID(), randomUpdateAssignmentRequest());
    }

    @Test(expected = AssignmentUpdateRequestEmptyException.class)
    public void shouldThrowAssignmentUpdateRequestEmptyExceptionWhenUpdateAssignment() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.updateAssignment(numberEntity.getId(), new UpdateAssignmentRequest(null, null, null));
    }

    @Test(expected = NumberNotAssignedException.class)
    public void shouldThrowNumberNotAssignedExceptionWhenUpdateAssignment() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.updateAssignment(numberEntity.getId(), randomUpdateAssignmentRequest());
    }

    @Test
    public void shouldReassignNumberSuccess() throws Exception {
        NumberEntity registeredNumber = numbersService.registerNumber(randomUnassignedNumberEntity());
        final Account accountM1 = serviceAccountManagementClient.getAccount(new AccountId("AccountM1"));
        numbersService.assignNumberToAccount(registeredNumber.getId(), createAssignmentEntityfromAccount(accountM1.getVendorAccountId()));
        final Account accountM11 = serviceAccountManagementClient.getAccount(new AccountId("AccountM11"));
        final AssignmentEntity reassignmentEntity = numbersService
                .reassignNumber(registeredNumber.getId(), createAssignmentEntityfromAccount(accountM11.getVendorAccountId()));

        // when
        final AssignmentEntity assignmentEntityDb = numbersService.loadAssignmentDetailsByNumberId(registeredNumber.getId());

        // then
        assertEquals(registeredNumber.getId(), reassignmentEntity.getNumberEntity().getId());
        assertEquals(accountM11.getVendorAccountId().getAccountId().getAccountId(), reassignmentEntity.getAccountId());
        assertEquals(accountM11.getVendorAccountId().getVendorId().getVendorId(), reassignmentEntity.getVendorId());
        assertEquals(reassignmentEntity, assignmentEntityDb);
    }

    @DataProvider
    public static Object[][] invalidRelationAccounts() {
        return new Object[][]{
                {"AccountM1", "AccountM2"},
                {"AccountC1", "AccountM12"},
        };
    }

    @Test(expected = InvalidAccountRelationshipException.class)
    @UseDataProvider("invalidRelationAccounts")
    public void shouldReassignNumberFailed(String acct1, String acct2) throws Exception {
        NumberEntity registeredNumber = numbersService.registerNumber(randomUnassignedNumberEntity());
        final Account owner = serviceAccountManagementClient.getAccount(new AccountId(acct1));
        numbersService.assignNumberToAccount(registeredNumber.getId(), createAssignmentEntityfromAccount(owner.getVendorAccountId()));
        final Account other = serviceAccountManagementClient.getAccount(new AccountId(acct2));
        numbersService.reassignNumber(registeredNumber.getId(), createAssignmentEntityfromAccount(other.getVendorAccountId()));
    }

    @Test
    public void shouldUpdateLabelAsNullWhenExplicitNullSpecified() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());
        UpdateAssignmentRequest updateAssignmentRequest = new UpdateAssignmentRequest(
                ValueWithNull.of(randomCallbackUrl()),
                ValueWithNull.of(randomHashMap()),
                ValueWithNull.explicitNull());
        AssignmentEntity updatedAssignmentEntity = numbersService.updateAssignment(numberEntity.getId(), updateAssignmentRequest);

        assertThat(updatedAssignmentEntity.getLabel(), is(nullValue()));
    }

    @Test
    public void shouldNotUpdateLabelWhenNotSpecified() {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntity());
        UpdateAssignmentRequest updateAssignmentRequest = new UpdateAssignmentRequest(
                ValueWithNull.of(randomCallbackUrl()),
                ValueWithNull.of(randomHashMap()),
                null);
        AssignmentEntity updatedAssignmentEntity = numbersService.updateAssignment(numberEntity.getId(), updateAssignmentRequest);

        assertThat(updatedAssignmentEntity.getLabel(), is(assignmentEntity.getLabel()));
    }
}
