/*
 * Copyright (c) Message4U Pty Ltd 2014-2020
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.service;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.framework.test.logging.TestLoggingAppender;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.repository.BillingRatePlanRepository;
import com.messagemedia.numbers.repository.entities.BillingRatePlanEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.billing.BillingNotificationService;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberType;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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

import java.util.HashSet;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.messagemedia.framework.test.IntegrationTestUtilities.pathToString;
import static com.messagemedia.numbers.TestData.randomAssignedNumberEntity;
import static com.messagemedia.numbers.TestData.randomBillingRatePlanEntity;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(DataProviderSpringRunner.class)
public class BillingNotificationServiceTest {

    private static final String RATE_PLAN_ID = "2c92a0fc6d151c44016d19e224077b07";
    private static final String AU_BRONZE_RATE_PLAN_ID = "2c92a00e6d151bc4016d19d7a1865808";
    private static final BillingRatePlanEntity RATE_PLAN = randomBillingRatePlanEntity(RATE_PLAN_ID, "NZ");

    private static final int BILLING_MANAGER_PORT = 7200;
    private static final int SLACK_PORT = 4200;

    @ClassRule
    public static WireMockRule mockBillingManager = new WireMockRule(BILLING_MANAGER_PORT);
    @ClassRule
    public static WireMockRule mockSlack = new WireMockRule(SLACK_PORT);

    @Autowired
    private BillingRatePlanRepository billingRatePlanRepository;
    @Autowired
    private BillingNotificationService billingNotificationService;

    @Value("${numbers.service.billingManagement.smsBroadcastRatePlanId}")
    private String smsBroadcastRatePlanId;

    @Value("${numbers.service.billingManagement.callableNumberRatePlanId}")
    private String callableNumberRatePlanId;

    private TestLoggingAppender testLoggingAppender;

    @Before
    public void setup() {
        deleteRatePlan();
        billingRatePlanRepository.save(RATE_PLAN);
        testLoggingAppender = new TestLoggingAppender();
        mockBillingManager.resetMappings();
    }

    @After
    public void deleteRatePlan() {
        billingRatePlanRepository.delete(RATE_PLAN);
    }

    @Test
    public void shouldSendAddNotification() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerAddRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendAddNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("add successful for rate plan " + RATE_PLAN_ID);
        verifyBillingManagerAddRequest(vendorAccountId, 1);
    }

    @Test
    public void shouldSendRemoveNotification() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerRemoveRequest(HttpStatus.NO_CONTENT.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendRemoveNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("remove successful for rate plan " + RATE_PLAN_ID);
        verifyBillingManagerRemoveRequest(vendorAccountId, 1);
    }

    @Test
    public void shouldSendAddNotificationForSmsBroadcast() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("SMSBroadcast", "FunGuys007");

        mockBillingManagerAddRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendAddNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("add successful for rate plan " + smsBroadcastRatePlanId);
        verifyBillingManagerAddRequest(vendorAccountId, 1, smsBroadcastRatePlanId);
    }

    @Test
    public void shouldSendRemoveNotificationForSmsBroadcast() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("SMSBroadcast", "FunGuys007");

        mockBillingManagerRemoveRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendRemoveNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("remove successful for rate plan " + smsBroadcastRatePlanId);
        verifyBillingManagerRemoveRequest(vendorAccountId, 1, smsBroadcastRatePlanId);
    }

    @Test
    public void shouldSendAddNotificationForCallCapabilityNumber() throws Exception {
        // Given AU number + CALL capability
        NumberEntity number = getNumberEntity("AU", ServiceType.CALL, Classification.BRONZE, NumberType.MOBILE);
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerAddRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendAddNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("add successful for rate plan " + callableNumberRatePlanId);
        verifyBillingManagerAddRequest(vendorAccountId, 1, callableNumberRatePlanId);
    }

    @Test
    public void shouldSendRemoveNotificationForCallCapabilityNumber() throws Exception {
        // Given AU number + CALL capability
        NumberEntity number = getNumberEntity("AU", ServiceType.CALL, Classification.BRONZE, NumberType.MOBILE);
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerRemoveRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendRemoveNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("remove successful for rate plan " + callableNumberRatePlanId);
        verifyBillingManagerRemoveRequest(vendorAccountId, 1, callableNumberRatePlanId);
    }

    @Test
    public void shouldSendAddNotificationForMmsCapabilityNumber() throws Exception {
        // Given AU number + MMS capability
        NumberEntity number = getNumberEntity("AU", ServiceType.MMS, Classification.BRONZE, NumberType.MOBILE);
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerAddRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendAddNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("add successful for rate plan " + AU_BRONZE_RATE_PLAN_ID);
        verifyBillingManagerAddRequest(vendorAccountId, 1, AU_BRONZE_RATE_PLAN_ID);
    }

    @Test
    public void shouldSendRemoveNotificationForMmsCapabilityNumber() throws Exception {
        // Given AU number + MMS capability
        NumberEntity number = getNumberEntity("AU", ServiceType.MMS, Classification.BRONZE, NumberType.MOBILE);
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerRemoveRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendRemoveNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresent("remove successful for rate plan " + AU_BRONZE_RATE_PLAN_ID);
        verifyBillingManagerRemoveRequest(vendorAccountId, 1, AU_BRONZE_RATE_PLAN_ID);
    }

    @Test
    public void shouldNotSendAddNotificationForUnbilledAccount() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("Bulletin", "Bulletin2FA");

        mockBillingManagerAddRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendAddNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresentWithReason("Not sending billing notification", "Unbilled account");
        verifyBillingManagerAddRequest(vendorAccountId, 0);
    }

    @Test
    public void shouldNotSendRemoveNotificationForUnbilledAccount() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("Bulletin", "Bulletin2FA");

        mockBillingManagerRemoveRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");

        // When
        billingNotificationService.sendRemoveNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresentWithReason("Not sending billing notification", "Unbilled account");
        verifyBillingManagerRemoveRequest(vendorAccountId, 0);
    }

    @Test
    public void shouldNotSendAddNotificationWhenNoRatePlanFound() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        number.setCountry("UK");
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerAddRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");
        mockSlackNotificationRequest();

        // When
        billingNotificationService.sendAddNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresentWithReason("Not sending billing notification", "No rate plan found for number " + number.getPhoneNumber());
        verifyBillingManagerAddRequest(vendorAccountId, 0);
        verifySlackNotificationRequest("No rate plan found for number " + number.getPhoneNumber());
    }

    @Test
    public void shouldNotSendRemoveNotificationWhenNoRatePlanFound() throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        number.setCountry("UK");
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerRemoveRequest(HttpStatus.CREATED.value(), "billing_manager_success_response.json");
        mockSlackNotificationRequest();

        // When
        billingNotificationService.sendRemoveNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresentWithReason("Not sending billing notification", "No rate plan found for number " + number.getPhoneNumber());
        verifyBillingManagerRemoveRequest(vendorAccountId, 0);
        verifySlackNotificationRequest("No rate plan found for number " + number.getPhoneNumber());
    }

    @Test
    @UseDataProvider("errorResponseScenarios")
    public void shouldHandleAddErrorResponses(int responseCode, String responseBodyFile, String logReason) throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerAddRequest(responseCode, responseBodyFile);
        mockSlackNotificationRequest();

        // When
        billingNotificationService.sendAddNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresentWithReason("Failed to add rate plan", logReason);
        verifyBillingManagerAddRequest(vendorAccountId, 1);
        verifySlackNotificationRequest(logReason);
    }

    @Test
    @UseDataProvider("errorResponseScenarios")
    public void shouldHandleRemoveErrorResponses(int responseCode, String responseBodyFile, String logReason) throws Exception {
        // Given
        NumberEntity number = getNumberEntity();
        VendorAccountId vendorAccountId = new VendorAccountId("MessageMedia", "FunGuys007");

        mockBillingManagerRemoveRequest(responseCode, responseBodyFile);
        mockSlackNotificationRequest();

        // When
        billingNotificationService.sendRemoveNotification(vendorAccountId, number);

        // Then
        testLoggingAppender.assertPresentWithReason("Failed to remove rate plan", logReason);
        verifyBillingManagerRemoveRequest(vendorAccountId, 1);
        verifySlackNotificationRequest(logReason);
    }

    @DataProvider
    public static Object[][] errorResponseScenarios() {
        return new Object[][]{
                {404, "billing_manager_invalid_account_response.json", "Billing account not found"},
                {422, "billing_manager_invalid_rate_plan_response.json", "Rate plan id does not exist"},
                {400, "billing_manager_currency_mismatch_response.json", "Currency mismatch"},
                {500, "billing_manager_currency_mismatch_response.json", "Unknown billing management internal server error"},
                {503, "billing_manager_currency_mismatch_response.json", "Unexpected status code=503"}
        };
    }

    private NumberEntity getNumberEntity() {
        NumberEntity number = randomAssignedNumberEntity();
        number.setCapabilities(RATE_PLAN.getCapabilities());
        number.setClassification(RATE_PLAN.getClassification());
        number.setCountry(RATE_PLAN.getCountry());
        number.setType(RATE_PLAN.getType());
        return number;
    }

    private NumberEntity getNumberEntity(String country, ServiceType capability, Classification classification, NumberType type) {
        Set<ServiceType> capabilities = new HashSet<>();
        capabilities.add(capability);

        NumberEntity number = randomAssignedNumberEntity();
        number.setCountry(country);
        number.setCapabilities(capabilities);
        number.setClassification(classification);
        number.setType(type);
        return number;
    }

    private void mockBillingManagerAddRequest(int statusCode, String responseBodyFile) {
        mockBillingManagerRequest(statusCode, responseBodyFile, post(urlPathEqualTo("/v1/addOn/assignment")));
    }

    private void mockBillingManagerRemoveRequest(int statusCode, String responseBodyFile) {
        mockBillingManagerRequest(statusCode, responseBodyFile, delete(urlPathEqualTo("/v1/addOn/assignment")));
    }

    private void mockBillingManagerRequest(int statusCode, String responseBodyFile, MappingBuilder mappingBuilder) {
        mockBillingManager.stubFor(mappingBuilder
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                        .withBody(pathToString("/billing_manager/" + responseBodyFile))));
    }

    private void verifyBillingManagerAddRequest(VendorAccountId vendorAccountId, int times) {
        verifyBillingManagerAddRequest(vendorAccountId, times, RATE_PLAN_ID);
    }

    private void verifyBillingManagerAddRequest(VendorAccountId vendorAccountId, int times, String ratePlanId) {
        verifyBillingManagerRequest(vendorAccountId, times, ratePlanId, postRequestedFor(urlPathEqualTo("/v1/addOn/assignment")));
    }

    private void verifyBillingManagerRemoveRequest(VendorAccountId vendorAccountId, int times) {
        verifyBillingManagerRemoveRequest(vendorAccountId, times, RATE_PLAN_ID);
    }

    private void verifyBillingManagerRemoveRequest(VendorAccountId vendorAccountId, int times, String ratePlanId) {
        verifyBillingManagerRequest(vendorAccountId, times, ratePlanId, deleteRequestedFor(urlPathEqualTo("/v1/addOn/assignment")));
    }

    private void verifyBillingManagerRequest(VendorAccountId vendorAccountId, int times, String ratePlanId,
                                             RequestPatternBuilder requestPatternBuilder) {
        mockBillingManager.verify(times, requestPatternBuilder
                .withHeader(HttpHeaders.CONTENT_TYPE, matching(APPLICATION_JSON_UTF8_VALUE))
                .withHeader("Vendor-Id", matching(vendorAccountId.getVendorId().getVendorId()))
                .withHeader("Effective-Account-Id", matching(vendorAccountId.getAccountId().getAccountId()))
                .withRequestBody(equalToJson(pathToString("/billing_manager/billing_manager_request.json").replace("${ratePlanId}", ratePlanId))));
    }

    private void mockSlackNotificationRequest() {
        mockSlack.stubFor(post(urlPathEqualTo("/services/abc/123"))
                .willReturn(aResponse().withStatus(200)));
    }

    private void verifySlackNotificationRequest(String message) {
        mockSlack.verify(postRequestedFor(urlPathEqualTo("/services/abc/123")).withRequestBody(containing(message)));
    }
}
