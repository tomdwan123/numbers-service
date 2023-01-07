/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.framework.jackson.core.valuewithnull.ValueWithNull;
import com.messagemedia.numbers.repository.BillingRatePlanRepository;
import com.messagemedia.numbers.repository.entities.BillingRatePlanEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.*;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.service.client.models.NumberType.MOBILE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

public class AssignmentControllerIT extends AbstractControllerIT {

    private static final String RATE_PLAN_ID = "2c92a0fc6d151c44016d19e224077b07";
    private static final BillingRatePlanEntity RATE_PLAN = randomBillingRatePlanEntity(RATE_PLAN_ID, "NZ");

    private static final int BILLING_MANAGER_PORT = 7200;
    private static final int CALLABLE_NUMBERS_SERVICE_PORT = 4201;

    @ClassRule
    public static WireMockRule mockBillingManager = new WireMockRule(BILLING_MANAGER_PORT);

    @ClassRule
    public static WireMockRule mockCallableNumbersApi = new WireMockRule(CALLABLE_NUMBERS_SERVICE_PORT);

    @Autowired
    private BillingRatePlanRepository billingRatePlanRepository;

    @Value("${numbers.service.availability.graceperiod.days}")
    private int extendAvailableAfterDays;

    @Before
    public void addRatePlan() {
        deleteRatePlan();
        RATE_PLAN.setType(MOBILE);
        billingRatePlanRepository.save(RATE_PLAN);
    }

    @After
    public void deleteRatePlan() {
        billingRatePlanRepository.delete(RATE_PLAN);
    }

    @Test
    public void shouldAssignNumberOk() throws Exception {
        // Given
        RegisterNumberRequest registerNumberRequest = new RegisterNumberRequest("+64121223322", UUID.randomUUID(), RATE_PLAN.getCountry(),
                RATE_PLAN.getType(), RATE_PLAN.getClassification(), RATE_PLAN.getCapabilities(), false);
        NumberDto numberDto = registerNumber(registerNumberRequest);
        AssignNumberRequest assignNumberRequest = randomAssignNumberRequest();
        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(assignNumberRequest));

