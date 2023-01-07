/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.mappers;

import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import com.messagemedia.numbers.service.client.models.NumberDto;
import com.messagemedia.numbers.service.client.models.RegisterNumberRequest;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import org.junit.Test;
import org.mapstruct.factory.Mappers;
import java.util.Arrays;
import java.util.List;

import static com.messagemedia.numbers.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NumbersMapperTest {

    private NumbersMapper numbersMapper = Mappers.getMapper(NumbersMapper.class);

    @Test
    public void shouldMapRegisterNumberRequestToNumberEntity() {
        RegisterNumberRequest registerNumberRequest = randomRegisterNumberRequest();
        NumberEntity numberEntity = numbersMapper.toNumberEntity(registerNumberRequest);
        assertEquals(registerNumberRequest.getPhoneNumber(), numberEntity.getPhoneNumber());
        assertEquals(registerNumberRequest.getProviderId(), numberEntity.getProviderId());
        assertEquals(registerNumberRequest.getClassification(), numberEntity.getClassification());
        assertEquals(registerNumberRequest.getCountry(), numberEntity.getCountry());
        assertEquals(registerNumberRequest.getType(), numberEntity.getType());
        assertEquals(registerNumberRequest.getCapabilities(), numberEntity.getCapabilities());
    }

    @Test
    public void shouldMapNumberEntityToNumberDto() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        NumberDto numberDto = numbersMapper.toNumberDto(numberEntity);
        assertEquals(numberEntity.getId(), numberDto.getId());
        assertEquals(numberEntity.getPhoneNumber(), numberDto.getPhoneNumber());
        assertEquals(numberEntity.getProviderId(), numberDto.getProviderId());
        assertEquals(numberEntity.getType(), numberDto.getType());
        assertEquals(numberEntity.getCountry(), numberDto.getCountry());
        assertEquals(numberEntity.getClassification(), numberDto.getClassification());
        assertEquals(numberEntity.getCreated(), numberDto.getCreated());
        assertEquals(numberEntity.getUpdated(), numberDto.getUpdated());
        assertEquals(numberEntity.getAvailableAfter(), numberDto.getAvailableAfter());
        assertEquals(numberEntity.getCapabilities(), numberDto.getCapabilities());
    }

    @Test
    public void shouldMapNumberEntityListToNumberDtoList() {
        NumberEntity numberEntity0 = randomUnassignedNumberEntity();
        NumberEntity numberEntity1 = randomAssignedNumberEntity();

        List<NumberDto> list = numbersMapper.toNumberDtoList(Arrays.asList(numberEntity0, numberEntity1));
        checkEquals(numberEntity0, list.get(0));
        checkEquals(numberEntity1, list.get(1));
    }

    private void checkEquals(NumberEntity numberEntity, NumberDto numberDto) {
        assertEquals(numberEntity.getId(), numberDto.getId());
        assertEquals(numberEntity.getPhoneNumber(), numberDto.getPhoneNumber());
        assertEquals(numberEntity.getProviderId(), numberDto.getProviderId());
        assertEquals(numberEntity.getType(), numberDto.getType());
        assertEquals(numberEntity.getCountry(), numberDto.getCountry());
        assertEquals(numberEntity.getClassification(), numberDto.getClassification());
        assertEquals(numberEntity.getCreated(), numberDto.getCreated());
        assertEquals(numberEntity.getUpdated(), numberDto.getUpdated());
        assertEquals(numberEntity.getAvailableAfter(), numberDto.getAvailableAfter());
        assertEquals(numberEntity.getCapabilities(), numberDto.getCapabilities());
        checkEquals(numberEntity.getAssignedTo(), numberDto.getAssignedTo());
    }

    private void checkEquals(AssignmentEntity assignmentEntity, AssignmentDto assignmentDto) {
        if (assignmentEntity == null) {
            assertNull(assignmentDto);
            return;
        }
        assertNotNull(assignmentDto);
        assertEquals(assignmentEntity.getId(), assignmentDto.getId());
        assertEquals(assignmentEntity.getAccountId(), assignmentDto.getAccountId());
        assertEquals(assignmentEntity.getCallbackUrl(), assignmentDto.getCallbackUrl());
        assertEquals(assignmentEntity.getExternalMetadata(), assignmentDto.getMetadata());
        assertEquals(assignmentEntity.getVendorId(), assignmentDto.getVendorId());
        assertEquals(assignmentEntity.getCreated(), assignmentDto.getCreated());
    }
}