/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.domainmodels.accounts.AccountId;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.framework.test.WaitUtils;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import com.messagemedia.numbers.service.client.models.Status;
import com.messagemedia.service.accountmanagement.client.ServiceAccountManagementClient;
import com.messagemedia.service.accountmanagement.client.model.account.Account;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.messagemedia.numbers.TestData.createAssignmentEntityfromAccount;
import static com.messagemedia.numbers.TestData.loadJson;
import static com.messagemedia.numbers.TestData.randomAssignmentEntityWithoutNumberEntity;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;
import static com.messagemedia.numbers.TestData.unassignedUsTollFreeNumberEntity;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;


@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(DataProviderSpringRunner.class)
public class NumbersServiceTollFreeNumberIT {

    private static final int AMS_API_PORT = 12345;
    private static final int SLACK_PORT = 4200;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(AMS_API_PORT);
    @ClassRule
    public static WireMockRule mockSlack = new WireMockRule(SLACK_PORT);

    @Autowired
    private NumbersService numbersService;

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
        mockSlackNotificationRequest();
    }

    @After
    public void clean() throws Exception {
        assignmentRepository.deleteAllInBatch();
        numbersRepository.deleteAllInBatch();
    }

    @Test
    @Transactional
    public void shouldAssignTollFreeNumber() {
        NumberEntity numberEntity = unassignedUsTollFreeNumberEntity();
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
        assertThat(dbAssignmentEntity.getLabel(), is(assignmentEntity.getLabel()));
        assertNull(numbersRepository.getOne(numberEntity.getId()).getAvailableAfter());
        assertEquals(numbersRepository.getOne(numberEntity.getId()).getStatus(), Status.UNVERIFIED);
        verifySlackNotificationRequest("Toll-free number was assigned.");
    }

    @Test
    public void shouldReassignTollFreeNumber() throws Exception {
        mockUpAmsEndpoint();
        NumberEntity registeredNumber = numbersService.registerNumber(unassignedUsTollFreeNumberEntity());
        final Account accountM1 = serviceAccountManagementClient.getAccount(new AccountId("AccountM1"));
        numbersService.assignNumberToAccount(registeredNumber.getId(), createAssignmentEntityfromAccount(accountM1.getVendorAccountId()));
        final Account accountM11 = serviceAccountManagementClient.getAccount(new AccountId("AccountM11"));
        final AssignmentEntity reassignmentEntity = numbersService
                .reassignNumber(registeredNumber.getId(), createAssignmentEntityfromAccount(accountM11.getVendorAccountId()));
        final AssignmentEntity assignmentEntityDb = numbersService.loadAssignmentDetailsByNumberId(registeredNumber.getId());
        assertEquals(registeredNumber.getId(), reassignmentEntity.getNumberEntity().getId());
        assertEquals(accountM11.getVendorAccountId().getAccountId().getAccountId(), reassignmentEntity.getAccountId());
        assertEquals(accountM11.getVendorAccountId().getVendorId().getVendorId(), reassignmentEntity.getVendorId());
        assertEquals(reassignmentEntity, assignmentEntityDb);
        verifySlackNotificationRequest("Toll-free number was reassigned.");
    }

    @Test
    public void shouldDisassociateTollFreeNumberAssignment() {
        //init number & allocate number
        NumberEntity registeredNumber = numbersService.registerNumber(unassignedUsTollFreeNumberEntity());
        numbersService.assignNumberToAccount(registeredNumber.getId(), randomAssignmentEntityWithoutNumberEntity());

        //disassociate
        numbersService.disassociateAssignment(registeredNumber.getId());
        NumberEntity updatedNumberEntity = numbersService.getNumber(registeredNumber.getId());

        //verify
        assertNull(updatedNumberEntity.getAssignedTo());
        assertEquals(ChronoUnit.DAYS.between(registeredNumber.getAvailableAfter(), updatedNumberEntity.getAvailableAfter()),
                extendAvailableAfterDays);
        assertEquals(updatedNumberEntity.getStatus(), null);
    }

    @DataProvider
    public static Object[] getNumberAssignmentsData() {
        return new Object[][]{
                {null, null, 3},
                {null, new String[] {"+1800", "+61"}, 3},
                {new String[] {}, new String[] {"+1800"}, 0},
                {new String[] {"all"}, null, 4},
                {new String[] {"all"}, new String[] {"+61"}, 3},
                {new String[] {"all"}, new String[] {"+1800"}, 1},
                {new String[] {"all"}, new String[] {"+1800", "+61"}, 4},
                {new String[] {"accountId2"}, null, 1}
        };
    }

    @Test
    @UseDataProvider("getNumberAssignmentsData")
    public void shouldGetNumberAssignmentListWithAccountsAndMatchings(String[] accounts, String[] matchings, int expectedResult) throws Exception {
        VendorAccountId vendorAccountId = new VendorAccountId("vendorId", "accountId");

        NumberEntity numberEntity0 = numbersService.registerNumber(randomUnassignedNumberEntity());
        NumberEntity numberEntity1 = numbersService.registerNumber(randomUnassignedNumberEntity());
        NumberEntity numberEntity2 = numbersService.registerNumber(randomUnassignedNumberEntity());
        NumberEntity numberEntityAssignedToDifferentAccount = numbersService.registerNumber(unassignedUsTollFreeNumberEntity());

        numbersService.assignNumberToAccount(numberEntity0.getId(), createAssignmentEntityfromAccount(vendorAccountId));
        numbersService.assignNumberToAccount(numberEntity1.getId(), createAssignmentEntityfromAccount(vendorAccountId));
        numbersService.assignNumberToAccount(numberEntity2.getId(), createAssignmentEntityfromAccount(vendorAccountId));
        numbersService.assignNumberToAccount(numberEntityAssignedToDifferentAccount.getId(),
                                            createAssignmentEntityfromAccount(new VendorAccountId("vendorId", "accountId2")));

        NumberAssignmentSearchRequest searchRequest = NumberAssignmentSearchRequest
                .NumberAssignmentSearchRequestBuilder
                .aNumberAssignmentSearchRequestBuilder()
                    .withVendorId(vendorAccountId.getVendorId().getVendorId())
                    .withAccountId(vendorAccountId.getAccountId().getAccountId())
                    .withAccounts(accounts)
                    .withMatchings(matchings)
                    .build();

        NumberListResult result = numbersService.getNumberAssignments(searchRequest);
        assertThat(result.getNumbers().size(), Matchers.equalTo(expectedResult));

    }

    private void mockUpAmsEndpoint() throws Exception {
        mockAmsGetAccount("ams/account_root.json", "MessageMedia");
        mockAmsGetAccount("ams/account_m1.json", "AccountM1");
        mockAmsGetAccount("ams/account_m2.json", "AccountM2");
        mockAmsGetAccount("ams/account_m1_1.json", "AccountM11");
        mockAmsGetAccount("ams/account_m1_2.json", "AccountM12");
        mockAmsGetAccount("ams/account_m1_2_1.json", "AccountM121");
        mockAmsGetAccount("ams/account_m1_2_2.json", "AccountM122");
        mockAmsGetAccount("ams/account_m2.json", "AccountM2");
        mockAmsGetAccount("ams/account_c1.json", "AccountC1");
        mockAmsGetAccount("ams/account_m3.json", "AccountM3");
        mockAmsGetAccount("ams/account_m3_1.json", "AccountM31");
        mockAmsGetAccount("ams/account_m3_1_1.json", "AccountM311");
        mockAmsGetAccount("ams/account_m3_1_2.json", "AccountM312");
        mockAmsGetAccount("ams/account_m4.json", "AccountM4");
    }

    private void mockAmsGetAccount(String filePath, String accountName) throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo(String.format("/v1/api/accounts/%s", accountName)))
                .withQueryParam("effectiveFeatures", equalTo(String.valueOf(false)))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                        .withBody(loadJson(filePath))));
    }

    private void mockSlackNotificationRequest() {
        mockSlack.stubFor(post(urlPathEqualTo("/services/def/123"))
                .willReturn(aResponse().withStatus(200)));
    }

    private void verifySlackNotificationRequest(String message) {
        mockSlack.verify(postRequestedFor(urlPathEqualTo("/services/def/123")).withRequestBody(containing(message)));
    }
}
