/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */


package com.messagemedia.numbers.service;

import static com.messagemedia.numbers.TestData.randomAssignmentEntityWithoutNumberEntity;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;

import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.repository.AssignmentRepository;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import java.time.OffsetDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(DataProviderSpringRunner.class)
public class AssignmentVerificationServiceIT {

    public static final String OTHER_ACCOUNT = "OtherAccount";
    public static final String OTHER_VENDOR = "OtherVendor";
    @Autowired
    private AssignmentVerificationService assignmentVerificationService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private NumbersRepository numbersRepository;

    @Autowired
    private NumbersService numbersService;

    @Test
    public void testValidAssignmentWhenTheNumberAvailableToLastOwner() throws Exception {
        // Given
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());
        assignmentRepository.save(assignmentEntity);
        numbersService.disassociateAssignment(numberEntity.getId());
        final NumberEntity numberDb = numbersService.getNumber(numberEntity.getId());

        // When and Then
        Assert.assertTrue(assignmentVerificationService.isValidForNewAssignment(numberDb, assignmentEntity.getVendorId(),
            assignmentEntity.getAccountId()));
    }

    @Test
    public void testValidAssignmentWhenTheNumberAvailableToEveryAccount() throws Exception {
        // Given
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());
        assignmentRepository.save(assignmentEntity);
        numbersService.disassociateAssignment(numberEntity.getId());
        final NumberEntity numberDb = numbersService.getNumber(numberEntity.getId());
        numberDb.setAvailableAfter(OffsetDateTime.now().minusSeconds(3_600));
        numbersRepository.save(numberDb);

        // When and Then
        Assert.assertTrue(assignmentVerificationService.isValidForNewAssignment(numberDb, OTHER_VENDOR, OTHER_ACCOUNT));
    }

    @Test
    public void testInvalidAssignmentBecauseAlreadyAssigned() throws Exception {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());
        assignmentRepository.save(assignmentEntity);
        final NumberEntity numberDb = numbersService.getNumber(numberEntity.getId());
        Assert.assertFalse(assignmentVerificationService.isValidForNewAssignment(numberDb, assignmentEntity.getVendorId(),
            assignmentEntity.getAccountId()));
    }

    @Test
    public void testInvalidAssignmentBecauseNotTheLastAssignmentAccount() throws Exception {
        NumberEntity numberEntity = numbersService.registerNumber(randomUnassignedNumberEntity());
        AssignmentEntity assignmentEntity = numbersService.assignNumberToAccount(numberEntity.getId(), randomAssignmentEntityWithoutNumberEntity());
        assignmentRepository.save(assignmentEntity);
        numbersService.disassociateAssignment(numberEntity.getId());
        final NumberEntity numberDb = numbersService.getNumber(numberEntity.getId());
        Assert.assertFalse(assignmentVerificationService.isValidForNewAssignment(numberDb, OTHER_VENDOR, OTHER_ACCOUNT));
    }
}
