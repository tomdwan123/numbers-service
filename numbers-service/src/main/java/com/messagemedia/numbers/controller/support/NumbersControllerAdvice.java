/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller.support;

import com.messagemedia.framework.logging.Logger;
import com.messagemedia.framework.logging.LoggerFactory;
import com.messagemedia.framework.web.controller.DefaultExceptionHandlerControllerAdvice;
import com.messagemedia.framework.web.controller.StandardRestControllerError;
import com.messagemedia.numbers.exception.*;
import com.messagemedia.service.accountmanagement.client.exception.ServiceAccountManagementNotFoundException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static java.util.stream.Collectors.toList;

@ControllerAdvice
@ResponseBody
public class NumbersControllerAdvice extends DefaultExceptionHandlerControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(NumbersControllerAdvice.class);
    public static final String BAD_REQUEST_MESSAGE = "Bad Request";

    @ExceptionHandler(value = {DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardRestControllerError catchDataIntegrityViolationException(DataIntegrityViolationException e) {
        return toStandardRestControllerError("Failed to insert or update data because violation of an integrity constraint", e);
    }

    @ExceptionHandler(value = {NumberNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardRestControllerError catchNumberNotFoundException(NumberNotFoundException e) {
        return toStandardRestControllerError("Number not found", e);
    }

    @ExceptionHandler(value = {NumberNotAvailableException.class, NumberAlreadyAssignedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardRestControllerError catchNumberNotAssignedException(Exception e) {
        return toStandardRestControllerError("Cannot assign number", e);
    }

    @ExceptionHandler(value = {NumberNotAssignedException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardRestControllerError catchAssignmentNotFoundException(NumberNotAssignedException e) {
        return toStandardRestControllerError("Number is not assigned", e);
    }

    @ExceptionHandler(value = {ServiceAccountManagementNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardRestControllerError catchServiceAccountManagementNotFoundException(ServiceAccountManagementNotFoundException e) {
        return toStandardRestControllerError("Account not found exception", e);
    }

    @ExceptionHandler(value = {NumberAvailableAfterUpdateException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardRestControllerError catchNumberAvailableAfterUpdateException(NumberAvailableAfterUpdateException e) {
        return toStandardRestControllerError("Error when updating number", e);
    }

    @ExceptionHandler(value = {InvalidAccountRelationshipException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardRestControllerError catchInvalidAccountRelationshipException(InvalidAccountRelationshipException e) {
        return toStandardRestControllerError("Error when reassign due to account relationship", e);
    }

    @ExceptionHandler(value = {NumberUpdateRequestEmptyException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardRestControllerError catchNumberUpdateRequestEmptyException(NumberUpdateRequestEmptyException e) {
        return toStandardRestControllerError(BAD_REQUEST_MESSAGE, e);
    }

    @ExceptionHandler(value = {NotUsTollFreeNumberException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardRestControllerError catchNotUsTollFreeNumberExceptionException(NotUsTollFreeNumberException e) {
        return toStandardRestControllerError(BAD_REQUEST_MESSAGE, e);
    }

    @ExceptionHandler(value = {TollFreeNumberUpdateStatusException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardRestControllerError catchTollFreeNumberUpdateStatusExceptionException(TollFreeNumberUpdateStatusException e) {
        return toStandardRestControllerError(BAD_REQUEST_MESSAGE, e);
    }

    @ExceptionHandler(value = {AssignmentUpdateRequestEmptyException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardRestControllerError catchAssignmentUpdateRequestEmptyException(AssignmentUpdateRequestEmptyException e) {
        return toStandardRestControllerError(BAD_REQUEST_MESSAGE, e);
    }

    @ExceptionHandler(value = {BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardRestControllerError catchBindException(BindException e) {
        LOGGER.errorWithReason("Bad Request. Bind Exception", e.getMessage(), e);
        List<String> errors = e.getAllErrors().stream().map(p -> ((FieldError) p).getField() + ": " + p.getDefaultMessage()).collect(toList());
        return new StandardRestControllerError(BAD_REQUEST_MESSAGE, errors);
    }

    @ExceptionHandler(value = {DeleteAssignedNumberException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardRestControllerError catchDeleteAssignedNumberException(DeleteAssignedNumberException e) {
        return toStandardRestControllerError("Error when deleting number", e);
    }

    @ExceptionHandler(value = {EventNotificationPublishingFailedException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public StandardRestControllerError catchEventNotificationPublishingFailedException(EventNotificationPublishingFailedException e) {
        return toStandardRestControllerError("Failed to publish message to sqs queue", e);
    }

    @ExceptionHandler(value = {VendorAccountRequiredException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardRestControllerError catchVendorAccountRequiredException(VendorAccountRequiredException e) {
        return toStandardRestControllerError(BAD_REQUEST_MESSAGE, e);
    }

    @ExceptionHandler(value = {NumberCapabilityNotAvailableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardRestControllerError catchNumberCapabilityNotAvailableException(NumberCapabilityNotAvailableException e) {
        return toStandardRestControllerError("Number capability not available", e);
    }

    private StandardRestControllerError toStandardRestControllerError(String msg, Exception e) {
        LOGGER.errorWithReason(msg, e.getMessage(), e);
        return new StandardRestControllerError(ExceptionUtils.getRootCauseMessage(e));
    }
}
