/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.framework.config.JsonConfig;
import com.messagemedia.framework.json.JsonFastMapper;
import com.messagemedia.framework.logging.Logger;
import com.messagemedia.framework.logging.LoggerFactory;
import com.messagemedia.numbers.exception.NumberCapabilityNotAvailableException;
import com.messagemedia.numbers.exception.NumberNotFoundException;
import com.messagemedia.numbers.model.dto.CallableNumberDto;
import com.messagemedia.numbers.model.dto.CallableNumberCustomerResponse;
import com.messagemedia.numbers.model.dto.CallableNumberCustomerRequest;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileCreateResponse;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileCreateRequest;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileStep;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileStepDetail;
import com.messagemedia.numbers.model.dto.CallableNumberCreateResponse;
import com.messagemedia.numbers.model.dto.CallableNumberCreateRequest;
import com.messagemedia.numbers.model.dto.CallableNumberUpdateRequest;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.ServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Service
public class CallableNumbersService {

    private static final JsonFastMapper MAPPER = new JsonConfig().fastMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(CallableNumbersService.class);
    private static final String NO_CALLABLE_NUMBERS_URL_VALUE = "NONE";
    private static final String INVALID_CALLABLE_NUMBERS_URL = "Invalid callable numbers url";

    private final String callableNumbersUrl;
    private final NumbersRepository numbersRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public CallableNumbersService(NumbersRepository numbersRepository,
                                  RestTemplate restTemplate,
                                  @Value("${service.numbers-service.callable-numbers.url:NONE}") String callableNumbersUrl) {
        this.callableNumbersUrl = callableNumbersUrl;
        this.numbersRepository = numbersRepository;
        this.restTemplate = restTemplate;
    }

    public CallableNumberDto getCallableNumber(UUID numberId) {
        String responseString = getCallableNumberRaw(numberId);

        try {
            return MAPPER.readFrom(responseString, CallableNumberDto.class);
        } catch (Exception e) {
            LOGGER.error("Failed to parse JSON response: {}", e.getMessage());
        }

        return null;
    }

    private String getCallableNumberRaw(UUID numberId) throws NumberNotFoundException {
        if (NO_CALLABLE_NUMBERS_URL_VALUE.equals(callableNumbersUrl))  {
            LOGGER.warn(INVALID_CALLABLE_NUMBERS_URL);
            return null;
        }

        NumberEntity numberEntity = getAndValidateNumberEntity(numberId);

        try {
            String url = String.format("%s/numbers/%s", callableNumbersUrl, numberEntity.getPhoneNumber());
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            LOGGER.warnWithReason("Failed to get callable number", e.getMessage(), e);
        }

        return null;
    }

