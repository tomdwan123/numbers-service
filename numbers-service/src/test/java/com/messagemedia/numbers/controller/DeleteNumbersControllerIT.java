/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.numbers.service.client.models.NumberDto;
import com.messagemedia.numbers.service.client.models.RegisterNumberRequest;

import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;
import static io.restassured.RestAssured.given;

public class DeleteNumbersControllerIT extends AbstractControllerIT {

    private static final int CALLABLE_NUMBERS_SERVICE_PORT = 4201;

    @ClassRule
    public static WireMockRule mockCallableNumbersApi = new WireMockRule(CALLABLE_NUMBERS_SERVICE_PORT);

    @Test
    public void shouldDeleteUnassignedNumberSuccess() throws Exception {
        NumberDto numberDto = registerNumber();

        given().spec(buildRequestSpecification())
                .when()
                .delete(NUMBERS_SERVICE_URL + "/" + numberDto.getId())
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldDeleteUnassignedNumberAndRecreateSuccess() throws Exception {
        NumberDto numberDto = registerNumber();

        given().spec(buildRequestSpecification())
                .when()
                .delete(NUMBERS_SERVICE_URL + "/" + numberDto.getId())
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        //re-create with same phone number
        RegisterNumberRequest numberRequest = new RegisterNumberRequest(numberDto.getPhoneNumber(), UUID.randomUUID(), numberDto.getCountry(),
                numberDto.getType(), randomClassification(), randomCapabilities(), randomDedicatedReceiver());

        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(numberRequest)))
                .when()
                .post(NUMBERS_SERVICE_URL)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void shouldDeleteDisassociatedNumberSuccess() throws Exception {
        NumberDto numberDto = registerNumber();
        assignNumber(numberDto.getId());

        mockCallableNumbersApi.stubFor(delete(urlPathEqualTo("/numbers/" + numberDto.getPhoneNumber()))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NO_CONTENT.value())));

        //disassociate
        given().spec(buildRequestSpecification())
                .when()
                .delete(String.format(ASSIGNMENT_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        //delete number
        given().spec(buildRequestSpecification())
                .when()
                .delete(NUMBERS_SERVICE_URL + "/" + numberDto.getId())
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given().when()
                .get(String.format(GET_NUMBER_URL_FORMAT, numberDto.getId()))
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReturnNotFoundWhenDeleteNumberDoesNotExist() throws Exception {
        given().spec(buildRequestSpecification())
                .when()
                .delete(NUMBERS_SERVICE_URL + "/" + UUID.randomUUID())
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldReturnConflictWhenDeleteAssignedNumber() throws Exception {
        NumberDto numberDto = registerNumber();
        assignNumber(numberDto.getId());
        given().spec(buildRequestSpecification())
                .when()
                .delete(NUMBERS_SERVICE_URL + "/" + numberDto.getId())
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    public void shouldReturnBadRequestWhenDeleteNumberWithInvalidNumberId() throws Exception {
        given().spec(buildRequestSpecification())
                .when()
                .delete(NUMBERS_SERVICE_URL + "/" + "invalid-id")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
