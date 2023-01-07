/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.messagemedia.numbers.TestData.ASSIGNMENT_URL_FORMAT;
import static com.messagemedia.numbers.TestData.createAssignmentRequestFromAccount;
import static com.messagemedia.numbers.TestData.mockUpAmsEndpoint;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.service.client.models.AssignNumberRequest;
import com.messagemedia.numbers.service.client.models.NumberDto;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class ReassignNumberControllerIT extends AbstractControllerIT {

    private static final int AMS_API_PORT = 12345;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(AMS_API_PORT);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockUpAmsEndpoint();
    }

    @Test
    public void shouldReassignAccountSuccess() throws Exception {
        // Given
        NumberDto numberDto = registerNumber();
        final AssignNumberRequest anr1 = createAssignmentRequestFromAccount(
            new VendorAccountId("MessageMedia", "AccountM11"));
        final AssignNumberRequest anr2 = createAssignmentRequestFromAccount(
            new VendorAccountId("MessageMedia", "AccountM12"));
        assignNumber(numberDto.getId(), anr1);

        given().spec(buildRequestSpecification().body(getObjectMapper().writeValueAsString(anr2)))
            .when()
            .put(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("id", notNullValue())
            .body("numberId", equalTo(numberDto.getId().toString()))
            .body("vendorId", equalTo(anr2.getVendorId()))
            .body("accountId", equalTo(anr2.getAccountId()))
            .body("callbackUrl", equalTo(anr2.getCallbackUrl()))
            .body("created", notNullValue())
            .body("metadata", notNullValue());
    }

    @Test
    public void shouldReassignAccountFailedWithAccountRelationConflict() throws Exception {
        // Given
        NumberDto numberDto = registerNumber();
        final AssignNumberRequest anr1 = createAssignmentRequestFromAccount(
            new VendorAccountId("MessageMedia", "AccountM121"));
        final AssignNumberRequest anr2 = createAssignmentRequestFromAccount(
            new VendorAccountId("MessageMedia", "AccountM3"));
        assignNumber(numberDto.getId(), anr1);

        given().spec(buildRequestSpecification().body(getObjectMapper().writeValueAsString(anr2)))
            .when()
            .put(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
            .then()
            .assertThat()
            .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    public void shouldReassignAccountFailedWithNumberNotAssigned() throws Exception {
        // Given
        NumberDto numberDto = registerNumber();
        final AssignNumberRequest anr2 = createAssignmentRequestFromAccount(
            new VendorAccountId("MessageMedia", "AccountM3"));

        given().spec(buildRequestSpecification().body(getObjectMapper().writeValueAsString(anr2)))
            .when()
            .put(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
            .then()
            .assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReassignAccountFailedWithAccountNotFound() throws Exception {
        // Given
        stubFor(get(urlEqualTo(String.format("/v1/api/accounts/%s", "UnknownAccount")))
            .willReturn(aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())));

        NumberDto numberDto = registerNumber();
        final AssignNumberRequest anr1 = createAssignmentRequestFromAccount(
            new VendorAccountId("MessageMedia", "AccountM121"));
        final AssignNumberRequest anr2 = createAssignmentRequestFromAccount(
            new VendorAccountId("MessageMedia", "UnknownAccount"));
        assignNumber(numberDto.getId(), anr1);

        given().spec(buildRequestSpecification().body(getObjectMapper().writeValueAsString(anr2)))
            .when()
            .put(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
            .then()
            .assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
