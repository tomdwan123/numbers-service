/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.messagemedia.framework.jackson.core.valuewithnull.ValueWithNull;
import com.messagemedia.numbers.service.client.models.*;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;
import static io.restassured.RestAssured.given;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.hamcrest.Matchers.*;

public class NumbersControllerIT extends AbstractControllerIT {

    @DataProvider
    public static Object[][] registerNumberSuccessData() {
        return new Object[][]{
                {"+61491570157", "AU", NumberType.MOBILE, Classification.BRONZE, Sets.newSet(ServiceType.SMS), TRUE},
                {"+841698750114", "VN", NumberType.LANDLINE, Classification.GOLD, Sets.newSet(ServiceType.MMS, ServiceType.SMS), null},
                {"991", "US", NumberType.SHORT_CODE, Classification.SILVER, Sets.newSet(ServiceType.values()), FALSE},
                {"+18002224357", "US", NumberType.TOLL_FREE, Classification.BRONZE, Sets.newSet(ServiceType.TTS, ServiceType.MMS), null}
        };
    }

    @Test
    @UseDataProvider("registerNumberSuccessData")
    public void shouldRegisterNumberOk(String phoneNumber,
                                       String countryCode,
                                       NumberType numberType,
                                       Classification classification,
                                       Set<ServiceType> serviceTypeSet,
                                       Boolean dedicatedReceiver) throws JsonProcessingException {
        RegisterNumberRequest numberRequest = new RegisterNumberRequest(phoneNumber, UUID.randomUUID(), countryCode,
                numberType, classification, serviceTypeSet, dedicatedReceiver);

        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(numberRequest));

        given().spec(requestSpecification)
                .when()
                .post(NUMBERS_SERVICE_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("providerId", equalTo(numberRequest.getProviderId().toString()))
                .body("phoneNumber", equalTo(phoneNumber))
                .body("country", equalTo(countryCode))
                .body("type", equalTo(numberType.name()))
                .body("classification", equalTo(classification.name()))
                .body("created", notNullValue())
                .body("updated", notNullValue())
                .body("availableAfter", notNullValue())
                .body("capabilities", hasSize(serviceTypeSet.size()))
                .body("dedicatedReceiver", equalTo(Boolean.TRUE.equals(numberRequest.getDedicatedReceiver())));
    }

    @Test
    public void shouldConflictWhenRegisterDuplicateNumber() throws JsonProcessingException {
        RegisterNumberRequest registerNumberRequest = new RegisterNumberRequest("+61491570122",
                UUID.randomUUID(),
                "AU",
                NumberType.MOBILE,
                randomClassification(),
                randomCapabilities(),
                randomDedicatedReceiver());

        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(registerNumberRequest));

        given().spec(requestSpecification)
                .when()
                .post(NUMBERS_SERVICE_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value());

