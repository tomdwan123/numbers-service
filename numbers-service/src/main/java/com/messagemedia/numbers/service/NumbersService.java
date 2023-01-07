/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.jackson.core.valuewithnull.ValueWithNull;
import com.messagemedia.framework.logging.Logger;
import com.messagemedia.framework.logging.LoggerFactory;
import com.messagemedia.numbers.exception.AssignmentUpdateRequestEmptyException;
import com.messagemedia.numbers.exception.DeleteAssignedNumberException;
import com.messagemedia.numbers.exception.InvalidAccountRelationshipException;
import com.messagemedia.numbers.exception.NotUsTollFreeNumberException;
import com.messagemedia.numbers.exception.NumberAlreadyAssignedException;
import com.messagemedia.numbers.exception.NumberAvailableAfterUpdateException;
import com.messagemedia.numbers.exception.NumberNotAssignedException;
import com.messagemedia.numbers.exception.NumberNotAvailableException;
import com.messagemedia.numbers.exception.NumberNotFoundException;
import com.messagemedia.numbers.exception.NumberUpdateRequestEmptyException;
import com.messagemedia.numbers.exception.TollFreeNumberUpdateStatusException;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.billing.SlackMessage;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import com.messagemedia.numbers.service.client.models.NumberSearchRequest;
import com.messagemedia.numbers.service.client.models.NumberType;
import com.messagemedia.numbers.service.client.models.Status;
import com.messagemedia.numbers.service.client.models.UpdateAssignmentRequest;
import com.messagemedia.numbers.service.client.models.UpdateNumberRequest;
import com.messagemedia.numbers.service.event.SlackNotificationEvent;
import com.messagemedia.numbers.specification.NumberAssignmentSearchSpecification;
import com.messagemedia.numbers.specification.NumberSearchSpecification;
import com.messagemedia.service.accountmanagement.client.exception.ServiceAccountManagementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.ofNullable;

