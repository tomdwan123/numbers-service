/*
 * Copyright (c) Message4U Pty Ltd 2014-2021
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.config.BuildVersion;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.numbers.config.WorkerContext;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.NumberForwardDto;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.HashSet;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.messagemedia.framework.config.impl.SpringProfileCalculator.DEFAULT_ENVIRONMENT;
import static com.messagemedia.framework.test.IntegrationTestUtilities.pathToString;
import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.TestDataCreateCallableNumber.randomAssignedNumberEntityWithCallCapability;
import static com.messagemedia.numbers.TestDataCreateCallableNumber.randomAssignedNumberEntityWithoutCallCapability;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@ActiveProfiles(DEFAULT_ENVIRONMENT)
@WebAppConfiguration
@ContextConfiguration(classes = WorkerContext.class)
@RunWith(DataProviderSpringRunner.class)
public class NumbersForwardControllerIT {
    private static final int PORT = Integer.parseInt(new BuildVersion().getValue(WEB_CONTAINER_PORT));

    private static final int CALLABLE_NUMBERS_SERVICE_PORT = 4201;
    private static final VendorAccountId VENDOR_ACCOUNT_ID = new VendorAccountId("vendorId", "accountId");
    private static final NumberEntity NUMBER_ENTITY = randomUnassignedNumberEntityWithCallCapability();
    private static final NumberEntity NUMBER_ENTITY_ASSIGNED = randomAssignedNumberEntityWithCallCapability(VENDOR_ACCOUNT_ID);
    private static final NumberEntity NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY =
            randomAssignedNumberEntityWithoutCallCapability(VENDOR_ACCOUNT_ID);

    @ClassRule
    public static WireMockRule mockCallableNumbersApi = new WireMockRule(CALLABLE_NUMBERS_SERVICE_PORT);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NumbersRepository numberRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Before
    public void setup() throws Exception {
        RestAssured.port = PORT;
        cleanUpDatabase();
        objectMapper.registerModule(new JavaTimeModule());
        getNumberRepository().save(NUMBER_ENTITY);
        AssignmentEntity assignmentEntity = NUMBER_ENTITY_ASSIGNED.getAssignedTo();
        assignmentEntity.setNumberEntity(NUMBER_ENTITY_ASSIGNED);
        NUMBER_ENTITY_ASSIGNED.setPhoneNumber("+61491570160");
        getNumberRepository().save(NUMBER_ENTITY_ASSIGNED);
        getAssignmentRepository().save(assignmentEntity);

        AssignmentEntity newAssignmentEntity = NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY.getAssignedTo();
        newAssignmentEntity.setNumberEntity(NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY);
        getNumberRepository().save(NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY);
        getAssignmentRepository().save(newAssignmentEntity);

        mockCallableNumbersApi.resetMappings();
    }

    @After
    public void tearDown() {
        cleanUpDatabase();
    }

    protected void cleanUpDatabase() {
        assignmentRepository.deleteAllInBatch();
        numberRepository.deleteAllInBatch();
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected NumbersRepository getNumberRepository() {
        return numberRepository;
    }

    protected AssignmentRepository getAssignmentRepository() {
        return assignmentRepository;
    }

    protected RequestSpecification buildRequestSpecification(String body) {
        return buildRequestSpecification().body(body);
    }

    protected RequestSpecification buildRequestSpecification() {
        return new RequestSpecBuilder()
                .setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .build();
    }

    @Test
    public void shouldGetNumberForward() {
        mockCallableNumbersServiceApiGet(HttpStatus.OK, pathToString("/callable_numbers_api/get_callable_number.json"));

        given().when()
                .get(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("destination", equalTo("+84977000000"));
    }

    @Test
    public void shouldReturnNotFoundStatus() {
        given().when()
                .get(String.format(FORWARD_URL_FORMAT, UUID.randomUUID()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldGetNumberForwardNullRoutingProfile() {
        mockCallableNumbersServiceApiGet(HttpStatus.OK,
                pathToString("/callable_numbers_api/get_callable_number_routing_profile_null.json"));

        given().when()
                .get(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("destination", nullValue());
    }

    @Test
    public void shouldReturn400WhenNumberForwardWithoutCallCapability() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity.setCapabilities(new HashSet<>());
        getNumberRepository().save(numberEntity);

        given().when()
                .get(String.format(FORWARD_URL_FORMAT, numberEntity.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void shouldGetNumberForwardCallableNumbersApiError() {
        mockCallableNumbersServiceApiGet(HttpStatus.INTERNAL_SERVER_ERROR, "");

        given().when()
                .get(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("destination", nullValue());
    }

    @Test
    public void shouldGetNumberForwardCallableNumbersServiceTimeout() {
        mockCallableNumbersServiceApiGet(HttpStatus.INTERNAL_SERVER_ERROR, "", 7000);

        given().when()
                .get(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("destination", nullValue());
    }

    private void mockCallableNumbersServiceApiGet(HttpStatus responseStatus, String responseBody) {
        mockCallableNumbersServiceApiGet(responseStatus, responseBody, 500);
    }

    private void mockCallableNumbersServiceApiGet(HttpStatus responseStatus, String responseBody, int delay) {
        mockCallableNumbersApi.stubFor(get(urlPathEqualTo("/api/numbers/" + NUMBER_ENTITY.getPhoneNumber()))
                .willReturn(aResponse().withFixedDelay(delay)
                        .withStatus(responseStatus.value())
                        .withBody(responseBody)));
    }

    @Test
    public void shouldDeleteNumberForward() {
        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo("/api/numbers/" + NUMBER_ENTITY.getPhoneNumber()))
                .willReturn(aResponse().withFixedDelay(500)
                        .withStatus(HttpStatus.NO_CONTENT.value())));

        given().when()
                .delete(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DataProvider
    public static Object[] createForwardNumberData() {
        return new Object[]{
           new NumberForwardDto("+61491570159")
        };
    }

    @Test
    @UseDataProvider("createForwardNumberData")
    public void shouldCreateNumberForwardSuccess(NumberForwardDto createForwardNumberData) throws Exception {
        //Given
        NumberForwardDto getForwardNumberResponse = new NumberForwardDto("null");
        mockCallableNumbersApi.stubFor(get(urlPathEqualTo("/api/numbers/" + NUMBER_ENTITY_ASSIGNED.getPhoneNumber()))
                .willReturn(aResponse().withFixedDelay(500)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(String.valueOf(getForwardNumberResponse))));
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo("/api/customers"))
                .willReturn(aResponse().withFixedDelay(500)
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                        .withBody(pathToString("/callable_numbers_api/create_callable_customer_response.json"))));
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo("/api/routing-profiles"))
                .willReturn(aResponse().withFixedDelay(500)
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                        .withBody(pathToString("/callable_numbers_api/create_callable_routing_profile_response.json"))));
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo("/api/numbers"))
                .willReturn(aResponse().withFixedDelay(500)
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                        .withBody(pathToString("/callable_numbers_api/create_callable_number_response.json"))));

        RequestSpecification requestSpecification = buildRequestSpecification().body(getObjectMapper().writeValueAsString(createForwardNumberData));
        given().spec(requestSpecification)
                .when()
                .post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY_ASSIGNED.getId()))
                .then().assertThat()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    @UseDataProvider("createForwardNumberData")
    public void shouldCreateNumberForwardIfUnassignedNotFound(NumberForwardDto createForwardNumberData) throws Exception {
        RequestSpecification requestSpecification = buildRequestSpecification().body(getObjectMapper().writeValueAsString(createForwardNumberData));
        given().spec(requestSpecification)
                .when()
                .post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @UseDataProvider("createForwardNumberData")
    public void shouldCreateNumberForwardEntityNotFound(NumberForwardDto createForwardNumberData) throws Exception {
        //Given
        mockCallableNumbersApi.stubFor(post(urlPathEqualTo("/api/numbers/"))
                .willReturn(aResponse().withFixedDelay(500)
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        RequestSpecification requestSpecification = buildRequestSpecification().body(getObjectMapper().writeValueAsString(createForwardNumberData));
        given().spec(requestSpecification)
                .when()
                .post(String.format(FORWARD_URL_FORMAT, UUID.randomUUID()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @UseDataProvider("createForwardNumberData")
    public void shouldCreateNumberForwardNoCallCapabilityBadRequest(NumberForwardDto createForwardNumberData) throws Exception {
        RequestSpecification requestSpecification = buildRequestSpecification().body(getObjectMapper().writeValueAsString(createForwardNumberData));
        given().spec(requestSpecification)
                .when()
                .post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DataProvider
    public static Object[] createForwardNumberDataBadRequest() {
        return new Object[]{
                new NumberForwardDto("invalidNumber")
        };
    }

    @Test
    @UseDataProvider("createForwardNumberDataBadRequest")
    public void shouldCreateNumberForwardInvalidNumberBadRequest(NumberForwardDto createForwardNumberDataBadRequest) throws Exception {
        RequestSpecification requestSpecification = buildRequestSpecification()
                .body(getObjectMapper().writeValueAsString(createForwardNumberDataBadRequest));
        given().spec(requestSpecification)
                .when()
                .post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY_ASSIGNED.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
