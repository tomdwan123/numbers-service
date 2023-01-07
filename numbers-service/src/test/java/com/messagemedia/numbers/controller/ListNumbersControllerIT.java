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
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberDto;
import com.messagemedia.numbers.service.client.models.NumberSearchRequest;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.messagemedia.numbers.service.client.models.UpdateNumberRequest;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.messagemedia.numbers.TestData.randomRegisterNumberRequest;
import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;
import static com.messagemedia.numbers.service.client.models.NumberSearchRequest.NumberSearchRequestBuilder.aNumberSearchRequestBuilder;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

public class ListNumbersControllerIT extends AbstractControllerIT {

    private static final Classification CLASSIFICATION_FILTER = Classification.BRONZE;

    @DataProvider
    public static Object[] provideReturnNumberList() {
        return new Object[][]{
                {60, 20},
                {65, 13},
                {3, 16}
        };
    }

    @Test
    @UseDataProvider("provideReturnNumberList")
    public void shouldReturnNumberList(int size, int pageSize) throws JsonProcessingException {
        assertTrue(size > 0);
        assertTrue(pageSize > 0);
        List<NumberDto> numbers = generateNumbers(size);
        checkListNumberWithPageSize(pageSize, numbers, null);
        checkListNumberWithDefaultPageSize(numbers);
    }

