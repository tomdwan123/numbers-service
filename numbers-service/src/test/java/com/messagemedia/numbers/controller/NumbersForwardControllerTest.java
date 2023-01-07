/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
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
import com.messagemedia.numbers.service.CallableNumbersService;
import com.messagemedia.numbers.service.NumbersService;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.model.dto.*;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.service.client.models.NumberForwardDto;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.messagemedia.numbers.TestData.*;
import static com.messagemedia.numbers.TestDataCreateCallableNumber.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(DataProviderRunner.class)
public class NumbersForwardControllerTest {

    private static final VendorAccountId VENDOR_ACCOUNT_ID = new VendorAccountId("vendorId", "accountId");
    private static final NumberEntity NUMBER_ENTITY_ASSIGNED = randomAssignedNumberEntityWithCallCapability(VENDOR_ACCOUNT_ID);
    private static final NumberEntity NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY =
            randomAssignedNumberEntityWithoutCallCapability(VENDOR_ACCOUNT_ID);
    private static final NumberEntity NUMBER_ENTITY = randomUnassignedNumberEntityWithCallCapability();
    private static final JsonFastMapper MAPPER = new JsonConfig().fastMapper();

    private MockMvc mockMvc;

    @Mock
    private CallableNumbersService callableNumbersService;
    @Mock
    private NumbersService numbersService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NumbersForwardController(numbersService, callableNumbersService))
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

    @Mock
    private NumbersRepository numbersRepository;

    @Test
    public void shouldGetForwardNumberSuccess() throws Exception {
        numbersRepository.save(NUMBER_ENTITY_ASSIGNED);

        NumberForwardDto createCallableRequest = new NumberForwardDto("+61491570162");
        CallableNumberRoutingProfileStepDetail callableNumberRoutingProfileStepDetail = randomCallableNumberRoutingProfileStepDetail();
        callableNumberRoutingProfileStepDetail.setNumber(createCallableRequest.getDestination());
        CallableNumberRoutingProfileStep callableNumberRoutingProfileStep = randomCallableNumberRoutingProfileStep();
        callableNumberRoutingProfileStep.setDirectDetails(callableNumberRoutingProfileStepDetail);
        CallableNumberRoutingProfileCreateResponse callableNumberRoutingProfileCreateResponse = randomCallableNumberRoutingProfileCreateResponse();
        List<CallableNumberRoutingProfileStep> stepsList = new ArrayList<>();
        stepsList.add(0, callableNumberRoutingProfileStep);

        CallableNumberRoutingProfile callableNumberRoutingProfile = new CallableNumberRoutingProfile(
                callableNumberRoutingProfileCreateResponse.getId(), callableNumberRoutingProfileCreateResponse.getName(), stepsList);

        CallableNumberDto callableNumberDto = randomCallableNumberDto();
        callableNumberDto.setRoutingProfile(callableNumberRoutingProfile);
        when(getCallableNumbersService().getCallableNumber(NUMBER_ENTITY_ASSIGNED.getId())).thenReturn(callableNumberDto);
        assertEquals(callableNumberDto, getCallableNumbersService().getCallableNumber(NUMBER_ENTITY_ASSIGNED.getId()));
        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders.get(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY_ASSIGNED.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.destination", is(callableNumberDto.getRoutingProfile().getNumber())));
    }

    @Test
    public void shouldGetForwardNumberSuccessNull() throws Exception {
        numbersRepository.save(NUMBER_ENTITY_ASSIGNED);
        CallableNumberDto callableNumberDto = randomCallableNumberDto();
        when(getCallableNumbersService().getCallableNumber(NUMBER_ENTITY_ASSIGNED.getId())).thenReturn(callableNumberDto);
        assertEquals(callableNumberDto, getCallableNumbersService().getCallableNumber(NUMBER_ENTITY_ASSIGNED.getId()));
        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders.get(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.destination", is(nullValue())));
    }

    @Test
    public void shouldDeleteForwardNumberSuccess() throws Exception {
        numbersRepository.save(NUMBER_ENTITY);
        doNothing().when(getCallableNumbersService()).deleteCallableNumber(any(UUID.class));
        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders.delete(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        result.andExpect(status().isNoContent());
    }

    @Test
    public void shouldCreateForwardNumberSuccess() throws Exception {
        numbersRepository.save(NUMBER_ENTITY_ASSIGNED);
        NumberForwardDto createCallableRequest = new NumberForwardDto("+61491570162");
        when(getNumbersService().getNumber(NUMBER_ENTITY_ASSIGNED.getId())).thenReturn(NUMBER_ENTITY_ASSIGNED);

        when(getCallableNumbersService().getCallableNumber(
                NUMBER_ENTITY_ASSIGNED.getId()))
                .thenReturn(null);

        CallableNumberCustomerResponse callableNumberCustomerResponse = randomCallableNumberCustomerResponse();

        CallableNumberRoutingProfileStepDetail callableNumberRoutingProfileStepDetail = randomCallableNumberRoutingProfileStepDetail();
        callableNumberRoutingProfileStepDetail.setNumber(createCallableRequest.getDestination());
        CallableNumberRoutingProfileStep callableNumberRoutingProfileStep = randomCallableNumberRoutingProfileStep();
        callableNumberRoutingProfileStep.setDirectDetails(callableNumberRoutingProfileStepDetail);
        CallableNumberRoutingProfileCreateResponse callableNumberRoutingProfileCreateResponse = randomCallableNumberRoutingProfileCreateResponse();
        List<CallableNumberRoutingProfileStep> stepsList = new ArrayList<>();
        stepsList.add(0, callableNumberRoutingProfileStep);

        CallableNumberRoutingProfile callableNumberRoutingProfile = new CallableNumberRoutingProfile(
                callableNumberRoutingProfileCreateResponse.getId(), callableNumberRoutingProfileCreateResponse.getName(), stepsList);

        when(getCallableNumbersService().createCustomer(VENDOR_ACCOUNT_ID.toColonString())).thenReturn(callableNumberCustomerResponse);
        when(getCallableNumbersService().createRoutingProfile(createCallableRequest.getDestination(), VENDOR_ACCOUNT_ID.toColonString()))
                .thenReturn(callableNumberRoutingProfileCreateResponse);

        CallableNumberCreateResponse callableNumberCreateResponse = callableNumberCreateResponse(
                "+61491570162", VENDOR_ACCOUNT_ID.toColonString(), callableNumberRoutingProfile);
        when(getCallableNumbersService().createCallableNumber(
                NUMBER_ENTITY_ASSIGNED.getId(), VENDOR_ACCOUNT_ID.toColonString(), "4"))
                .thenReturn(callableNumberCreateResponse);

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders.post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY_ASSIGNED.getId()))
                .content(toJson(createCallableRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.destination", is("+61491570162")));
    }

    @Test
    public void shouldCreateForwardNumberIfUnassignedNotFound() throws Exception {
        numbersRepository.save(NUMBER_ENTITY);
        NumberForwardDto createCallableRequest = new NumberForwardDto("+61491570162");
        when(getNumbersService().getNumber(NUMBER_ENTITY.getId())).thenReturn(NUMBER_ENTITY);

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders.post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY.getId()))
                .content(toJson(createCallableRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        result.andExpect(status().isNotFound());
    }

    @Test
    public void shouldCreateForwardNumberNoCallCapabilityBadRequest() throws Exception {
        numbersRepository.save(NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY);
        when(getNumbersService().getNumber(NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY.getId()))
                .thenReturn(NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY);
        NumberForwardDto createCallableRequest = new NumberForwardDto();
        createCallableRequest.setDestination("+123456789");

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders
                .post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY_ASSIGNED_WITHOUT_CALL_CAPABILITY.getId()))
                .content(toJson(createCallableRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON)
        );
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateForwardNumberSuccess() throws Exception {
        numbersRepository.save(NUMBER_ENTITY_ASSIGNED);
        NumberForwardDto createCallableRequest = new NumberForwardDto("+61491570163");
        when(getNumbersService().getNumber(NUMBER_ENTITY_ASSIGNED.getId())).thenReturn(NUMBER_ENTITY_ASSIGNED);

        CallableNumberDto callableNumberDto = randomCallableNumberDto();
        callableNumberDto.getRoutingProfile().getSteps().get(0).getDirectDetails().setNumber("+61491570162");
        when(getCallableNumbersService().getCallableNumber(
                NUMBER_ENTITY_ASSIGNED.getId()))
                .thenReturn(callableNumberDto);

        CallableNumberRoutingProfileStepDetail callableNumberRoutingProfileStepDetail = randomCallableNumberRoutingProfileStepDetail();
        callableNumberRoutingProfileStepDetail.setNumber(createCallableRequest.getDestination());
        CallableNumberRoutingProfileStep callableNumberRoutingProfileStep = randomCallableNumberRoutingProfileStep();
        callableNumberRoutingProfileStep.setDirectDetails(callableNumberRoutingProfileStepDetail);
        CallableNumberRoutingProfileCreateResponse callableNumberRoutingProfileCreateResponse = randomCallableNumberRoutingProfileCreateResponse();
        List<CallableNumberRoutingProfileStep> stepsList = new ArrayList<>();
        stepsList.add(0, callableNumberRoutingProfileStep);

        when(getCallableNumbersService().createRoutingProfile(createCallableRequest.getDestination(), VENDOR_ACCOUNT_ID.toColonString()))
                .thenReturn(callableNumberRoutingProfileCreateResponse);

        ResultActions result = getMockMvc().perform(MockMvcRequestBuilders.post(String.format(FORWARD_URL_FORMAT, NUMBER_ENTITY_ASSIGNED.getId()))
                .content(toJson(createCallableRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        result.andExpect(status().isOk());
    }
}
