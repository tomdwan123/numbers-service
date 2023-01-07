/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.exception.EventNotificationPublishingFailedException;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.mappers.AssignmentMapper;
import com.messagemedia.numbers.repository.mappers.NumberAssignmentMapper;
import com.messagemedia.numbers.repository.mappers.NumbersMapper;
import com.messagemedia.numbers.service.CallableNumbersService;
import com.messagemedia.numbers.service.NumberListResult;
import com.messagemedia.numbers.service.NumbersService;
import com.messagemedia.numbers.service.NotificationService;
import com.messagemedia.numbers.service.billing.BillingNotificationService;
import com.messagemedia.numbers.service.client.models.*;
import com.messagemedia.service.accountmanagement.client.exception.ServiceAccountManagementException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import javax.validation.Valid;
import java.util.UUID;

import static com.messagemedia.numbers.controller.NumbersController.NUMBERS_SERVICE_URL;

@RestController
@RequestMapping(value = NUMBERS_SERVICE_URL, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class NumbersController {

    static final String NUMBERS_SERVICE_URL = "/v1/numbers";
    private static final String ASSIGNMENT_SERVICE_URL = "/{id}/assignment";
    private static final String ASSIGNMENTS_SERVICE_URL = "/assignments";

    private final NumbersService numbersService;
    private final NotificationService notificationService;
    private final BillingNotificationService billingNotificationService;
    private final NumbersMapper numbersMapper;
    private final AssignmentMapper assignmentMapper;
    private final NumberAssignmentMapper numberAssignmentMapper;
    private final CallableNumbersService callableNumbersService;

    @Autowired
    public NumbersController(NumbersService numbersService,
                             NotificationService notificationService,
                             BillingNotificationService billingNotificationService,
                             NumbersMapper numbersMapper,
                             AssignmentMapper assignmentMapper,
                             NumberAssignmentMapper numberAssignmentMapper,
                             CallableNumbersService callableNumbersService) {
        this.numbersService = numbersService;
        this.notificationService = notificationService;
        this.billingNotificationService = billingNotificationService;
        this.numbersMapper = numbersMapper;
        this.assignmentMapper = assignmentMapper;
        this.numberAssignmentMapper = numberAssignmentMapper;
        this.callableNumbersService = callableNumbersService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public NumberListResponse getNumbers(@Valid NumberSearchRequest request) {
        NumberListResult result = numbersService.getNumbers(request);
        return new NumberListResponse(
                numbersMapper.toNumberDtoList(result.getNumbers()),
                new PageMetadata(request.getPageSize(), result.getToken())
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NumberDto registerNumber(@Valid @RequestBody RegisterNumberRequest numberRequest) throws EventNotificationPublishingFailedException {
        NumberDto numberDto = numbersMapper.toNumberDto(numbersService.registerNumber(numbersMapper.toNumberEntity(numberRequest)));
        notificationService.push(Event.NUMBER_CREATED, numberDto);
        return numberDto;
    }

    @GetMapping(path = ASSIGNMENTS_SERVICE_URL)
    @ResponseStatus(HttpStatus.OK)
    public NumberAssignmentListResponse getAssignments(@Valid NumberAssignmentSearchRequest request) {
        NumberListResult result = numbersService.getNumberAssignments(request);
        return new NumberAssignmentListResponse(
            numberAssignmentMapper.toNumberAssignmentDtoList(result.getNumbers()),
            new PageMetadata(request.getPageSize(), result.getToken())
        );
    }

    @PostMapping(path = ASSIGNMENT_SERVICE_URL)
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDto assignNumber(@Valid @RequestBody AssignNumberRequest assignNumberRequest,
                                      @PathVariable("id") UUID numberId) throws EventNotificationPublishingFailedException {
        addVendorAccountMdcValues(assignNumberRequest.getVendorId(), assignNumberRequest.getAccountId());
        try {
            AssignmentEntity assignmentEntity = assignmentMapper.toAssignmentEntity(assignNumberRequest);
            AssignmentDto assignmentDto = assignmentMapper.toAssignmentDto(numbersService.assignNumberToAccount(numberId, assignmentEntity));
            NumberEntity numberEntity = numbersService.getNumber(numberId);
            NumberDto numberDto = numbersMapper.toNumberDto(numberEntity);
            notificationService.push(Event.NUMBER_ASSIGNED, numberDto);
            billingNotificationService
                    .sendAddNotification(new VendorAccountId(assignmentDto.getVendorId(), assignmentDto.getAccountId()), numberEntity);
            return assignmentDto;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping(path = ASSIGNMENT_SERVICE_URL)
    @ResponseStatus(HttpStatus.OK)
    public AssignmentDto loadAssignmentDetails(@PathVariable("id") UUID numberId) {
        return assignmentMapper.toAssignmentDto(numbersService.loadAssignmentDetailsByNumberId(numberId));
    }

    @DeleteMapping(path = ASSIGNMENT_SERVICE_URL)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disassociateAssignment(@PathVariable("id") UUID numberId) throws EventNotificationPublishingFailedException, HttpStatusCodeException {
        AssignmentDto assignmentDto = assignmentMapper.toAssignmentDto(numbersService.disassociateAssignment(numberId));
        addVendorAccountMdcValues(assignmentDto.getVendorId(), assignmentDto.getAccountId());
        NumberEntity numberEntity = numbersService.getNumber(numberId);
        deleteCallableNumber(numberEntity);
        try {
            NumberDto numberDto = numbersMapper.toNumberDto(numberEntity);
            numberDto.setAssignedTo(assignmentDto);
            notificationService.push(Event.NUMBER_UNASSIGNED, numberDto);
            billingNotificationService
                    .sendRemoveNotification(new VendorAccountId(assignmentDto.getVendorId(), assignmentDto.getAccountId()), numberEntity);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public NumberDto getNumber(@PathVariable("id") UUID numberId) {
        return numbersMapper.toNumberDto(numbersService.getNumber(numberId));
    }

    @PatchMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public NumberDto updateNumber(@Valid @RequestBody UpdateNumberRequest numberRequest,
                                  @PathVariable("id") UUID numberId) throws EventNotificationPublishingFailedException {
        NumberDto numberDto = numbersMapper.toNumberDto(numbersService.updateNumber(numberId, numberRequest));
        notificationService.push(Event.NUMBER_UPDATED, numberDto);
        return numberDto;
    }

    @PatchMapping(path = ASSIGNMENT_SERVICE_URL)
    @ResponseStatus(HttpStatus.OK)
    public AssignmentDto updateAssignment(@Valid @RequestBody UpdateAssignmentRequest assignmentRequest,
                                          @PathVariable("id") UUID numberId) throws EventNotificationPublishingFailedException {
        AssignmentDto assignmentDto = assignmentMapper.toAssignmentDto(numbersService.updateAssignment(numberId, assignmentRequest));
        NumberDto numberDto = numbersMapper.toNumberDto(numbersService.getNumber(numberId));
        notificationService.push(Event.ASSIGNMENT_UPDATED, numberDto);
        return assignmentDto;
    }

    @PutMapping(path = ASSIGNMENT_SERVICE_URL)
    @ResponseStatus(HttpStatus.OK)
    public AssignmentDto reassignNumber(@PathVariable("id") UUID numberId, @Valid @RequestBody AssignNumberRequest assignNumberRequest)
                                    throws ServiceAccountManagementException {
        AssignmentEntity assignmentEntity = assignmentMapper.toAssignmentEntity(assignNumberRequest);
        final AssignmentEntity newAssignmentEntity = numbersService.reassignNumber(numberId, assignmentEntity);
        newAssignmentEntity.getNumberEntity().setAssignedTo(newAssignmentEntity);
        final NumberDto numberDto = this.numbersMapper.toNumberDto(newAssignmentEntity.getNumberEntity());
        notificationService.push(Event.NUMBER_REASSIGNED, numberDto);
        return assignmentMapper.toAssignmentDto(newAssignmentEntity);
    }

    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNumber(@PathVariable("id") UUID numberId) throws EventNotificationPublishingFailedException {
        NumberDto numberDto = numbersMapper.toNumberDto(numbersService.deleteNumber(numberId));
        notificationService.push(Event.NUMBER_DELETED, numberDto);
    }

    private void addVendorAccountMdcValues(String vendorId, String accountId) {
        MDC.put("vendorId", vendorId);
        MDC.put("accountId", accountId);
    }

    private void deleteCallableNumber(NumberEntity number) throws HttpStatusCodeException {
        if (number.getCapabilities().contains(ServiceType.CALL)) {
            callableNumbersService.deleteCallableNumber(number.getId());
        }
    }
}