        given().spec(requestSpecification)
                .when()
                .post(NUMBERS_SERVICE_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @DataProvider
    public static Object[][] registerNumberBadData() {
        return new Object[][]{
                {null, UUID.randomUUID(), randomCountryCode(), randomNumberType(), randomClassification(), randomCapabilities()},
                {"", UUID.randomUUID(), randomCountryCode(), randomNumberType(), randomClassification(), randomCapabilities()},
                {"+12-3", UUID.randomUUID(), randomCountryCode(), NumberType.MOBILE, randomClassification(), randomCapabilities()},
                {"090909090909", UUID.randomUUID(), randomCountryCode(), NumberType.LANDLINE, randomClassification(), randomCapabilities()},
                {"+012345678901234a", UUID.randomUUID(), randomCountryCode(), NumberType.MOBILE, randomClassification(), randomCapabilities()},
                {"", UUID.randomUUID(), randomCountryCode(), NumberType.TOLL_FREE, randomClassification(), randomCapabilities()},
                {"611234567890", UUID.randomUUID(), "AU", NumberType.TOLL_FREE, randomClassification(), randomCapabilities()},
                {"911a", UUID.randomUUID(), randomCountryCode(), NumberType.SHORT_CODE, randomClassification(), randomCapabilities()},

                {"+61491570158", UUID.randomUUID(), null, NumberType.MOBILE, randomClassification(), randomCapabilities()},
                {"+61491570158", UUID.randomUUID(), "", NumberType.MOBILE, randomClassification(), randomCapabilities()},
                {"+61491570158", UUID.randomUUID(), "Au", NumberType.MOBILE, randomClassification(), randomCapabilities()},
                {"+61491570158", UUID.randomUUID(), "au", NumberType.LANDLINE, randomClassification(), randomCapabilities()},
                {"+61491570158", UUID.randomUUID(), "AB", NumberType.MOBILE, randomClassification(), randomCapabilities()},
                {"+61491570158", UUID.randomUUID(), "AUS", NumberType.MOBILE, randomClassification(), randomCapabilities()},
                {"+61491570158", UUID.randomUUID(), "VN", NumberType.MOBILE, randomClassification(), randomCapabilities()},

                {randomPhoneNumber(), null, randomCountryCode(), randomNumberType(), randomClassification(), randomCapabilities()},
                {randomPhoneNumber(), UUID.randomUUID(), randomCountryCode(), null, Classification.BRONZE, randomCapabilities()},
                {randomPhoneNumber(), UUID.randomUUID(), randomCountryCode(), NumberType.MOBILE, null, randomCapabilities()},
                {randomPhoneNumber(), UUID.randomUUID(), randomCountryCode(), randomNumberType(), randomClassification(), null},
                {randomPhoneNumber(), UUID.randomUUID(), randomCountryCode(), randomNumberType(), randomClassification(), Sets.newSet()}
        };
    }

    @Test
    @UseDataProvider("registerNumberBadData")
    public void shouldReturnBadRequestWhenRegisterNumber(String phoneNumber,
                                                         UUID providerId,
                                                         String countryCode,
                                                         NumberType numberType,
                                                         Classification classification,
                                                         Set<ServiceType> serviceTypeSet) throws Exception {
        RegisterNumberRequest numberRequest = new RegisterNumberRequest(phoneNumber, providerId, countryCode,
                numberType, classification, serviceTypeSet, randomDedicatedReceiver());

        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(numberRequest));

        given().spec(requestSpecification)
                .when()
                .post(NUMBERS_SERVICE_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void shouldGetUnassignedNumberOk() throws Exception {
        NumberDto numberDto = registerNumber();
        ValidatableResponse validatableResponse = given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());

        expectNumberDtoFields(validatableResponse, numberDto)
                .body("availableAfter", not(isEmptyString()))
                .body("$", not(hasKey("assignedTo")));
    }

    @Test
    public void shouldGetAssignedNumberOk() throws Exception {
        NumberDto numberDto = registerNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId());

        ValidatableResponse validatableResponse = given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());

