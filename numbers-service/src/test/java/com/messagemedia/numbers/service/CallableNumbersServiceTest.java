/*
 * Copyright (c) Message4U Pty Ltd 2014-2021
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.exception.NumberCapabilityNotAvailableException;
import com.messagemedia.numbers.exception.NumberNotFoundException;
import com.messagemedia.numbers.model.dto.CallableNumberCreateResponse;
import com.messagemedia.numbers.model.dto.CallableNumberCustomerResponse;
import com.messagemedia.numbers.model.dto.CallableNumberDto;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileCreateResponse;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.messagemedia.framework.test.IntegrationTestUtilities.pathToString;
import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.TestDataCreateCallableNumber.randomAssignedNumberEntityWithCallCapability;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(DataProviderSpringRunner.class)
public class CallableNumbersServiceTest {

    private static final int CALLABLE_NUMBERS_SERVICE_PORT = 4201;
    private static final VendorAccountId VENDOR_ACCOUNT_ID = new VendorAccountId("vendorIdTest", "accountIdTest");
    private static final NumberEntity NUMBER_ENTITY_ASSIGNED = randomAssignedNumberEntityWithCallCapability(VENDOR_ACCOUNT_ID);
    private static final NumberEntity NUMBER_ENTITY = randomUnassignedNumberEntityWithCallCapability();
    private static final String CALLABLE_NUMBERS_API_NUMBER = "/api/numbers/" + NUMBER_ENTITY.getPhoneNumber();
    private static final String CALLABLE_NUMBERS_API_ASSIGNED_NUMBER = "/api/numbers/" + NUMBER_ENTITY_ASSIGNED.getPhoneNumber();
    private static final String CALLABLE_NUMBERS_API_NUMBER_CREATE = "/api/numbers";
    private static final String CALLABLE_NUMBERS_API_CUSTOMERS = "/api/customers";
    private static final String CALLABLE_NUMBERS_API_ROUTING_PROFILE = "/api/routing-profiles";

    @ClassRule
    public static WireMockRule mockCallableNumbersApi = new WireMockRule(CALLABLE_NUMBERS_SERVICE_PORT);

    @Autowired
    private NumbersRepository numbersRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CallableNumbersService callableNumbersService;

    @Before
    public void setup() throws Exception {
        deleteAllNumbers();
        numbersRepository.save(NUMBER_ENTITY);
        numbersRepository.save(NUMBER_ENTITY_ASSIGNED);
        mockCallableNumbersApi.resetMappings();
    }

    @After
    public void deleteAllNumbers() {
        assignmentRepository.deleteAllInBatch();
        numbersRepository.deleteAllInBatch();
    }

    @Test
    public void shouldGetCallableNumber() {
        mockCallableNumbersApiGet(pathToString("/callable_numbers_api/get_callable_number.json"), HttpStatus.OK);
        CallableNumberDto callableNumber = callableNumbersService.getCallableNumber(NUMBER_ENTITY.getId());
        mockCallableNumbersApi.verify(getRequestedFor(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER)));
        assertNotNull(callableNumber);
        assertEquals("+84977000000", callableNumber.getRoutingProfile().getNumber());
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundException() {
        callableNumbersService.getCallableNumber(UUID.randomUUID());
    }

    @Test(expected = NumberCapabilityNotAvailableException.class)
    public void shouldThrowNumberCapabilityNotAvailableExceptionWhenGetCallableNumber() {
        NumberEntity numberEntity = numbersRepository.findById(NUMBER_ENTITY.getId()).get();
        numberEntity.setCapabilities(new HashSet<>());
        numbersRepository.save(numberEntity);

        callableNumbersService.getCallableNumber(numberEntity.getId());
    }

    @Test
    public void shouldGetCallableNumberInvalidResponse() {
        mockCallableNumbersApiGet("invalid+json+string===", HttpStatus.OK);
        CallableNumberDto callableNumberDto = callableNumbersService.getCallableNumber(NUMBER_ENTITY.getId());
        assertNull(callableNumberDto);
    }

    @Test
    public void shouldGetCallableNumberInternalServerError() {
        mockCallableNumbersApiGetInternalServerError();
        CallableNumberDto callableNumberDto = callableNumbersService.getCallableNumber(NUMBER_ENTITY.getId());
        assertNull(callableNumberDto);
    }

    private void mockCallableNumbersApiGetInternalServerError() {
        mockCallableNumbersApiGet("", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void mockCallableNumbersApiGet(String responseBody, HttpStatus responseStatus) {
        mockCallableNumbersApi.stubFor(get(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER))
                .willReturn(aResponse()
                        .withStatus(responseStatus.value())
                        .withBody(responseBody)));
    }

    @Test
    public void shouldReturnNull() {
        CallableNumbersService callableNumbersServiceNew = new CallableNumbersService(numbersRepository, new RestTemplate(), "NONE");
        callableNumbersServiceNew.getCallableNumber(UUID.randomUUID());
        callableNumbersServiceNew.deleteCallableNumber(UUID.randomUUID());
        callableNumbersServiceNew.createCustomer(randomVendorAccountId().toColonString());
        callableNumbersServiceNew.createRoutingProfile(randomPhoneNumber(), randomVendorAccountId().toColonString());
        callableNumbersServiceNew.createCallableNumber(UUID.randomUUID(), randomVendorAccountId().toColonString(), "3");
        callableNumbersServiceNew.updateCallableNumber(UUID.randomUUID(), randomVendorAccountId().toColonString(), "3");
    }

    @Test
    public void shouldDeleteCallableNumber() {
        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NO_CONTENT.value())));

        callableNumbersService.deleteCallableNumber(NUMBER_ENTITY.getId());
        mockCallableNumbersApi.verify(deleteRequestedFor(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER)));
    }

    @Test(expected = NumberNotFoundException.class)
    public void shouldThrowNumberNotFoundExceptionsWhenDeleteCallableNumber() {
        callableNumbersService.deleteCallableNumber(UUID.randomUUID());
    }

    @Test(expected = NumberCapabilityNotAvailableException.class)
    public void shouldThrowNumberCapabilityNotAvailableExceptionWhenDeleteCallableNumber() {
        NumberEntity numberEntity = numbersRepository.findById(NUMBER_ENTITY.getId()).get();
        numberEntity.setCapabilities(new HashSet<>());
        numbersRepository.save(numberEntity);

        callableNumbersService.deleteCallableNumber(numberEntity.getId());
    }

    @Test(expected = HttpStatusCodeException.class)
    public void shouldDeleteCallableNumberInternalServerError() {
        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        callableNumbersService.deleteCallableNumber(NUMBER_ENTITY.getId());
    }

    @Test
    public void shouldCreateCustomer() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_CUSTOMERS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withBody(pathToString("/callable_numbers_api/create_callable_customer_response.json"))));
        callableNumbersService.createCustomer(VENDOR_ACCOUNT_ID.toColonString());
        mockCallableNumbersApi.verify(postRequestedFor(urlPathEqualTo(CALLABLE_NUMBERS_API_CUSTOMERS)));
    }

    @Test
    public void shouldCreateCustomerConflict() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_CUSTOMERS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CONFLICT.value())));
        callableNumbersService.createCustomer(VENDOR_ACCOUNT_ID.toColonString());
    }

    @Test
    public void shouldCreateCustomerInvalidResponse() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_CUSTOMERS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("invalid+json+string===")));
        CallableNumberCustomerResponse callableNumberCustomerResponse = callableNumbersService.createCustomer(VENDOR_ACCOUNT_ID.toColonString());
        assertNull(callableNumberCustomerResponse);
    }

    @Test(expected = HttpStatusCodeException.class)
    public void shouldCreateCustomerInternalServerError() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_CUSTOMERS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        CallableNumberCustomerResponse callableNumberCustomerResponse = callableNumbersService.createCustomer(VENDOR_ACCOUNT_ID.toColonString());
        assertNull(callableNumberCustomerResponse);
    }

    @Test
    public void shouldCreateRoutingProfile() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_ROUTING_PROFILE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withBody(pathToString("/callable_numbers_api/create_callable_routing_profile_response.json"))));

        callableNumbersService.createRoutingProfile("+61491570160", VENDOR_ACCOUNT_ID.toColonString());
        mockCallableNumbersApi.verify(postRequestedFor(urlPathEqualTo(CALLABLE_NUMBERS_API_ROUTING_PROFILE)));
    }

    @Test
    public void shouldCreateRoutingProfileInvalidResponse() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_ROUTING_PROFILE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("invalid+json+string===")));
        CallableNumberRoutingProfileCreateResponse callableNumberRoutingProfileCreateResponse =
                callableNumbersService.createRoutingProfile("+61491570160", VENDOR_ACCOUNT_ID.toColonString());
        assertNull(callableNumberRoutingProfileCreateResponse);
    }

    @Test
    public void shouldCreateCallableNumber() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER_CREATE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withBody(pathToString("/callable_numbers_api/create_callable_number_response.json"))));

        callableNumbersService.createCallableNumber(NUMBER_ENTITY_ASSIGNED.getId(), VENDOR_ACCOUNT_ID.toColonString(), "4");
        mockCallableNumbersApi.verify(postRequestedFor(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER_CREATE)));
    }

    @Test
    public void shouldCreateCallableNumberInvalidResponse() {
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo(CALLABLE_NUMBERS_API_NUMBER_CREATE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("invalid+json+string===")));
        CallableNumberCreateResponse callableNumberCreateResponse =
                callableNumbersService.createCallableNumber(NUMBER_ENTITY_ASSIGNED.getId(), VENDOR_ACCOUNT_ID.toColonString(), "4");
        assertNull(callableNumberCreateResponse);
    }

    @Test
    public void shouldUpdateCallableNumber() {
        mockCallableNumbersApi.stubFor(put(urlPathEqualTo(CALLABLE_NUMBERS_API_ASSIGNED_NUMBER))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())));

        callableNumbersService.updateCallableNumber(NUMBER_ENTITY_ASSIGNED.getId(), VENDOR_ACCOUNT_ID.toColonString(), "4");
        mockCallableNumbersApi.verify(putRequestedFor(urlPathEqualTo(CALLABLE_NUMBERS_API_ASSIGNED_NUMBER)));
    }
}