    public void deleteCallableNumber(UUID numberId) {
        if (NO_CALLABLE_NUMBERS_URL_VALUE.equals(callableNumbersUrl))  {
            LOGGER.warn(INVALID_CALLABLE_NUMBERS_URL);
            return;
        }

        NumberEntity numberEntity = getAndValidateNumberEntity(numberId);

        try {
            String url = String.format("%s/numbers/%s", callableNumbersUrl, numberEntity.getPhoneNumber());
            restTemplate.delete(url);
        } catch (HttpStatusCodeException e) {
            // catch only the http not found exception
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                LOGGER.warnWithReason("Failed to delete callable number", e.getMessage(), e);

                throw e;
            }
        }
    }

    public CallableNumberCustomerResponse createCustomer(String vendorAccountId) {
        if (NO_CALLABLE_NUMBERS_URL_VALUE.equals(callableNumbersUrl))  {
            LOGGER.warn(INVALID_CALLABLE_NUMBERS_URL);
            return null;
        }

        try {
            CallableNumberCustomerRequest request = new CallableNumberCustomerRequest(vendorAccountId, vendorAccountId);
            String url = String.format("%s/customers", callableNumbersUrl);
            String callableNumberCreateCustomer = restTemplate.postForObject(url, request, String.class);
            try {
                return MAPPER.readFrom(callableNumberCreateCustomer, CallableNumberCustomerResponse.class);
            } catch (Exception e) {
                return null;
            }
        } catch (HttpStatusCodeException e) {
            // catch only the 409 conflict exception
            if (e.getStatusCode() != HttpStatus.CONFLICT) {
                throw e;
            } else {
                LOGGER.warnWithReason("Customer already exists", e.getMessage(), e);
                return null;
            }
        }
    }

    public CallableNumberRoutingProfileCreateResponse createRoutingProfile(String number, String vendorAccountId) {
        if (NO_CALLABLE_NUMBERS_URL_VALUE.equals(callableNumbersUrl))  {
            LOGGER.warn(INVALID_CALLABLE_NUMBERS_URL);
            return null;
        }

        //Set Steps Details
        CallableNumberRoutingProfileStepDetail stepsDetails = new CallableNumberRoutingProfileStepDetail();
        stepsDetails.setNumber(number);
        //Set Steps Routing Type
        CallableNumberRoutingProfileStep steps = new CallableNumberRoutingProfileStep(
                "direct",
                stepsDetails
        );

        List<CallableNumberRoutingProfileStep> stepsList = new ArrayList<>();
        stepsList.add(0, steps);
        CallableNumberRoutingProfileCreateRequest request = new CallableNumberRoutingProfileCreateRequest(
                vendorAccountId, vendorAccountId + "Route", stepsList);


        String url = String.format("%s/routing-profiles", callableNumbersUrl);
        String callableNumberRoutingProfileCreateResponse = restTemplate.postForObject(url, request, String.class);
        try {
            return MAPPER.readFrom(callableNumberRoutingProfileCreateResponse, CallableNumberRoutingProfileCreateResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    public CallableNumberCreateResponse createCallableNumber(UUID numberId, String vendorAccountId, String routingProfileId) {
        if (NO_CALLABLE_NUMBERS_URL_VALUE.equals(callableNumbersUrl))  {
            LOGGER.warn(INVALID_CALLABLE_NUMBERS_URL);
            return null;
        }

        NumberEntity numberEntity = getAndValidateNumberEntity(numberId);

        CallableNumberCreateRequest createNumberRequest = new CallableNumberCreateRequest(
                vendorAccountId, numberEntity.getPhoneNumber(), routingProfileId);

        String url = String.format("%s/numbers", callableNumbersUrl);
        String createCallableNumberResponse = restTemplate.postForObject(url, createNumberRequest, String.class);
        try {
            return MAPPER.readFrom(createCallableNumberResponse, CallableNumberCreateResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateCallableNumber(UUID numberId, String vendorAccountId, String routingProfileId) {
        if (NO_CALLABLE_NUMBERS_URL_VALUE.equals(callableNumbersUrl))  {
            LOGGER.warn(INVALID_CALLABLE_NUMBERS_URL);
            return;
        }

        NumberEntity numberEntity = getAndValidateNumberEntity(numberId);

        CallableNumberUpdateRequest updateCallableNumberRequest = new CallableNumberUpdateRequest(routingProfileId, vendorAccountId);

        String url = String.format("%s/numbers/%s", callableNumbersUrl, numberEntity.getPhoneNumber());
        restTemplate.put(url, updateCallableNumberRequest);
    }

    private NumberEntity getAndValidateNumberEntity(UUID numberId) {
        Optional <NumberEntity> numberEntity = numbersRepository.findById(numberId);

        if (!numberEntity.isPresent()) {
            throw new NumberNotFoundException(numberId);
        }

        if (!numberEntity.get().getCapabilities().contains(ServiceType.CALL)) {
            throw new NumberCapabilityNotAvailableException(numberId, ServiceType.CALL);
        }

        return numberEntity.get();
    }
}
