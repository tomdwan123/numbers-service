/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.messagemedia.numbers.exception.NumberCapabilityNotAvailableException;
import com.messagemedia.numbers.exception.NumberNotAssignedException;
import com.messagemedia.numbers.exception.NumberNotFoundException;
import com.messagemedia.numbers.model.dto.CallableNumberDto;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileCreateResponse;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.CallableNumbersService;
import com.messagemedia.numbers.service.NumbersService;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.messagemedia.numbers.service.client.models.NumberForwardDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;

@RestController
@RequestMapping(value = NUMBERS_SERVICE_URL, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class NumbersForwardController {

    private static final String FORWARD_SERVICE_URL = "/{id}/forward";

    private final CallableNumbersService callableNumbersService;
    private final NumbersService numbersService;

    @Autowired
    public NumbersForwardController(NumbersService numbersService,
                                    CallableNumbersService callableNumbersService) {
        this.callableNumbersService = callableNumbersService;
        this.numbersService = numbersService;
    }

    @GetMapping(path = FORWARD_SERVICE_URL)
    @ResponseStatus(HttpStatus.OK)
    public NumberForwardDto getForwardNumber(@PathVariable("id") UUID numberId) throws NumberNotFoundException {
        CallableNumberDto callableNumberDto = callableNumbersService.getCallableNumber(numberId);
        NumberForwardDto numberForwardDto = new NumberForwardDto();

        if (callableNumberDto == null || callableNumberDto.getRoutingProfile() == null) {
            numberForwardDto.setDestination(null);
        } else {
            numberForwardDto.setDestination(callableNumberDto.getRoutingProfile().getNumber());
        }

        return numberForwardDto;
    }

    @DeleteMapping(path = FORWARD_SERVICE_URL)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForwardNumber(@PathVariable("id") UUID numberId) throws NumberNotFoundException {
        callableNumbersService.deleteCallableNumber(numberId);
    }

    @PostMapping(path = FORWARD_SERVICE_URL)
    public ResponseEntity<NumberForwardDto> createForwardNumber(@PathVariable("id") UUID numberId,
                                                                @RequestBody @Valid NumberForwardDto numberForwardCreateRequest) {
        NumberEntity numberEntity = numbersService.getNumber(numberId);
        if (numberEntity.getAssignedTo() == null) {
            throw new NumberNotAssignedException(numberId);
        }
        if (!numberEntity.getCapabilities().contains(ServiceType.CALL)) {
            throw new NumberCapabilityNotAvailableException(numberId, ServiceType.CALL);
        }
        //Get and Set VendorAccountId
        String vendorAccountId = numberEntity.getVendorAccountIdString();

        CallableNumberDto callableNumberDto = callableNumbersService.getCallableNumber(numberId);
        if (callableNumberDto == null) {
            callableNumbersService.createCustomer(vendorAccountId);
            CallableNumberRoutingProfileCreateResponse routingProfile = callableNumbersService.createRoutingProfile(
                    numberForwardCreateRequest.getDestination(), vendorAccountId);
            callableNumbersService.createCallableNumber(numberId, vendorAccountId, routingProfile.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(new NumberForwardDto(numberForwardCreateRequest.getDestination()));
        } else {
            if (!callableNumberDto.getRoutingProfile().getNumber().equals(numberForwardCreateRequest.getDestination())) {
                CallableNumberRoutingProfileCreateResponse routingProfile = callableNumbersService.createRoutingProfile(
                        numberForwardCreateRequest.getDestination(),  vendorAccountId);
                callableNumbersService.updateCallableNumber(numberId, vendorAccountId, routingProfile.getId());
                return ResponseEntity.status(HttpStatus.OK).body(new NumberForwardDto(numberForwardCreateRequest.getDestination()));
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(new NumberForwardDto(numberForwardCreateRequest.getDestination()));
    }
}