        mockBillingManager.stubFor(post(urlPathEqualTo("/v1/addOn/assignment"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                        .withBody(loadJson("billing_manager/billing_manager_success_response.json"))));

        // When
        given().spec(requestSpecification)
                .when()
                .post(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("numberId", equalTo(numberDto.getId().toString()))
                .body("vendorId", equalTo(assignNumberRequest.getVendorId()))
                .body("accountId", equalTo(assignNumberRequest.getAccountId()))
                .body("callbackUrl", equalTo(assignNumberRequest.getCallbackUrl()))
                .body("created", notNullValue())
                .body("metadata", notNullValue());

        // Then
        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("availableAfter", nullValue()); //availableAfter is set to null when assign number

        mockBillingManager.verify(postRequestedFor(urlPathEqualTo("/v1/addOn/assignment"))
                .withHeader(HttpHeaders.CONTENT_TYPE, matching(APPLICATION_JSON_UTF8_VALUE))
                .withHeader("Vendor-Id", matching(assignNumberRequest.getVendorId()))
                .withHeader("Effective-Account-Id", matching(assignNumberRequest.getAccountId()))
                .withRequestBody(equalToJson(loadJson("billing_manager/billing_manager_request.json").replace("${ratePlanId}", RATE_PLAN_ID))));
    }

    @DataProvider
    public static Object[][] assignNumberBadData() {
        return new Object[][]{
                {null, randomAccount(), null, randomCallbackUrl(), randomValidLabel()},
                {"  ", randomAccount(), null, randomCallbackUrl(), randomValidLabel()},
                {randomVendor(), null, null, randomCallbackUrl(), randomValidLabel()},
                {randomVendor(), "   ", null, randomCallbackUrl(), randomValidLabel()},
                {"", "", null, randomCallbackUrl(), randomValidLabel()},
                {null, null, null, null, randomValidLabel()},
                {randomVendor(), randomAccount(), randomInvalidSizeMap(), randomCallbackUrl(), randomValidLabel()},
                {randomVendor(), randomAccount(), randomInvalidKeyMap(), randomCallbackUrl(), randomValidLabel()},
                {randomVendor(), randomAccount(), randomInvalidValueMap(), randomCallbackUrl(), randomValidLabel()},
                {randomVendor(), randomAccount(), randomEmptyKeyMap(), randomCallbackUrl(), randomValidLabel()},
                //don't need to test the map with null key because it is invalid json

                {randomVendor(), randomAccount(), randomHashMap(), "", randomValidLabel()},
                {randomVendor(), randomAccount(), randomHashMap(), "invalid-url", randomValidLabel()},
                {randomVendor(), randomAccount(), randomHashMap(), "ftp://abc.com", randomValidLabel()},
                {randomVendor(), randomAccount(), randomHashMap(), "ftp://abc.com", randomInvalidLabel()},
        };
    }

    @Test
    @UseDataProvider("assignNumberBadData")
    public void shouldAssignNumberBadRequest(String vendorId,
                                             String accountId,
                                             Map<String, String> metadata,
                                             String callbackUrl,
                                             String label) throws JsonProcessingException {
        AssignNumberRequest assignNumberRequest = new AssignNumberRequest(vendorId, accountId, callbackUrl, metadata, label);
        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(assignNumberRequest));

        given().spec(requestSpecification)
                .when()
                .post(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void shouldAssignNumberReturnNotFound() throws JsonProcessingException {
        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(randomAssignNumberRequest()));

        given().spec(requestSpecification)
                .when()
                .post(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReturnConflictWhenAssignNotAvailableNumber() throws JsonProcessingException {
        //create numberEntity, on onPrePersist event will make the availableAfter=created=now
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity = getNumberRepository().save(numberEntity);

        //need to update availableAfter in the future
        numberEntity.setAvailableAfter(OffsetDateTime.now().plusSeconds(10));
        numberEntity = getNumberRepository().save(numberEntity);

        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(randomAssignNumberRequest()));

        given().spec(requestSpecification)
                .when()
                .post(String.format(ASSIGNMENT_URL_FORMAT, numberEntity.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    public void shouldReturnConflictWhenAssignNumberAlreadyAssignedWithSameAccount() throws JsonProcessingException {
        NumberDto numberDto = registerNumber();
        AssignNumberRequest assignNumberRequest = randomAssignNumberRequest();
        assignNumber(numberDto.getId(), assignNumberRequest);

        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(assignNumberRequest)))
                .when()
                .post(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    public void shouldReturnConflictWhenAssignNumberAlreadyAssignedWithDifferentAccount() throws JsonProcessingException {
        NumberDto numberDto = registerNumber();
        AssignNumberRequest assignNumberRequest = new AssignNumberRequest("testVendor", "testAccount",
                randomCallbackUrl(),
                randomHashMap(),
                randomValidLabel());
        assignNumber(numberDto.getId(), assignNumberRequest);

        AssignNumberRequest otherAccountRequest = new AssignNumberRequest("otherVendor", "otherAccount",
                randomCallbackUrl(),
                randomHashMap(),
                randomValidLabel());
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(otherAccountRequest)))
                .when()
                .post(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    public void shouldLoadAssignmentSuccess() throws JsonProcessingException {
        AssignmentDto assignmentDto = assignNumber(registerNumber().getId());

        given().spec(buildRequestSpecification(""))
                .when()
                .get(String.format(ASSIGNMENT_URL_FORMAT, assignmentDto.getNumberId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("numberId", equalTo(assignmentDto.getNumberId().toString()))
                .body("vendorId", equalTo(assignmentDto.getVendorId()))
                .body("accountId", equalTo(assignmentDto.getAccountId()))
                .body("callbackUrl", equalTo(assignmentDto.getCallbackUrl()))
                .body("created", notNullValue())
                .body("metadata", equalTo(assignmentDto.getMetadata()));
    }

    @Test
    public void shouldNotLoadAssignmentDetailsWithUnregisteredNumber() throws JsonProcessingException {
        given().spec(buildRequestSpecification(""))
                .when()
                .get(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldNotLoadAssignmentDetailsWithUnassignedNumber() throws JsonProcessingException {
        given().spec(buildRequestSpecification(""))
                .when()
                .get(String.format(ASSIGNMENT_URL_FORMAT, registerNumber().getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldLoadAssignmentDetailsBadRequest() {
        given().spec(buildRequestSpecification(""))
                .when()
                .get(String.format(ASSIGNMENT_URL_FORMAT, "Invalid uuid"))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void shouldDisassociateAssignmentSuccess() throws Exception {
        // Given
        RegisterNumberRequest registerNumberRequest = new RegisterNumberRequest("+64121223322", UUID.randomUUID(), RATE_PLAN.getCountry(),
                RATE_PLAN.getType(), RATE_PLAN.getClassification(), RATE_PLAN.getCapabilities(), false);
        NumberDto numberDto = registerNumber(registerNumberRequest);
        AssignNumberRequest assignNumberRequest = randomAssignNumberRequest();
        assignNumber(numberDto.getId(), assignNumberRequest);

        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignedTo", notNullValue());

        mockBillingManager.stubFor(delete(urlPathEqualTo("/v1/addOn/assignment"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NO_CONTENT.value())));

        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo("/api/numbers/" + numberDto.getPhoneNumber()))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NO_CONTENT.value())));

        // When
        given().spec(buildRequestSpecification())
                .when()
                .delete(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Then
        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignedTo", nullValue())
                .body("availableAfter", isAfterNow(extendAvailableAfterDays));

        mockBillingManager.verify(deleteRequestedFor(urlPathEqualTo("/v1/addOn/assignment"))
                .withHeader(HttpHeaders.CONTENT_TYPE, matching(APPLICATION_JSON_UTF8_VALUE))
                .withHeader("Vendor-Id", matching(assignNumberRequest.getVendorId()))
                .withHeader("Effective-Account-Id", matching(assignNumberRequest.getAccountId()))
                .withRequestBody(equalToJson(loadJson("billing_manager/billing_manager_request.json").replace("${ratePlanId}", RATE_PLAN_ID))));
    }

    @Test
    public void shouldReturnNotFoundWhenDisassociatedNumberDoesNotExist() throws Exception {
        given().spec(buildRequestSpecification())
                .when()
                .delete(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReturnNotFoundWhenDisassociatedAssignmentDoesNotExist() throws Exception {
        NumberDto numberDto = registerNumber();
        given().spec(buildRequestSpecification())
                .when()
                .delete(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReturnBadRequestWhenDisassociateAssignmentWithInvalidNumberId() throws Exception {
        given().spec(buildRequestSpecification())
                .when()
                .delete(String.format(ASSIGNMENT_URL_FORMAT, "invalid-id"))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DataProvider
    public static Object[] updateAssignmentSuccessData() {
        return new Object[] {
                randomUpdateAssignmentRequest(),
                new UpdateAssignmentRequest(null, ValueWithNull.of(randomHashMap()), ValueWithNull.of(randomValidLabel())),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()), null, ValueWithNull.of(randomValidLabel())),
                new UpdateAssignmentRequest(ValueWithNull.of(randomCallbackUrl()),
                        ValueWithNull.explicitNull(),
                        ValueWithNull.of(randomValidLabel())),
                new UpdateAssignmentRequest(ValueWithNull.explicitNull(),
                        ValueWithNull.of(randomHashMap()),
                        ValueWithNull.of(randomValidLabel())),
                new UpdateAssignmentRequest(ValueWithNull.explicitNull(),
                        ValueWithNull.explicitNull(),
                        ValueWithNull.of(randomValidLabel()))
        };
    }

    @Test
    @UseDataProvider("updateAssignmentSuccessData")
    public void shouldUpdateAssignmentSuccess(UpdateAssignmentRequest assignmentRequest) throws Exception {
        AssignmentDto assignmentDto = assignNumber(registerNumber().getId());
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(assignmentRequest)))
                .when()
                .patch(String.format(ASSIGNMENT_URL_FORMAT, assignmentDto.getNumberId().toString()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("callbackUrl",
                        is(assignmentRequest.getCallbackUrl() != null ? assignmentRequest.getCallbackUrlValue() : assignmentDto.getCallbackUrl()))
                .body("metadata",
                        is(assignmentRequest.getMetadata() != null ? assignmentRequest.getMetadataValue() : assignmentDto.getMetadata()))
                .body("id", is(assignmentDto.getId().toString()))
                .body("numberId", is(assignmentDto.getNumberId().toString()))
                .body("vendorId", is(assignmentDto.getVendorId()))
                .body("accountId", is(assignmentDto.getAccountId()))
                .body("created", notNullValue());
    }

    @Test
    public void shouldReturnNotFoundWhenUpdateAssignmentDoesNotExist() throws Exception {
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(randomUpdateAssignmentRequest())))
                .when()
                .patch(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID().toString()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReturnNotFoundWhenUpdateUnassignedAssignment() throws Exception {
        NumberDto numberDto = registerNumber();
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(randomUpdateAssignmentRequest())))
                .when()
                .patch(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId().toString()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @DataProvider
    public static Object[][] updateAssignmentBadRequestData() {
        return new Object[][] {
                {randomUpdateAssignmentRequest(), "invalid-id"},
                {new UpdateAssignmentRequest(null, null, null), UUID.randomUUID().toString()}
        };
    }

    @Test
    @UseDataProvider("updateAssignmentBadRequestData")
    public void shouldReturnBadRequestWhenUpdateAssignment(UpdateAssignmentRequest assignmentRequest, String numberId) throws Exception {
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(assignmentRequest)))
                .when()
                .patch(String.format(ASSIGNMENT_URL_FORMAT, numberId))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void shouldDeleteCallableNumberWhenUnassigned() throws Exception {
        // Given
        RATE_PLAN.getCapabilities().add(ServiceType.CALL);
        RegisterNumberRequest registerNumberRequest = new RegisterNumberRequest("+64121223322", UUID.randomUUID(), RATE_PLAN.getCountry(),
                RATE_PLAN.getType(), RATE_PLAN.getClassification(), RATE_PLAN.getCapabilities(), false);
        NumberDto numberDto = registerNumber(registerNumberRequest);
        AssignNumberRequest assignNumberRequest = randomAssignNumberRequest();
        assignNumber(numberDto.getId(), assignNumberRequest);

        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignedTo", notNullValue());

        mockBillingManager.stubFor(delete(urlPathEqualTo("/v1/addOn/assignment"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NO_CONTENT.value())));

        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo("/api/numbers/" + numberDto.getPhoneNumber()))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NO_CONTENT.value())));

        // When
        given().spec(buildRequestSpecification())
                .when()
                .delete(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Then
        mockCallableNumbersApi.verify(deleteRequestedFor(urlPathEqualTo("/api/numbers/" + numberDto.getPhoneNumber())));
    }
}
