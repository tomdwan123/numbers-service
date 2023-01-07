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
import com.messagemedia.numbers.service.client.models.AssignNumberRequest;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import com.messagemedia.numbers.service.client.models.NumberDto;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.restassured.response.ValidatableResponse;

import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.messagemedia.numbers.TestData.randomAssignNumberRequest;
import static com.messagemedia.numbers.controller.AuditController.AUDITING_URL;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AuditControllerIT extends AbstractControllerIT {

    private static final String AUDITING_ASSIGNMENT_URL = AUDITING_URL + "/assignments";
    private static final int CALLABLE_NUMBERS_SERVICE_PORT = 4201;

    @ClassRule
    public static WireMockRule mockCallableNumbersApi = new WireMockRule(CALLABLE_NUMBERS_SERVICE_PORT);

    @Test
    public void shouldGetAssignmentAudits() throws Exception {
        given().spec(buildRequestSpecification())
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void shouldFilterByIdWhenGetAssignmentAudits() throws Exception {
        NumberDto numberDto = registerNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId());
        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification())
                .queryParam("id", assignmentDto.getId())
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(1));
        verifyAssignmentAuditInArray(validatableResponse, 0, assignmentDto, false);
    }

    @Test
    public void shouldFilterByCreatedDateWhenGetAssignmentAudits() throws Exception {
        NumberDto numberDto = registerNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId());
        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification())
                .queryParam("id", assignmentDto.getId())
                .queryParam("createdBefore", DateTimeFormatter.ISO_DATE_TIME.format(assignmentDto.getCreated()))
                .queryParam("createdAfter", DateTimeFormatter.ISO_DATE_TIME.format(assignmentDto.getCreated().minusSeconds(1)))
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(1));
        verifyAssignmentAuditInArray(validatableResponse, 0, assignmentDto, false);
    }

    @Test
    public void shouldFilterByDeletedDateWhenGetAssignmentAudits() throws Exception {
        NumberDto numberDto = registerNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId());
        disassociateAssignment(numberDto.getId());

        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo("/numbers/" + numberDto.getPhoneNumber()))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NO_CONTENT.value())));

        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification())
                .queryParam("id", assignmentDto.getId())
                .queryParam("deletedBefore", DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now().plusDays(1)))
                .queryParam("deletedAfter", DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now().minusDays(1)))
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(1));
        verifyAssignmentAuditInArray(validatableResponse, 0, assignmentDto, true);
    }

    @Test
    public void shouldFilterByNumberIdWhenGetAssignmentAudits() throws Exception {
        NumberDto numberDto = registerNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId());
        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo("/numbers/" + numberDto.getPhoneNumber()))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NO_CONTENT.value())));
        disassociateAssignment(numberDto.getId());
        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification())
                .queryParam("numberId", numberDto.getId())
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(2));

        verifyAssignmentAuditInArray(validatableResponse, 0, assignmentDto, true);
        verifyAssignmentAuditInArray(validatableResponse, 1, assignmentDto, false);
    }

    @Test
    public void shouldFilterByVendorAccountIdWhenGetAssignmentAudits() throws Exception {
        NumberDto numberDto = registerNumber();
        assignNumber(numberDto.getId());

        NumberDto numberDto2 = registerNumber();
        AssignmentDto assignmentDto2 = assignNumber(numberDto2.getId());

        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification())
                .queryParam("vendorAccountId", assignmentDto2.getVendorId() + ":" + assignmentDto2.getAccountId())
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(1));
        verifyAssignmentAuditInArray(validatableResponse, 0, assignmentDto2, false);
    }

    @Test
    public void shouldFilterByAllFieldsWhenGetAssignmentAudits() throws Exception {
        createAssignedNumbers(5);
        AssignNumberRequest assignNumberRequest = randomAssignNumberRequest();
        for (int i = 0; i < 3; i++) {
            NumberDto numberDto = registerNumber();
            assignNumber(numberDto.getId(), assignNumberRequest);
        }

        NumberDto numberDto = registerNumber();
        AssignmentDto assignmentDto = assignNumber(numberDto.getId(), assignNumberRequest);
        disassociateAssignment(numberDto.getId());

        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo("/numbers/" + numberDto.getPhoneNumber()))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NO_CONTENT.value())));

        ValidatableResponse validatableResponse = given().spec(buildRequestSpecification())
                .queryParam("vendorAccountId", assignNumberRequest.getVendorId() + ":" + assignNumberRequest.getAccountId())
                .queryParam("deletedBefore", DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now().plusDays(1)))
                .queryParam("deletedAfter", DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now().minusDays(1)))
                .queryParam("createdBefore", DateTimeFormatter.ISO_DATE_TIME.format(assignmentDto.getCreated()))
                .queryParam("createdAfter", DateTimeFormatter.ISO_DATE_TIME.format(assignmentDto.getCreated().minusSeconds(1)))
                .queryParam("numberId", numberDto.getId())
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(1));
        verifyAssignmentAuditInArray(validatableResponse, 0, assignmentDto, true);
    }

    @DataProvider
    public static Object[] invalidVendorAccountId() {
        return new Object[]{
                "",
                " ",
                "InvalidFormatWithoutColon"
        };
    }

    @Test
    @UseDataProvider("invalidVendorAccountId")
    public void shouldReturnBadRequestWhenGetAssignmentAuditsWithInvalidVendorAccountId(String vendorAccountId) throws Exception {
        given().spec(buildRequestSpecification())
                .queryParam("vendorAccountId", vendorAccountId)
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DataProvider
    public static Object[] invalidAuditToken() {
        return new Object[]{
                "",
                " ",
                "tokenWithoutColon",
                "invalidId:12",
                "d4a4768a-5423-4641-9e87-3ecf5ab90c91:invalidRevisionNumber"
        };
    }

    @Test
    @UseDataProvider("invalidAuditToken")
    public void shouldReturnBadRequestWhenGetAssignmentAuditsWithInvalidToken(String auditToken) throws Exception {
        given().spec(buildRequestSpecification())
                .queryParam("token", auditToken)
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", notNullValue());
    }

    @Test
    public void shouldFilterWithPagingDefaultWhenGetAssignmentAudits() throws Exception {
        createAssignedNumbers(51);
        given().spec(buildRequestSpecification())
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(50))
                .body("pageMetadata.pageSize", equalTo(50))
                .body("pageMetadata.token", notNullValue());
    }

    @Test
    public void shouldFilterByPageSizeWhenGetAssignmentAudits() throws Exception {
        createAssignedNumbers(5);
        given().spec(buildRequestSpecification())
                .queryParam("pageSize", 3)
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(3))
                .body("pageMetadata.pageSize", equalTo(3))
                .body("pageMetadata.token", notNullValue());
    }

    @Test
    public void shouldFilterByTokenWhenGetAssignmentAudits() throws Exception {
        NumberDto numberDto = registerNumber();
        assignNumber(numberDto.getId());
        disassociateAssignment(numberDto.getId());

        createAssignedNumbers(5);
        String token = given().spec(buildRequestSpecification())
                .queryParam("pageSize", 3)
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(3))
                .body("pageMetadata.pageSize", equalTo(3))
                .extract()
                .path("pageMetadata.token").toString();

        given().spec(buildRequestSpecification())
                .queryParam("token", token)
                .queryParam("pageSize", 3)
                .when()
                .get(AUDITING_ASSIGNMENT_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("assignments", hasSize(3))
                .body("pageMetadata.pageSize", equalTo(3))
                .body("pageMetadata.token", notNullValue());
    }

    private void createAssignedNumbers(int times) throws JsonProcessingException {
        for (int i = 0; i < times; i++) {
            NumberDto numberDto = registerNumber();
            assignNumber(numberDto.getId());
        }
    }

    private void verifyAssignmentAuditInArray(ValidatableResponse validatableResponse,
                                             int index,
                                             AssignmentDto expectedAssignmentDto,
                                             boolean includedDeletedField) {
        String prefix = String.format("assignments[%s]", index);
        validatableResponse
                .body(prefix + ".id", equalTo(expectedAssignmentDto.getId().toString()))
                .body(prefix + ".created", notNullValue())
                .body(prefix + ".vendorId", equalTo(expectedAssignmentDto.getVendorId()))
                .body(prefix + ".accountId", equalTo(expectedAssignmentDto.getAccountId()))
                .body(prefix + ".callbackUrl", equalTo(expectedAssignmentDto.getCallbackUrl()))
                .body(prefix + ".metadata", equalTo(expectedAssignmentDto.getMetadata()));
        if (!includedDeletedField) {
            validatableResponse.body(prefix + ".deleted", nullValue());
        } else {
            validatableResponse.body(prefix + ".deleted", notNullValue());
        }
    }
}