        expectNumberDtoFields(validatableResponse, numberDto)
                .body("availableAfter", nullValue())
                .body("assignedTo.id", equalTo(assignmentDto.getId().toString()))
                .body("assignedTo.vendorId", equalTo(assignmentDto.getVendorId()))
                .body("assignedTo.accountId", equalTo(assignmentDto.getAccountId()))
                .body("assignedTo.callbackUrl", equalTo(assignmentDto.getCallbackUrl()))
                .body("assignedTo.created", not(isEmptyString()))
                .body("assignedTo.metadata", equalTo(assignmentDto.getMetadata()));
    }

    @Test
    public void shouldGetNumberNotFound() {
        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, UUID.randomUUID().toString()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReturnBadRequestWhenGetNumberWithInvalidId() throws Exception {
        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, "invalid-id"))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DataProvider
    public static Object[] updateUnassignedNumberDataSuccess() {
        return new Object[] {
                new UpdateNumberRequest(Classification.BRONZE, null, null, null, null, null),
                new UpdateNumberRequest(Classification.SILVER, null, null, FALSE, null, null),
                new UpdateNumberRequest(Classification.GOLD, null, null, TRUE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS), null, null, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.MMS), null, FALSE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.TTS), null, TRUE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS, ServiceType.MMS), null, TRUE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.MMS, ServiceType.TTS), null, FALSE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS, ServiceType.MMS, ServiceType.TTS), null, FALSE, null, null),
                new UpdateNumberRequest(null, null, ValueWithNull.of(OffsetDateTime.now()), null, null, null),
                new UpdateNumberRequest(Classification.BRONZE, Sets.newSet(ServiceType.SMS), null, null, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS), ValueWithNull.of(OffsetDateTime.now()), null, null, null),
                new UpdateNumberRequest(Classification.BRONZE, null, ValueWithNull.of(OffsetDateTime.now()), null, null, null),
                new UpdateNumberRequest(Classification.BRONZE, Sets.newSet(ServiceType.SMS),
                        ValueWithNull.of(OffsetDateTime.now()), TRUE, null, null),
        };
    }

    @Test
    @UseDataProvider("updateUnassignedNumberDataSuccess")
    public void shouldUpdateUnassignedNumberSuccess(UpdateNumberRequest numberRequest) throws JsonProcessingException {
        NumberDto numberDto = registerNumber();
        ValidatableResponse validatableResponse = given()
                .spec(buildRequestSpecification(getObjectMapper().writeValueAsString(numberRequest)))
                .when()
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());

        // update new value to verify
        ofNullable(numberRequest.getClassification()).ifPresent(numberDto::setClassification);
        ofNullable(numberRequest.getCapabilities()).ifPresent(numberDto::setCapabilities);
        expectNumberDtoFields(validatableResponse, numberDto)
                .body("availableAfter", not(isEmptyString()))
                .body("$", not(hasKey("assignedTo")))
                .body("dedicatedReceiver", not(isEmptyString()));
    }

    @DataProvider
    public static Object[] updateAssignedNumberDataSuccess() {
        return new Object[] {
                new UpdateNumberRequest(Classification.BRONZE, null, null, FALSE, null, null),
                new UpdateNumberRequest(Classification.SILVER, null, null, TRUE, null, null),
                new UpdateNumberRequest(Classification.GOLD, null, null, FALSE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS), null, TRUE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.MMS), null, null, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.TTS), null, null, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS, ServiceType.MMS), null, null, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.MMS, ServiceType.TTS), null, FALSE, null, null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS, ServiceType.MMS, ServiceType.TTS), null, TRUE, null, null),
                new UpdateNumberRequest(Classification.BRONZE, Sets.newSet(ServiceType.SMS), null, null, null, null),
        };
    }

    @Test
    @UseDataProvider("updateAssignedNumberDataSuccess")
    public void shouldUpdateAssignedNumberSuccess(UpdateNumberRequest numberRequest) throws JsonProcessingException {
        NumberDto numberDto = registerNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId());

        ValidatableResponse validatableResponse = given()
                .spec(buildRequestSpecification(getObjectMapper().writeValueAsString(numberRequest)))
                .when()
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());

        // update new value to verify
        ofNullable(numberRequest.getClassification()).ifPresent(numberDto::setClassification);
        ofNullable(numberRequest.getCapabilities()).ifPresent(numberDto::setCapabilities);
        expectNumberDtoFields(validatableResponse, numberDto)
                .body("availableAfter", nullValue())
                .body("assignedTo.id", equalTo(assignmentDto.getId().toString()))
                .body("assignedTo.vendorId", equalTo(assignmentDto.getVendorId()))
                .body("assignedTo.accountId", equalTo(assignmentDto.getAccountId()))
                .body("assignedTo.callbackUrl", equalTo(assignmentDto.getCallbackUrl()))
                .body("assignedTo.created", not(isEmptyString()))
                .body("assignedTo.metadata", equalTo(assignmentDto.getMetadata()));
    }

    @DataProvider
    public static Object[] updateAssignedTollFreeNumberDataSuccess() {
        return new Object[]{
                new UpdateNumberRequest(Classification.BRONZE, null, null, FALSE, randomStatus(), randomProviderId()),
                new UpdateNumberRequest(Classification.SILVER, null, null, TRUE, randomStatus(), randomProviderId()),
                new UpdateNumberRequest(Classification.GOLD, null, null, FALSE, randomStatus(), randomProviderId()),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS), null, TRUE, randomStatus(), null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.MMS), null, null, randomStatus(), null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.TTS), null, null, randomStatus(), null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS, ServiceType.MMS), null, null, randomStatus(), null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.MMS, ServiceType.TTS), null, FALSE, randomStatus(), null),
                new UpdateNumberRequest(null, Sets.newSet(ServiceType.SMS, ServiceType.MMS, ServiceType.TTS), null, TRUE, randomStatus(), null),
                new UpdateNumberRequest(Classification.BRONZE, Sets.newSet(ServiceType.SMS), null, null, randomStatus(), null),
        };
    }

    @Test
    @UseDataProvider("updateAssignedTollFreeNumberDataSuccess")
    public void shouldUpdateAssignedTollFreeNumberSuccess(UpdateNumberRequest numberRequest) throws JsonProcessingException {
        NumberDto numberDto = registerUsTollFreeNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId());

        ValidatableResponse validatableResponse = given()
                .spec(buildRequestSpecification(getObjectMapper().writeValueAsString(numberRequest)))
                .when()
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());

        // update new value to verify
        ofNullable(numberRequest.getClassification()).ifPresent(numberDto::setClassification);
        ofNullable(numberRequest.getCapabilities()).ifPresent(numberDto::setCapabilities);
        ofNullable(numberRequest.getStatus()).ifPresent(numberDto::setStatus);
        ofNullable(numberRequest.getProviderId()).ifPresent(numberDto::setProviderId);
        expectNumberDtoFields(validatableResponse, numberDto)
                .body("availableAfter", nullValue())
                .body("assignedTo.id", equalTo(assignmentDto.getId().toString()))
                .body("assignedTo.vendorId", equalTo(assignmentDto.getVendorId()))
                .body("assignedTo.accountId", equalTo(assignmentDto.getAccountId()))
                .body("assignedTo.callbackUrl", equalTo(assignmentDto.getCallbackUrl()))
                .body("assignedTo.created", not(isEmptyString()))
                .body("assignedTo.metadata", equalTo(assignmentDto.getMetadata()))
                .body("status", equalTo(numberDto.getStatus().name()));
    }

    @Test
    public void shouldBadRequestWhenUpdateNumberWithInvalidId() throws JsonProcessingException {
        given()
                .spec(buildRequestSpecification(getObjectMapper().writeValueAsString(randomUpdateNumberRequestWithAvailableAfter())))
                .when()
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", "invalid-uuid"))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DataProvider
    public static Object[] updateNumberBadData() {
        return new Object[] {
                "",
                "{}",
                "{\"classification\":}",
                "{\"classification\":\"\"}",
                "{\"classification\":\"null\"}",
                "{\"classification\":\"invalid-classification\"}",
                "{\"capabilities\":}",
                "{\"capabilities\":\"\"}",
                "{\"capabilities\":null}",
                "{\"capabilities\":[]}",
                "{\"capabilities\":\"invalid-capabilities\"}",
                "{\"availableAfter\":}",
                "{\"availableAfter\":\"invalid-datetime\"}",
                "{\"status\":}",
                "{\"status\":\"\"}",
                "{\"status\":\"null\"}",
                "{\"status\":\"invalid-status\"}"
        };
    }

    @Test
    @UseDataProvider("updateNumberBadData")
    public void shouldBadRequestWhenUpdateNumberWithBadData(String numberRequest) throws JsonProcessingException {
        NumberDto numberDto = registerNumber();
        given().spec(buildRequestSpecification(numberRequest))
                .when()
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberDto.getId().toString()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void shouldNotFoundWhenUpdateNumber() throws JsonProcessingException {
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(randomUpdateNumberRequestWithAvailableAfter())))
                .when()
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", UUID.randomUUID()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