    private List<NumberDto> generateNumbers(int size) throws JsonProcessingException {
        List<NumberDto> numbers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NumberDto numberDto = registerNumber();
            if (i % 3 == 0) {
                AssignmentDto assignmentDto = assignNumber(numberDto.getId());
                numberDto.setAssignedTo(assignmentDto);
            }
            numbers.add(numberDto);
        }
        numbers.sort(Comparator.comparing(o -> o.getId().toString()));
        return numbers;
    }

    private void checkListNumberWithDefaultPageSize(List<NumberDto> numbers) {
        int size = numbers.size();
        String token = null;
        if (numbers.size() > NumberSearchRequest.DEFAULT_PAGE_SIZE) {
            size = NumberSearchRequest.DEFAULT_PAGE_SIZE;
            token = numbers.get(NumberSearchRequest.DEFAULT_PAGE_SIZE).getId().toString();
        }
        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification("")).when()
                .get(NUMBERS_SERVICE_URL).then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("numbers", hasSize(size))
                .body("pageMetadata.pageSize", equalTo(NumberSearchRequest.DEFAULT_PAGE_SIZE))
                .body("pageMetadata.token", equalTo(token));

        for (int i = 0; i < size; i++) {
            expectNumberInArrayResult(validatableResponse, i, numbers.get(i));
        }
    }

    private RequestSpecification addParamsToRequest(RequestSpecification requestSpecification, NumberSearchRequest request) {
        if (request == null) {
            return requestSpecification;
        }
        if (request.getMatching() != null) {
            requestSpecification.queryParam("matching", request.getMatching());
        }
        if (request.getAssigned() != null) {
            requestSpecification.queryParam("assigned", request.getAssigned());
        }
        if (request.getCountry() != null) {
            requestSpecification.queryParam("country", request.getCountry());
        }
        if (request.getClassification() != null) {
            requestSpecification.queryParam("classification", request.getClassification().name());
        }
        if (ArrayUtils.isNotEmpty(request.getServiceTypes())) {
            requestSpecification.queryParam("serviceTypes", StringUtils.join(request.getServiceTypes(), ','));
        }
        if (request.getAvailableBy() != null) {
            requestSpecification.queryParam("availableBy", request.getAvailableBy().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        return requestSpecification;
    }

    private void checkListNumberWithPageSize(int pageSize, List<NumberDto> numbers, NumberSearchRequest request) {
        if (numbers.isEmpty()) {
            addParamsToRequest(
                    given().spec(buildRequestSpecification("")).when()
                            .queryParam("pageSize", pageSize),
                    request
            ).get(NUMBERS_SERVICE_URL).then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("numbers", empty())
                    .body("pageMetadata.pageSize", is(pageSize))
                    .body("pageMetadata.token", nullValue());
            return;
        }

        int size = numbers.size();
        int numberRequest = (size - 1) / pageSize + 1;
        String token = null;
        ValidatableResponse validatableResponse;

        // validate first (numberRequest - 1) requests
        for (int i = 0; i < numberRequest - 1; i++) {
            String nextToken = numbers.get((i + 1) * pageSize).getId().toString();

            validatableResponse = addParamsToRequest(
                    given().spec(buildRequestSpecification("")).when()
                            .queryParam("pageSize", pageSize)
                            .queryParam("token", token),
                    request
            ).get(NUMBERS_SERVICE_URL).then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("numbers", hasSize(pageSize))
                    .body("pageMetadata.pageSize", equalTo(pageSize))
                    .body("pageMetadata.token", equalTo(nextToken));

            int offset = i * pageSize;
            for (int index = 0; index < pageSize; index++) {
                expectNumberInArrayResult(validatableResponse, index, numbers.get(index + offset));
            }
            token = nextToken;
        }

        // validate final request
        int remainSize = (size % pageSize == 0) ? pageSize : size % pageSize;
        validatableResponse = addParamsToRequest(
                given().spec(buildRequestSpecification("")).when()
                        .queryParam("pageSize", pageSize)
                        .queryParam("token", token),
                request
        ).get(NUMBERS_SERVICE_URL).then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("numbers", hasSize(remainSize))
                .body("pageMetadata.pageSize", equalTo(pageSize))
                .body("pageMetadata.token", nullValue());

        int offset = (numberRequest - 1) * pageSize;
        for (int i = 0; i < remainSize; i++) {
            expectNumberInArrayResult(validatableResponse, i, numbers.get(i + offset));
        }
    }

    @Test
    @UseDataProvider("provideReturnNumberList")
    public void shouldReturnNumberListWithFilter(int size, int pageSize) throws Exception {
        List<NumberDto> numbers = generateNumbersWithFilter(size);
        NumberSearchRequest request = aNumberSearchRequestBuilder()
                .withCountry("Au")
                .withAssigned(true)
                .withMatching("^.61.*")
                .withClassification(CLASSIFICATION_FILTER)
                .withServiceTypes(new ServiceType[]{ServiceType.SMS, ServiceType.TTS, ServiceType.MMS, ServiceType.CALL})
                .build();
        checkListNumberWithPageSize(pageSize, numbers, request);

        NumberSearchRequest notFoundRequest = aNumberSearchRequestBuilder()
                .withCountry("US")
                .withMatching("^.61.*")
                .build();
        checkListNumberWithPageSize(pageSize, Collections.emptyList(), notFoundRequest);
    }

    private List<NumberDto> generateNumbersWithFilter(int size) throws JsonProcessingException {
        List<NumberDto> numbers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NumberDto numberDto = registerNumber(randomRegisterNumberRequest(CLASSIFICATION_FILTER));
            if (i % 2 == 0) {
                AssignmentDto assignmentDto = assignNumber(numberDto.getId());
                numberDto.setAssignedTo(assignmentDto);
                numbers.add(numberDto);
            }
        }
        numbers.sort(Comparator.comparing(o -> o.getId().toString()));
        return numbers;
    }

    @Test
    public void shouldReturnEmptyWhenNoNumbersAreRegistered() {
        given().spec(buildRequestSpecification())
                .when()
                .get(NUMBERS_SERVICE_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("numbers", empty());
    }

    @Test
    public void shouldReturnBadRequestWhenRequestWithInvalidParam() {
        given().spec(buildRequestSpecification())
                .param("token", "invalid token")
                .param("pageSize", "not number")
                .param("serviceTypes", "SMS,S")
                .param("classification", "SILICON")
                .param("assigned", "not boolean")
                .when()
                .get(NUMBERS_SERVICE_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(containsString("token"), containsString("pageSize"),
                        containsString("serviceTypes"), containsString("assigned"));
    }

    @DataProvider
    public static Object[] availableByFilterScenarios() {
        return new Object[][]{
                {1, ChronoUnit.MILLIS, 1, false},
                {1, ChronoUnit.MILLIS, 0, false},
                {1, ChronoUnit.DAYS, -10, true},
                {0, ChronoUnit.MILLIS, -1, false}
        };
    }

    @Test
    @UseDataProvider("availableByFilterScenarios")
    public void shouldReturnNumberListWithAbailableByFilter(int expectedResultsSize, TemporalUnit filterAdjustUnit,
                                                            long filterAdjustAmount, boolean explicitNull) throws Exception {
        NumberDto numberDto = registerNumber(randomRegisterNumberRequest());
        OffsetDateTime availableBy;
        if (explicitNull) {
            availableBy = OffsetDateTime.now().plus(filterAdjustAmount, filterAdjustUnit);
            updateNumber(numberDto.getId(), new UpdateNumberRequest(null, null, ValueWithNull.explicitNull(), null, null, null));
        } else {
            assignNumber(numberDto.getId());
            disassociateAssignment(numberDto.getId());
            NumberDto numberWithAvailableAfter = getNumber(numberDto.getId());
            availableBy = numberWithAvailableAfter.getAvailableAfter().plus(filterAdjustAmount, filterAdjustUnit);
        }

        NumberSearchRequest request = aNumberSearchRequestBuilder().withAvailableBy(availableBy).build();
        addParamsToRequest(
                given().spec(buildRequestSpecification("")).when()
                        .queryParam("pageSize", 50),
                request
        ).get(NUMBERS_SERVICE_URL).then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("numbers", hasSize(expectedResultsSize));
    }
}