@Service
public class NumbersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NumbersService.class);

    private final NumbersRepository numbersRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentVerificationService assignmentVerificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final String slackNotificationUrl;

    @Value("${numbers.service.availability.graceperiod.days}")
    private int extendAvailableAfterDays;

    private final AccountReassignVerificationService accountReassignVerificationService;

    @Autowired
    public NumbersService(NumbersRepository numbersRepository, AssignmentRepository assignmentRepository,
                          AccountReassignVerificationService accountReassignVerificationService,
                          AssignmentVerificationService assignmentVerificationService,
                          ApplicationEventPublisher eventPublisher,
                          @Value("${service.numbers-service.slack-notification.assigned-tfn.url:NONE}") String slackNotificationUrl) {
        this.numbersRepository = numbersRepository;
        this.assignmentRepository = assignmentRepository;
        this.accountReassignVerificationService = accountReassignVerificationService;
        this.assignmentVerificationService = assignmentVerificationService;
        this.eventPublisher = eventPublisher;
        this.slackNotificationUrl = slackNotificationUrl;
    }

    public NumberListResult getNumbers(NumberSearchRequest request) {
        Pageable pageable = PageRequest.of(0, request.getPageSize() + 1, new Sort(Sort.Direction.ASC, "id"));
        // Requesting (pageSize + 1) numbers as the ID of the last number is used as the token to the next page.
        List<NumberEntity> numbers = numbersRepository.findAll(new NumberSearchSpecification(request), pageable).getContent();
        return getNumberListResult(request.getPageSize(), numbers);
    }

    private NumberListResult getNumberListResult(int requestedPageSize, List<NumberEntity> numbers) {
        UUID token = null;
        if (numbers.size() == requestedPageSize + 1) {
            token = numbers.get(requestedPageSize).getId();
            numbers = numbers.subList(0, requestedPageSize);
        }
        return new NumberListResult(numbers, token);
    }

    public NumberEntity registerNumber(NumberEntity numberEntity) {
        Objects.requireNonNull(numberEntity);
        return numbersRepository.save(numberEntity);
    }

    public NumberListResult getNumberAssignments(NumberAssignmentSearchRequest request) {
        Pageable pageable = PageRequest.of(0, request.getPageSize() + 1, new Sort(Sort.Direction.ASC, "id"));
        List<NumberEntity> numbers = numbersRepository.findAll(new NumberAssignmentSearchSpecification(request), pageable).getContent();
        return getNumberListResult(request.getPageSize(), numbers);
    }

    @Transactional
    public AssignmentEntity assignNumberToAccount(UUID numberId, AssignmentEntity assignmentEntity) {
        Objects.requireNonNull(assignmentEntity);

        NumberEntity numberEntity = getNumber(numberId);

        if (numberEntity.getAssignedTo() != null) {
            throw new NumberAlreadyAssignedException(numberId);
        }

        final boolean validForNewAssignment = assignmentVerificationService
            .isValidForNewAssignment(numberEntity, assignmentEntity.getVendorId(), assignmentEntity.getAccountId());
        if (!validForNewAssignment) {
            throw new NumberNotAvailableException(numberId);
        }

        //indicate that this number is not available
        numberEntity.setAvailableAfter(null);
        numbersRepository.save(numberEntity);

        assignmentEntity.setNumberEntity(numberEntity);
        assignmentEntity = assignmentRepository.save(assignmentEntity);

        if (isUsTollFreeNumber(numberEntity)) {
            numberEntity.setAssignedTo(assignmentEntity);
            updateTollFreeNumberStatus(numberEntity, Status.UNVERIFIED);
            numbersRepository.save(numberEntity);
            publishEvent(numberEntity.getPhoneNumber(), new VendorAccountId(assignmentEntity.getVendorId(), assignmentEntity.getAccountId()),
                    "assign");
        }

        return assignmentEntity;
    }

    @Transactional
    public AssignmentEntity reassignNumber(UUID numberId, AssignmentEntity assignmentEntity) throws ServiceAccountManagementException {
        Objects.requireNonNull(assignmentEntity);

        NumberEntity numberEntity = getNumber(numberId);

        final AssignmentEntity currentAssignment = numberEntity.getAssignedTo();
        if (currentAssignment == null) {
            throw new NumberNotAssignedException(numberId);
        }

        VendorAccountId newAssignee = new VendorAccountId(assignmentEntity.getVendorId(), assignmentEntity.getAccountId());
        VendorAccountId currentAssignee = new VendorAccountId(currentAssignment.getVendorId(), currentAssignment.getAccountId());

        if (!this.accountReassignVerificationService.verifyAccountRelationship(newAssignee, currentAssignee)) {
            throw new InvalidAccountRelationshipException(newAssignee);
        }

        assignmentRepository.delete(currentAssignment);
        assignmentEntity.setNumberEntity(numberEntity);
        assignmentEntity = assignmentRepository.save(assignmentEntity);

        if (isUsTollFreeNumber(numberEntity)) {
            publishEvent(numberEntity.getPhoneNumber(), new VendorAccountId(assignmentEntity.getVendorId(), assignmentEntity.getAccountId()),
                    "reassign");
        }

        return assignmentEntity;
    }

    public AssignmentEntity loadAssignmentDetailsByNumberId(UUID numberId) {
        NumberEntity numberEntity = getNumber(numberId);
        if (numberEntity.getAssignedTo() == null) {
            throw new NumberNotAssignedException(numberId);
        }
        return numberEntity.getAssignedTo();
    }

    @Transactional
    public AssignmentEntity disassociateAssignment(UUID numberId) {
        NumberEntity number = getNumber(numberId);
        AssignmentEntity assignedTo = ofNullable(number.getAssignedTo())
                .orElseThrow(() -> new NumberNotAssignedException(numberId));

        assignmentRepository.delete(assignedTo);
        number.setAvailableAfter(OffsetDateTime.now().plusDays(extendAvailableAfterDays));
        number.setAssignedTo(null);
        number.setDedicatedReceiver(FALSE);

        if (isUsTollFreeNumber(number)) {
            updateTollFreeNumberStatus(number, null);
        }

        numbersRepository.save(number);
        return assignedTo;
    }

    public NumberEntity getNumber(UUID numberId) {
        Objects.requireNonNull(numberId);
        return numbersRepository.findById(numberId)
                .orElseThrow(() -> new NumberNotFoundException(numberId));
    }

    @Transactional
    public NumberEntity updateNumber(UUID numberId, UpdateNumberRequest numberRequest) {
        Objects.requireNonNull(numberRequest);

        if (numberRequest.isEmpty()) {
            throw new NumberUpdateRequestEmptyException(String.format("request body is invalid: %s", Objects.toString(numberRequest)));
        }

        NumberEntity numberEntity = getNumber(numberId);

        if (numberRequest.hasStatus()) {
            if (!isUsTollFreeNumber(numberEntity)) {
                throw new NotUsTollFreeNumberException(numberEntity.getId());
            }
            updateTollFreeNumberStatus(numberEntity, numberRequest.getStatus());
        }

        // the assigned number should not change availableAfter to non-null
        if (numberEntity.getAssignedTo() != null
                && !Optional.ofNullable(numberRequest.getAvailableAfter()).map(ValueWithNull::isExplicitNull).orElse(true)) {
            throw new NumberAvailableAfterUpdateException(numberId);
        }

        // update existed fields
        ofNullable(numberRequest.getAvailableAfter()).ifPresent(availableAfter -> numberEntity.setAvailableAfter(availableAfter.get()));
        ofNullable(numberRequest.getClassification()).ifPresent(numberEntity::setClassification);
        ofNullable(numberRequest.getCapabilities()).ifPresent(numberEntity::setCapabilities);
        ofNullable(numberRequest.getDedicatedReceiver()).ifPresent(numberEntity::setDedicatedReceiver);
        ofNullable(numberRequest.getProviderId()).ifPresent(numberEntity::setProviderId);

        return numbersRepository.save(numberEntity);
    }

    public AssignmentEntity updateAssignment(UUID numberId, UpdateAssignmentRequest assignmentRequest) {
        Objects.requireNonNull(assignmentRequest);

        if (assignmentRequest.isEmpty()) {
            throw new AssignmentUpdateRequestEmptyException(String.format("request body is invalid: %s", Objects.toString(assignmentRequest)));
        }

        NumberEntity numberEntity = getNumber(numberId);

        AssignmentEntity assignmentEntity = numberEntity.getAssignedTo();
        if (assignmentEntity == null) {
            throw new NumberNotAssignedException(numberId);
        }

        // update existed fields
        ofNullable(assignmentRequest.getCallbackUrl()).ifPresent(callbackUrl -> assignmentEntity.setCallbackUrl(callbackUrl.get()));
        ofNullable(assignmentRequest.getMetadata()).ifPresent(metadata -> assignmentEntity.setExternalMetadata(metadata.get()));
        ofNullable(assignmentRequest.getLabel()).ifPresent(label -> assignmentEntity.setLabel(label.get()));

        return assignmentRepository.save(assignmentEntity);
    }

    public NumberEntity deleteNumber(UUID numberId) {
        NumberEntity numberEntity = getNumber(numberId);
        if (numberEntity.getAssignedTo() != null) {
            throw new DeleteAssignedNumberException(numberId);
        }
        numbersRepository.deleteById(numberId);
        return numberEntity;
    }

    private boolean isUsTollFreeNumber(NumberEntity numberEntity) {
        return "US".equals(numberEntity.getCountry()) && numberEntity.getType() == NumberType.TOLL_FREE;
    }

    private void publishEvent(final String phoneNumber,
                              final VendorAccountId vendorAccountId,
                              final String action) {
        String hubSupportLink = "https://hub.messagemedia.com/support/" + vendorAccountId.getAccountId();
        String message = String.format("Toll-free number was %sed.\n"
                + "Account: %s\n"
                + "Phone number: %s\n"
                + "View on HST: %s\n", action, vendorAccountId.toColonString(), phoneNumber, hubSupportLink);

        final SlackNotificationEvent event = new SlackNotificationEvent(new SlackMessage(message), slackNotificationUrl);
        this.eventPublisher.publishEvent(event);
    }

    private void updateTollFreeNumberStatus(NumberEntity numberEntity, Status status) {
        if (!isValidStatusUpdateForTfn(numberEntity.getAssignedTo(), status)) {
            throw new TollFreeNumberUpdateStatusException(numberEntity.getId(), status);
        }

        if (status == Status.UNVERIFIED) {
            numberEntity.setAvailableAfter(null);
        }
        if (status == Status.ASSIGNED) {
            numberEntity.setAvailableAfter(null);
        }

        // update existed fields
        numberEntity.setStatus(status);
        LOGGER.info("Number id {} is set the status to {}", numberEntity.getId(), status);
    }

    private boolean isValidStatusUpdateForTfn(AssignmentEntity assignment, Status status) {
        return (status != null) == (assignment != null);
    }
}
