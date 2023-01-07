/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.messagemedia.framework.config.BuildVersion;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.numbers.config.WorkerContext;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.service.client.models.*;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.util.List;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.messagemedia.framework.config.impl.SpringProfileCalculator.DEFAULT_ENVIRONMENT;
import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

@ActiveProfiles(DEFAULT_ENVIRONMENT)
@WebAppConfiguration
@ContextConfiguration(classes = WorkerContext.class)
@RunWith(DataProviderSpringRunner.class)
public abstract class AbstractControllerIT {
    private static final int PORT = Integer.parseInt(new BuildVersion().getValue(WEB_CONTAINER_PORT));

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmazonSQS sqs;

    @Autowired
    private NumbersRepository numberRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Before
    public void setUp() throws Exception {
        RestAssured.port = PORT;
        cleanUpDatabase();
        objectMapper.registerModule(new JavaTimeModule());
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

    protected ValidatableResponse expectNumberDtoFields(ValidatableResponse validatableResponse, NumberDto numberDto) {
        //not include availableAfter field because has specific rule
        return validatableResponse.body("id", equalTo(numberDto.getId().toString()))
                .body("providerId", equalTo(numberDto.getProviderId().toString()))
                .body("phoneNumber", equalTo(numberDto.getPhoneNumber()))
                .body("country", equalTo(numberDto.getCountry()))
                .body("type", equalTo(numberDto.getType().name()))
                .body("classification", equalTo(numberDto.getClassification().name()))
                .body("created", not(isEmptyString()))
                .body("updated", not(isEmptyString()))
                .body("capabilities", containsInAnyOrder(numberDto.getCapabilities().stream().map(ServiceType::name).toArray()));
    }

    protected void expectNumberInArrayResult(ValidatableResponse validatableResponse, int index, NumberDto numberDto) {
        String prefix = String.format("numbers[%d]", index);
        checkNumber(prefix, validatableResponse, numberDto);
        if (numberDto.getAssignedTo() != null) {
            validatableResponse.body(prefix + ".availableAfter", nullValue());
            checkAssignment(prefix + ".assignedTo", validatableResponse, numberDto.getAssignedTo());

        } else {
            validatableResponse.body(prefix + ".availableAfter", notNullValue());
        }
    }

    protected void checkNumber(String prefix, ValidatableResponse validatableResponse, NumberDto numberDto) {
        String[] caps = numberDto.getCapabilities().stream().map(Enum::name).toArray(String[]::new);
        validatableResponse.body(prefix + ".id", equalTo(numberDto.getId().toString()))
                .body(prefix + ".classification", equalTo(numberDto.getClassification().toString()))
                .body(prefix + ".country", equalTo(numberDto.getCountry()))
                .body(prefix + ".phoneNumber", equalTo(numberDto.getPhoneNumber()))
                .body(prefix + ".providerId", equalTo(numberDto.getProviderId().toString()))
                .body(prefix + ".type", equalTo(numberDto.getType().toString()))
                .body(prefix + ".capabilities", hasItems(caps));
    }

    protected void checkAssignment(String prefix, ValidatableResponse validatableResponse, AssignmentDto assignmentDto) {
        validatableResponse.body(prefix + ".id", equalTo(assignmentDto.getId().toString()));
        validatableResponse.body(prefix + ".accountId", equalTo(assignmentDto.getAccountId()));
        validatableResponse.body(prefix + ".callbackUrl", equalTo(assignmentDto.getCallbackUrl()));
        validatableResponse.body(prefix + ".metadata", equalTo(assignmentDto.getMetadata()));
        validatableResponse.body(prefix + ".vendorId", equalTo(assignmentDto.getVendorId()));
        validatableResponse.body(prefix + ".numberId", equalTo(assignmentDto.getNumberId().toString()));
    }

    protected NumberDto registerNumber() throws JsonProcessingException {
        RegisterNumberRequest numberRequest = randomRegisterNumberRequest();
        return registerNumber(numberRequest);
    }

    protected NumberDto registerUsTollFreeNumber() throws JsonProcessingException {
        RegisterNumberRequest numberRequest = randomRegisterUsTollFreeNumberRequest();
        return registerNumber(numberRequest);
    }

    protected NumberDto registerNumber(RegisterNumberRequest numberRequest) throws JsonProcessingException {
        RequestSpecification requestSpecification = buildRequestSpecification(objectMapper.writeValueAsString(numberRequest));

        return given().spec(requestSpecification)
                .post(NUMBERS_SERVICE_URL)
                .as(NumberDto.class);
    }

    protected NumberDto getNumber(UUID numberId) throws JsonProcessingException {
        return given().spec(buildRequestSpecification())
                .get(String.format(NUMBERS_SERVICE_URL + "/%s", numberId))
                .as(NumberDto.class);
    }

    protected NumberDto updateNumber(UUID numberId) throws JsonProcessingException {
        return updateNumber(numberId, null);
    }

    protected NumberDto updateNumber(UUID numberId, UpdateNumberRequest request) throws JsonProcessingException {
        if (null == request) {
            request = randomUpdateNumberRequestWithoutAvailableAfter();
        }
        RequestSpecification requestSpecification = buildRequestSpecification(objectMapper.writeValueAsString(request));

        return given().spec(requestSpecification)
                .patch(String.format(NUMBERS_SERVICE_URL + "/%s", numberId))
                .as(NumberDto.class);
    }

    protected void deleteNumber(UUID numberId) {
        given().spec(buildRequestSpecification())
                .delete(String.format(NUMBERS_SERVICE_URL + "/%s", numberId));
    }

    protected AssignmentDto assignNumber(UUID numberId) throws JsonProcessingException {
        return assignNumber(numberId, randomAssignNumberRequest());
    }

    protected AssignmentDto assignNumber(UUID numberId, AssignNumberRequest assignNumberRequest) throws JsonProcessingException {
        RequestSpecification requestSpecification = buildRequestSpecification(objectMapper.writeValueAsString(assignNumberRequest));
        return given().spec(requestSpecification)
                .post(String.format(ASSIGNMENT_URL_FORMAT, numberId))
                .as(AssignmentDto.class);
    }

    protected AssignmentDto updateAssignment(UUID numberId) throws JsonProcessingException {
        return given().spec(buildRequestSpecification(objectMapper.writeValueAsString(randomUpdateAssignmentRequest())))
                .patch(String.format(ASSIGNMENT_URL_FORMAT, numberId))
                .as(AssignmentDto.class);
    }

    protected void disassociateAssignment(UUID numberId) {
        given().spec(buildRequestSpecification())
                .delete(String.format(ASSIGNMENT_URL_FORMAT, numberId));
    }

    protected RequestSpecification buildRequestSpecification(String body) {
        return buildRequestSpecification().body(body);
    }

    protected RequestSpecification buildRequestSpecification() {
        return new RequestSpecBuilder()
                .setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .build();
    }

    /**
     * the matcher which verifies that the days between now() & json datetime in payload
     * This will be removed if exists popular library support this.
     */
    public static BaseMatcher<String> isAfterNow(long expectedDays) {
        return new BaseMatcher<String>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(String.valueOf(expectedDays));
            }

            @Override
            public boolean matches(Object item) {
                if (item == null) {
                    return false;
                }
                OffsetDateTime payloadDateTime = OffsetDateTime.parse(item.toString(), DateTimeFormatter.ISO_DATE_TIME);
                return ChronoUnit.DAYS.between(
                        OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS),
                        payloadDateTime) == expectedDays;
            }
        };
    }

    protected AmazonSQS getSqs() {
        return sqs;
    }

    protected void assertEvent(Event event, String messageBody) throws JSONException {
        assertThat(messageBody, CoreMatchers.containsString("eventId"));
        assertThat(messageBody, CoreMatchers.containsString("eventTimestamp"));
        JSONAssert.assertEquals(String.format("{\"event\":\"%s\"}", event.name()), messageBody, LENIENT);
    }

    protected void assertNumber(NumberDto numberDto, String messageBody) throws JSONException, IOException {
        JSONAssert.assertEquals(String.format("{\"number\":%s}", objectMapper.writeValueAsString(numberDto)), messageBody, LENIENT);
    }

    protected void assertAssignment(AssignmentDto assignmentDto, String messageBody) throws JSONException, IOException {
        JSONAssert.assertEquals(String.format("{\"assignment\":%s}", objectMapper.writeValueAsString(assignmentDto)), messageBody, LENIENT);
    }

    protected List<Message> receiveMessages(String queueUrl) {
        ReceiveMessageRequest request = new ReceiveMessageRequest();
        request.setQueueUrl(queueUrl);
        request.setMaxNumberOfMessages(5);
        return sqs.receiveMessage(request).getMessages();
    }
}
