/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.event.SlackNotificationEvent;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import static com.messagemedia.numbers.TestData.*;
import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class NumbersServiceTollFreeNumberTest {

    private static final String SLACK_URL = "http://localhost:4200/def/123";

    @Mock
    private NumbersRepository numbersRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentVerificationService assignmentVerificationService;

    @Mock
    private AccountReassignVerificationService accountReassignVerificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<SlackNotificationEvent> eventCaptor;

    private NumbersService numbersService;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.numbersService = new NumbersService(numbersRepository, assignmentRepository, accountReassignVerificationService,
                assignmentVerificationService, eventPublisher, SLACK_URL);
    }

    @Test
    public void shouldAssignTollFreeNumber() throws Exception {
        // Given
        NumberEntity numberEntity = unassignedUsTollFreeNumberEntity();
        numberEntity.setAvailableAfter(OffsetDateTime.now().minusSeconds(10));
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        when(assignmentRepository.save(assignmentEntity)).thenReturn(assignmentEntity);
        when(assignmentVerificationService.isValidForNewAssignment(any(), any(), any())).thenReturn(true);

        // When
        AssignmentEntity dbAssignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), assignmentEntity);

        // Then
        assertAssignmentsEquals(assignmentEntity, dbAssignmentEntity);
        verify(this.eventPublisher).publishEvent(this.eventCaptor.capture());
    }

    @Test
    public void shouldReassignTollFreeNumber() throws Exception {
        // Given
        NumberEntity numberEntity = assignedUsTollFreeNumberEntity();
        AssignmentEntity newAssignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        newAssignmentEntity.setNumberEntity(numberEntity);
        when(numbersRepository.findById(numberEntity.getId())).thenReturn(Optional.of(numberEntity));
        when(accountReassignVerificationService.verifyAccountRelationship(any(), any())).thenReturn(true);
        when(assignmentRepository.save(newAssignmentEntity)).thenReturn(newAssignmentEntity);

        // When
        final AssignmentEntity assignmentEntity = numbersService.reassignNumber(numberEntity.getId(), newAssignmentEntity);

        // Then
        assertAssignmentsEquals(newAssignmentEntity, assignmentEntity);
        verify(this.eventPublisher).publishEvent(this.eventCaptor.capture());
    }

    @Test
    public void shouldDisassociateTollFreeNumberAssignment() {
        NumberEntity assignedNumberEntity = assignedUsTollFreeNumberEntity();
        when(numbersRepository.findById(any(UUID.class))).thenReturn(Optional.of(assignedNumberEntity));
        when(numbersRepository.save(any(NumberEntity.class))).thenReturn(assignedNumberEntity);

        numbersService.disassociateAssignment(assignedNumberEntity.getId());

        verify(assignmentRepository).delete(any(AssignmentEntity.class));
        verify(numbersRepository).save(any(NumberEntity.class));
    }

    private void assertAssignmentsEquals(AssignmentEntity assignmentEntity, AssignmentEntity comparingAssignmentEntity) {
        assertNotNull(assignmentEntity);
        assertEquals(assignmentEntity, comparingAssignmentEntity);
        assertEquals(assignmentEntity.getId(), comparingAssignmentEntity.getId());
        assertEquals(assignmentEntity.getNumberEntity().getId(), comparingAssignmentEntity.getNumberEntity().getId());
        assertEquals(assignmentEntity.getAccountId(), comparingAssignmentEntity.getAccountId());
        assertEquals(assignmentEntity.getVendorId(), comparingAssignmentEntity.getVendorId());
        assertEquals(assignmentEntity.getCallbackUrl(), comparingAssignmentEntity.getCallbackUrl());
        assertEquals(assignmentEntity.getExternalMetadata(), comparingAssignmentEntity.getExternalMetadata());
    }
}
