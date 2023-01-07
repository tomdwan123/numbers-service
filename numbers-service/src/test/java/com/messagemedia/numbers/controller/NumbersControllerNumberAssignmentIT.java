/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberAssignmentDto;
import com.messagemedia.numbers.service.client.models.NumberAssignmentListResponse;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import com.messagemedia.numbers.service.client.models.NumberDto;
import com.messagemedia.numbers.service.client.models.NumberSearchRequest;
import com.messagemedia.numbers.service.client.models.NumberType;
import com.messagemedia.numbers.service.client.models.RegisterNumberRequest;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.messagemedia.numbers.service.client.models.UpdateNumberRequest;
import com.messagemedia.numbers.service.client.models.Status;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.google.common.collect.Sets.newHashSet;
import static com.messagemedia.numbers.TestData.createAssignmentRequestFromAccount;
import static com.messagemedia.numbers.TestData.randomProviderId;
import static com.messagemedia.numbers.service.client.models
        .NumberAssignmentSearchRequest.NumberAssignmentSearchRequestBuilder.aNumberAssignmentSearchRequestBuilder;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NumbersControllerNumberAssignmentIT extends AbstractControllerIT {

    private static final VendorAccountId VENDOR_ACCOUNT_ID = new VendorAccountId("vendorId", "accountId");
    private static final String NUMBER_ASSIGNMENTS_PATH = "/v1/numbers/assignments";

    @DataProvider
    public static Object[] numberAssignments() {
        return new Object[][]{
                {60, 20},
                {65, 13},
                {3, 16}
        };
    }

    @Test
    @UseDataProvider("numberAssignments")
    public void shouldReturnNumberAssignmentList(int size, int pageSize) throws Exception {
        List<NumberDto> numbers = generateNumberAssignments(size);
        checkListNumberAssignmentWithPageSize(pageSize, numbers);
        checkListNumberAssignmentWithDefaultPageSize(numbers);
    }

    @DataProvider
    public static Object[] filterScenarios() {
        return new Object[][]{
                {5, aNumberAssignmentSearchRequestBuilder().withCountry("AU").build()},
                {1, aNumberAssignmentSearchRequestBuilder().withCountry("AU").withClassification(Classification.SILVER).build()},
                {1, aNumberAssignmentSearchRequestBuilder().withCountry("NZ").build()},
                {0, aNumberAssignmentSearchRequestBuilder().withCountry("NZ").withServiceTypes(new ServiceType[]{ServiceType.MMS}).build()},
                {1, aNumberAssignmentSearchRequestBuilder().withServiceTypes(new ServiceType[]{ServiceType.MMS}).build()},
                {6, aNumberAssignmentSearchRequestBuilder().withLabel("My.*").build()},
                {3, aNumberAssignmentSearchRequestBuilder().withLabel("My.*").withMatching("\\+61.*").build()},
                {2, aNumberAssignmentSearchRequestBuilder().withLabel("My.*").withMatching("\\+61.*").withCountry("AU")
                        .withServiceTypes(new ServiceType[]{ServiceType.SMS}).withClassification(Classification.GOLD).build()},
                {1, aNumberAssignmentSearchRequestBuilder().withStatus(Status.UNVERIFIED).build()},
                {1, aNumberAssignmentSearchRequestBuilder().withStatus(Status.ASSIGNED).withLabel("My.*").build()}
        };
    }

    private void createNumbersAndAssignments() throws Exception {
        NumberDto number1 = registerNumber(new RegisterNumberRequest("+61726252435", UUID.randomUUID(), "AU", NumberType.MOBILE,
                Classification.GOLD, newHashSet(ServiceType.SMS), false));
        NumberDto number2 = registerNumber(new RegisterNumberRequest("+61123456784", UUID.randomUUID(), "AU", NumberType.MOBILE,
                Classification.GOLD, newHashSet(ServiceType.SMS), false));
        NumberDto number3 = registerNumber(new RegisterNumberRequest("+64726252436", UUID.randomUUID(), "NZ", NumberType.MOBILE,
                Classification.GOLD, newHashSet(ServiceType.SMS), false));
        NumberDto number4 = registerNumber(new RegisterNumberRequest("12345", UUID.randomUUID(), "AU", NumberType.SHORT_CODE,
                Classification.GOLD, newHashSet(ServiceType.SMS), false));
        NumberDto number5 = registerNumber(new RegisterNumberRequest("+61726252437", UUID.randomUUID(), "AU", NumberType.MOBILE,
                Classification.SILVER, newHashSet(ServiceType.SMS), false));
        NumberDto number6 = registerNumber(new RegisterNumberRequest("+61726252438", UUID.randomUUID(), "AU", NumberType.MOBILE,
                Classification.GOLD, newHashSet(ServiceType.MMS), false));
        NumberDto number7 = registerNumber(new RegisterNumberRequest("+12025961701", UUID.randomUUID(), "US", NumberType.TOLL_FREE,
                Classification.BRONZE, newHashSet(ServiceType.SMS), false));
        NumberDto number8 = registerNumber(new RegisterNumberRequest("+12025961702", UUID.randomUUID(), "US", NumberType.TOLL_FREE,
                Classification.GOLD, newHashSet(ServiceType.SMS), false));

        assignNumber(number1.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "MyLabel1"));
        assignNumber(number2.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "MyLabel2"));
        assignNumber(number3.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "MyLabel3"));
        assignNumber(number4.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "MyLabel4"));
        assignNumber(number5.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "MyLabel5"));
        assignNumber(number6.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "OtherLabel"));
        assignNumber(number7.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "UnverifiedNumber"));
        updateNumber(number7.getId(),
                new UpdateNumberRequest(Classification.BRONZE, newHashSet(ServiceType.SMS), null, false, Status.UNVERIFIED, randomProviderId()));
        assignNumber(number8.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID, "MyLabel8"));
        updateNumber(number8.getId(),
                new UpdateNumberRequest(Classification.GOLD, newHashSet(ServiceType.SMS), null, false, Status.ASSIGNED, randomProviderId()));
    }

    @Test
    @UseDataProvider("filterScenarios")
    public void testGetNumberAssignmentFilters(int expectedResults, NumberAssignmentSearchRequest request) throws Exception {
        // Given
        request.setVendorId(VENDOR_ACCOUNT_ID.getVendorId().getVendorId());
        request.setVendorId(VENDOR_ACCOUNT_ID.getAccountId().getAccountId());
        createNumbersAndAssignments();

        // When
        NumberAssignmentListResponse response = getRequestWithFilters(request)
                .get(NUMBER_ASSIGNMENTS_PATH)
                .then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract().body().as(NumberAssignmentListResponse.class);

        // Then
        assertEquals(expectedResults, response.getNumberAssignments().size());
        for (NumberAssignmentDto assignment : response.getNumberAssignments()) {
            if (request.getLabel() != null) {
                assertTrue(Pattern.matches(request.getLabel(), assignment.getAssignment().getLabel()));
            }
            if (request.getClassification() != null) {
                assertEquals(request.getClassification(), assignment.getNumber().getClassification());
            }
            if (request.getCountry() != null) {
                assertEquals(request.getCountry(), assignment.getNumber().getCountry());
            }
            if (request.getMatching() != null) {
                assertTrue(Pattern.matches(request.getMatching(), assignment.getNumber().getPhoneNumber()));
            }
            if (request.getServiceTypes() != null) {
                assertThat(assignment.getNumber().getCapabilities(), containsInAnyOrder(request.getServiceTypes()));
            }
            if (request.getStatus() != null) {
                assertEquals(assignment.getNumber().getStatus(), request.getStatus());
            }
        }
    }

    @Test
    public void shouldReturnBadRequestWhenRequestWithInvalidParam() {
        given()
                .param("token", "invalid token")
                .param("pageSize", "not number")
                .param("serviceTypes", "SMS,S")
                .param("classification", "SILICON")
                .when()
                .get(NUMBER_ASSIGNMENTS_PATH)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(containsString("token"), containsString("pageSize"),
                        containsString("serviceTypes"), containsString("classification"));
    }

    private List<NumberDto> generateNumberAssignments(int size) throws JsonProcessingException {
        List<NumberDto> numbers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NumberDto numberDto = registerNumber();
            AssignmentDto assignmentDto = assignNumber(numberDto.getId(), createAssignmentRequestFromAccount(VENDOR_ACCOUNT_ID));
            numberDto.setAssignedTo(assignmentDto);
            numbers.add(numberDto);
        }
        numbers.sort(Comparator.comparing(o -> o.getId().toString()));
        return numbers;
    }

    private RequestSpecification getRequestWithFilters(NumberAssignmentSearchRequest request) {
        RequestSpecification requestSpecification = given()
                .queryParam("vendorId", VENDOR_ACCOUNT_ID.getVendorId().getVendorId())
                .queryParam("accountId", VENDOR_ACCOUNT_ID.getAccountId().getAccountId());
        if (request.getMatching() != null) {
            requestSpecification.queryParam("matching", request.getMatching());
        }
        if (request.getLabel() != null) {
            requestSpecification.queryParam("label", request.getLabel());
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
        if (request.getStatus() != null) {
            requestSpecification.queryParam("status", request.getStatus());
        }
        return requestSpecification;
    }

    private void checkListNumberAssignmentWithPageSize(int pageSize, List<NumberDto> numbers) {
        if (numbers.isEmpty()) {
            given().spec(buildRequestSpecification()).when()
                    .queryParam("pageSize", pageSize)
                    .queryParam("vendorId", VENDOR_ACCOUNT_ID.getVendorId().getVendorId())
                    .queryParam("accountId", VENDOR_ACCOUNT_ID.getAccountId().getAccountId())
                    .get(NUMBER_ASSIGNMENTS_PATH).then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("numberAssignments", empty())
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

            validatableResponse = given().spec(buildRequestSpecification()).when()
                                    .queryParam("pageSize", pageSize)
                                    .queryParam("token", token)
                                    .queryParam("vendorId", VENDOR_ACCOUNT_ID.getVendorId().getVendorId())
                                    .queryParam("accountId", VENDOR_ACCOUNT_ID.getAccountId().getAccountId())
                                    .get(NUMBER_ASSIGNMENTS_PATH).then().assertThat()
                                    .statusCode(HttpStatus.OK.value())
                                    .body("numberAssignments", hasSize(pageSize))
                                    .body("pageMetadata.pageSize", equalTo(pageSize))
                                    .body("pageMetadata.token", equalTo(nextToken));

            int offset = i * pageSize;
            for (int index = 0; index < pageSize; index++) {
                expectNumberAssignmentInArrayResult(validatableResponse, index, numbers.get(index + offset));
            }
            token = nextToken;
        }

        // validate final request
        int remainSize = (size % pageSize == 0) ? pageSize : size % pageSize;
        validatableResponse = given().spec(buildRequestSpecification()).when()
                                .queryParam("pageSize", pageSize)
                                .queryParam("token", token)
                                .queryParam("vendorId", VENDOR_ACCOUNT_ID.getVendorId().getVendorId())
                                .queryParam("accountId", VENDOR_ACCOUNT_ID.getAccountId().getAccountId())
                                .get(NUMBER_ASSIGNMENTS_PATH).then().assertThat()
                                .statusCode(HttpStatus.OK.value())
                                .body("numberAssignments", hasSize(remainSize))
                                .body("pageMetadata.pageSize", equalTo(pageSize))
                                .body("pageMetadata.token", nullValue());

        int offset = (numberRequest - 1) * pageSize;
        for (int i = 0; i < remainSize; i++) {
            expectNumberAssignmentInArrayResult(validatableResponse, i, numbers.get(i + offset));
        }
    }

    private void checkListNumberAssignmentWithDefaultPageSize(List<NumberDto> numbers) {
        int size = numbers.size();
        String token = null;
        if (numbers.size() > NumberAssignmentSearchRequest.DEFAULT_PAGE_SIZE) {
            size = NumberAssignmentSearchRequest.DEFAULT_PAGE_SIZE;
            token = numbers.get(NumberAssignmentSearchRequest.DEFAULT_PAGE_SIZE).getId().toString();
        }
        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification("")).when()
                .queryParam("vendorId", VENDOR_ACCOUNT_ID.getVendorId().getVendorId())
                .queryParam("accountId", VENDOR_ACCOUNT_ID.getAccountId().getAccountId())
                .get(NUMBER_ASSIGNMENTS_PATH).then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("numberAssignments", hasSize(size))
                .body("pageMetadata.pageSize", equalTo(NumberSearchRequest.DEFAULT_PAGE_SIZE))
                .body("pageMetadata.token", equalTo(token));

        for (int i = 0; i < size; i++) {
            expectNumberAssignmentInArrayResult(validatableResponse, i, numbers.get(i));
        }
    }

    private void expectNumberAssignmentInArrayResult(ValidatableResponse validatableResponse, int index, NumberDto numberDto) {
        String numberPrefix = String.format("numberAssignments[%d].number", index);
        String assignmentPrefix = String.format("numberAssignments[%d].assignment", index);
        checkNumber(numberPrefix, validatableResponse, numberDto);
        checkAssignment(assignmentPrefix, validatableResponse, numberDto.getAssignedTo());
    }
}
