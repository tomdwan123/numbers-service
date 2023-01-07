/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.TestData;
import com.messagemedia.numbers.service.client.models.AssignNumberRequest;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import com.messagemedia.numbers.service.client.models.Event;
import com.messagemedia.numbers.service.client.models.NumberDto;
import com.messagemedia.numbers.service.client.models.RegisterNumberRequest;
import com.messagemedia.numbers.service.client.models.UpdateNumberRequest;
import io.restassured.specification.RequestSpecification;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.messagemedia.numbers.TestData.ASSIGNMENT_URL_FORMAT;
import static com.messagemedia.numbers.TestData.createAssignmentRequestFromAccount;
import static com.messagemedia.numbers.TestData.randomAssignNumberRequest;
import static com.messagemedia.numbers.TestData.randomUpdateAssignmentRequest;
import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

public class ProvisioningNotificationIT extends AbstractControllerIT {

    private static final int AMS_API_PORT = 12345;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(AMS_API_PORT);

    @Value("${service.numbers-service.provisioning-queue.url}")
    private String provisioningQueueUrl;

    @Before
    public void init() {
        getSqs().purgeQueue(new PurgeQueueRequest(provisioningQueueUrl));
    }

    @After
    public void clean() {
        getSqs().purgeQueue(new PurgeQueueRequest(provisioningQueueUrl));
    }

    @Test
    public void shouldNotifyWhenRegisterNumber() throws JSONException, IOException {
        NumberDto numberDto = registerNumber();

        List<Message> messages = receiveMessages();
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.NUMBER_CREATED, messageBody);
        assertNumber(numberDto, messageBody);
    }

    @Test
    public void shouldNotNotifyWhenRegisterNumberFail() throws IOException {
        RegisterNumberRequest numberRequest = new RegisterNumberRequest(null, null, null, null, null, null, null);
        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(numberRequest));
        given().spec(requestSpecification).post(NUMBERS_SERVICE_URL);

        List<Message> messages = receiveMessages();
        assertEquals(0, messages.size());
    }

    @Test
    public void shouldNotifyWhenUpdateUnassignedNumber() throws JSONException, IOException {
        NumberDto numberDto = registerNumber();
        NumberDto numberDtoUpdated = updateNumber(numberDto.getId());

        List<Message> messages = receiveMessages();
        assertEquals(2, messages.size());
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.NUMBER_UPDATED, messageBody);
        assertNumber(numberDtoUpdated, messageBody);
    }

    @Test
    public void shouldNotifyWhenUpdateAssignedNumber() throws JSONException, IOException {
        NumberDto numberDto = registerNumber();
        assignNumber(numberDto.getId());
        NumberDto numberDtoUpdated = updateNumber(numberDto.getId());

        List<Message> messages = receiveMessages();
        assertEquals(3, messages.size());
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.NUMBER_UPDATED, messageBody);
        assertAssignment(numberDtoUpdated.getAssignedTo(), messageBody);
        numberDtoUpdated.setAssignedTo(null);
        assertNumber(numberDtoUpdated, messageBody);
    }

    @Test
    public void shouldNotNotifyWhenUpdateNumberFail() throws JSONException, IOException {
        NumberDto numberDto = registerNumber();
        UpdateNumberRequest updateNumberRequest = new UpdateNumberRequest(null, null, null, null, null, null);
        RequestSpecification requestSpecification = buildRequestSpecification(getObjectMapper().writeValueAsString(updateNumberRequest));
        given().spec(requestSpecification).patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberDto.getId()));

        List<Message> messages = receiveMessages();
        assertEquals(1, messages.size());
    }

    @Test
    public void shouldNotifyWhenDeleteUnassignedNumber() throws JSONException, IOException {
        NumberDto numberDto = registerNumber();
        deleteNumber(numberDto.getId());

        List<Message> messages = receiveMessages();
        assertEquals(2, messages.size());
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.NUMBER_DELETED, messageBody);
        assertNumber(numberDto, messageBody);
    }

    @Test
    public void shouldNotNotifyWhenDeleteAssignedNumber() throws IOException {
        NumberDto numberDto = registerNumber();
        assignNumber(numberDto.getId());
        deleteNumber(numberDto.getId());

        assertEquals(2, receiveMessages().size());
    }

    @Test
    public void shouldNotifyWhenAssignNumber() throws JSONException, IOException {
        AssignmentDto assignmentDto = assignNumber(registerNumber().getId());
        NumberDto updatedNumberDto = getNumber(assignmentDto.getNumberId());
        updatedNumberDto.setAssignedTo(null);

        List<Message> messages = receiveMessages();
        assertEquals(2, messages.size());
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.NUMBER_ASSIGNED, messageBody);
        assertNumber(updatedNumberDto, messageBody);
        assertAssignment(assignmentDto, messageBody);
    }

    @Test
    public void shouldNotNotifyWhenAssignNumberFail() throws IOException {
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(randomAssignNumberRequest())))
                .post(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID()));
        assertEquals(0, receiveMessages().size());
    }

    @Test
    public void shouldNotifyWhenDisassociateNumber() throws JSONException, IOException {
        AssignmentDto assignmentDto = assignNumber(registerNumber().getId());
        disassociateAssignment(assignmentDto.getNumberId());
        NumberDto updatedNumberDto = getNumber(assignmentDto.getNumberId());
        updatedNumberDto.setAssignedTo(null);

        List<Message> messages = receiveMessages();
        assertEquals(3, messages.size());
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.NUMBER_UNASSIGNED, messageBody);
        assertNumber(updatedNumberDto, messageBody);
        assertAssignment(assignmentDto, messageBody);
    }

    @Test
    public void shouldNotNotifyWhenDisassociateNumberFail() {
        disassociateAssignment(UUID.randomUUID());
        assertEquals(0, receiveMessages().size());
    }

    @Test
    public void shouldNotifyWhenUpdateAssignment() throws JSONException, IOException {
        AssignmentDto updatedAssignmentDto = updateAssignment(
                assignNumber(registerNumber().getId()).getNumberId());
        NumberDto updatedNumberDto = getNumber(updatedAssignmentDto.getNumberId());
        updatedNumberDto.setAssignedTo(null);

        List<Message> messages = receiveMessages();
        assertEquals(3, messages.size());
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.ASSIGNMENT_UPDATED, messageBody);
        assertNumber(updatedNumberDto, messageBody);
        assertAssignment(updatedAssignmentDto, messageBody);
    }

    @Test
    public void shouldNotNotifyWhenUpdateAssignmentFail() throws IOException {
        given().spec(buildRequestSpecification(getObjectMapper().writeValueAsString(randomUpdateAssignmentRequest())))
                .patch(String.format(ASSIGNMENT_URL_FORMAT, UUID.randomUUID().toString()));
        assertEquals(0, receiveMessages().size());
    }

    @Test
    public void shouldSendNotificationWhenReassign() throws Exception {
        TestData.mockUpAmsEndpoint();
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
                .statusCode(HttpStatus.OK.value());

        final NumberDto numberDb = this.getNumber(numberDto.getId());

        AssignmentDto assignmentDto = numberDb.getAssignedTo();

        List<Message> messages = receiveMessages();
        assertEquals(3, messages.size());
        String messageBody = messages.get(0).getBody();
        assertEvent(Event.NUMBER_REASSIGNED, messageBody);
        numberDb.setAssignedTo(null);
        assertNumber(numberDb, messageBody);
        assertAssignment(assignmentDto, messageBody);
    }

    private List<Message> receiveMessages() {
        return receiveMessages(provisioningQueueUrl);
    }

}
