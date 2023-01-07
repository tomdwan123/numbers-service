/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers;

import com.google.common.collect.ImmutableList;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.model.dto.*;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;

import java.util.*;

import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfile;
import static com.messagemedia.numbers.TestData.randomCallableNumberRoutingProfileStep;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntityWithCallCapability;
import static com.messagemedia.numbers.TestData.randomAssignmentEntityWithoutNumberEntity;
import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;
import static org.apache.commons.lang3.RandomStringUtils.*;

public class TestDataCreateCallableNumber {

    public static CallableNumberCreateRequest randomCallableNumberCreateRequest() {
        return new CallableNumberCreateRequest(UUID.randomUUID().toString(),
                randomNumeric(10),
                randomNumeric(2));
    }

    public static CallableNumberCreateResponse callableNumberCreateResponse(
            String phoneNumber, String vendorAccountId, CallableNumberRoutingProfile routingProfile) {
        return new CallableNumberCreateResponse(
                randomNumeric(2),
                vendorAccountId,
                phoneNumber,
                randomAlphabetic(10),
                routingProfile.getId(),
                routingProfile);
    }

    public static CallableNumberCreateResponse randomCallableNumberCreateResponse() {
        return new CallableNumberCreateResponse(randomNumeric(2),
                randomAlphanumeric(20),
                randomNumeric(10),
                randomAlphabetic(10),
                randomNumeric(2),
                randomCallableNumberRoutingProfile());
    }

    public static CallableNumberCustomerRequest randomCallableNumberCustomerRequest() {
        return new CallableNumberCustomerRequest(UUID.randomUUID().toString(),
                randomAlphabetic(10));
    }

    public static CallableNumberCustomerResponse randomCallableNumberCustomerResponse() {
        return new CallableNumberCustomerResponse(UUID.randomUUID().toString(),
                randomAlphabetic(10),
                0,
                0);
    }

    public static CallableNumberRoutingProfileCreateRequest randomCallableNumberRoutingProfileCreateRequest() {
        return new CallableNumberRoutingProfileCreateRequest(UUID.randomUUID().toString(),
                randomAlphabetic(10),
                ImmutableList.of(randomCallableNumberRoutingProfileStep()));
    }

    public static CallableNumberRoutingProfileCreateResponse randomCallableNumberRoutingProfileCreateResponse() {
        return new CallableNumberRoutingProfileCreateResponse(randomNumeric(2),
                UUID.randomUUID().toString(),
                randomAlphabetic(10),
                ImmutableList.of(randomCallableNumberRoutingProfileStep()));
    }

    public static CallableNumberUpdateRequest randomCallableNumberUpdateRequest() {
        return new CallableNumberUpdateRequest(randomNumeric(2), randomAlphanumeric(10));
    }

    public static NumberEntity randomAssignedNumberEntityWithCallCapability(VendorAccountId vendorAccountId) {
        NumberEntity numberEntity = randomUnassignedNumberEntityWithCallCapability();
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        assignmentEntity.setVendorId(vendorAccountId.getVendorId().getVendorId());
        assignmentEntity.setAccountId(vendorAccountId.getAccountId().getAccountId());
        numberEntity.setAssignedTo(assignmentEntity);
        assignmentEntity.setNumberEntity(numberEntity);
        return numberEntity;
    }

    public static NumberEntity randomAssignedNumberEntityWithoutCallCapability(VendorAccountId vendorAccountId) {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity.setCapabilities(new HashSet<>());
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        assignmentEntity.setVendorId(vendorAccountId.getVendorId().toString());
        assignmentEntity.setAccountId(vendorAccountId.getAccountId().toString());
        numberEntity.setAssignedTo(assignmentEntity);
        assignmentEntity.setNumberEntity(numberEntity);
        return numberEntity;
    }
}
