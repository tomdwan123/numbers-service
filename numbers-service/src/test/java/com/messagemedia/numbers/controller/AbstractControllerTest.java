/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.messagemedia.framework.config.JsonConfig;
import com.messagemedia.framework.json.JsonFastMapper;
import com.messagemedia.numbers.controller.support.NumbersControllerAdvice;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.mappers.AssignmentMapper;
import com.messagemedia.numbers.repository.mappers.NumberAssignmentMapper;
import com.messagemedia.numbers.repository.mappers.NumbersMapper;
import com.messagemedia.numbers.service.CallableNumbersService;
import com.messagemedia.numbers.service.NotificationService;
import com.messagemedia.numbers.service.NumbersService;
import com.messagemedia.numbers.service.billing.BillingNotificationService;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@RunWith(DataProviderRunner.class)
public abstract class AbstractControllerTest {

    private static final JsonFastMapper MAPPER = new JsonConfig().fastMapper();

    private MockMvc mockMvc;

    @Mock
    private CallableNumbersService callableNumbersService;
    @Mock
    private NumbersService numbersService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BillingNotificationService billingNotificationService;

    private AssignmentMapper assignmentMapper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        assignmentMapper = Mappers.getMapper(AssignmentMapper.class);
        NumberAssignmentMapper numberAssignmentMapper = Mappers.getMapper(NumberAssignmentMapper.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NumbersController(numbersService,
                    notificationService, billingNotificationService,
                        Mappers.getMapper(NumbersMapper.class), assignmentMapper, numberAssignmentMapper, callableNumbersService))
                .setControllerAdvice(new NumbersControllerAdvice())
                .build();
    }

    protected String toJson(Object object) throws IOException {
        return MAPPER.toJsonString(object);
    }

    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    protected NumbersService getNumbersService() {
        return numbersService;
    }

    protected CallableNumbersService getCallableNumbersService() {
        return callableNumbersService;
    }

    protected NotificationService getNotificationService() {
        return notificationService;
    }

    protected AssignmentMapper getAssignmentMapper() {
        return assignmentMapper;
    }

    protected BillingNotificationService getBillingNotificationService() {
        return billingNotificationService;
    }

    protected ResultActions expectNumberEntityFields(ResultActions resultActions, NumberEntity numberEntity) throws Exception {
        return resultActions
                .andExpect(jsonPath("$.id").value(numberEntity.getId().toString()))
                .andExpect(jsonPath("$.providerId").value(numberEntity.getProviderId().toString()))
                .andExpect(jsonPath("$.country").value(numberEntity.getCountry()))
                .andExpect(jsonPath("$.type").value(numberEntity.getType().name()))
                .andExpect(jsonPath("$.classification").value(numberEntity.getClassification().name()))
                .andExpect(jsonPath("$.created").value(numberEntity.getCreated()))
                .andExpect(jsonPath("$.updated").value(numberEntity.getUpdated()))
                .andExpect(jsonPath("$.availableAfter").value(numberEntity.getAvailableAfter()))
                .andExpect(jsonPath("$.capabilities").value(containsInAnyOrder(numberEntity.getCapabilities()
                        .stream().map(ServiceType::name).toArray())));
    }
}
