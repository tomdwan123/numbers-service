/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.mappers;

import com.google.common.collect.ImmutableList;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import com.messagemedia.numbers.service.client.models.NumberAssignmentDto;
import com.messagemedia.numbers.service.client.models.NumberDto;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static com.messagemedia.numbers.TestData.randomAssignedNumberEntity;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class NumberAssignmentMapperTest {

    private NumberAssignmentMapper mapper = Mappers.getMapper(NumberAssignmentMapper.class);

    @Test
    public void shouldMapNumberEntityToNumberAssignmentDto() {
        NumberEntity numberEntity = randomAssignedNumberEntity();

        NumberAssignmentDto numberAssignmentDto = mapper.toNumberAssignmentDto(numberEntity);
        NumberDto numberDto = numberAssignmentDto.getNumber();
        AssignmentDto assignmentDto = numberAssignmentDto.getAssignment();

        checkNumber(numberEntity, numberDto);
        checkAssignment(numberEntity, assignmentDto);
    }

    @Test
    public void shouldMapNumberEntityListToNumberAssignmentDtoList() {
        NumberEntity numberEntity1 = randomAssignedNumberEntity();
        NumberEntity numberEntity2 = randomAssignedNumberEntity();

        List<NumberAssignmentDto> numberAssignmentDtoList = mapper.toNumberAssignmentDtoList(ImmutableList.of(numberEntity1, numberEntity2));

        checkNumber(numberEntity1, numberAssignmentDtoList.get(0).getNumber());
        checkAssignment(numberEntity1, numberAssignmentDtoList.get(0).getAssignment());
        checkNumber(numberEntity2, numberAssignmentDtoList.get(1).getNumber());
        checkAssignment(numberEntity2, numberAssignmentDtoList.get(1).getAssignment());
    }

    private void checkAssignment(NumberEntity numberEntity, AssignmentDto assignmentDto) {
        assertThat(assignmentDto.getId(), equalTo(numberEntity.getAssignedTo().getId()));
        assertThat(assignmentDto.getAccountId(), equalTo(numberEntity.getAssignedTo().getAccountId()));
        assertThat(assignmentDto.getCallbackUrl(), equalTo(numberEntity.getAssignedTo().getCallbackUrl()));
        assertThat(assignmentDto.getMetadata(), equalTo(numberEntity.getAssignedTo().getExternalMetadata()));
        assertThat(assignmentDto.getVendorId(), equalTo(numberEntity.getAssignedTo().getVendorId()));
        assertThat(assignmentDto.getCreated(), equalTo(numberEntity.getAssignedTo().getCreated()));
    }

    private void checkNumber(NumberEntity numberEntity, NumberDto numberDto) {
        assertThat(numberDto.getAssignedTo(), equalTo(null));
        assertThat(numberDto.getId(), equalTo(numberEntity.getId()));
        assertThat(numberDto.getPhoneNumber(), equalTo(numberEntity.getPhoneNumber()));
        assertThat(numberDto.getProviderId(), equalTo(numberEntity.getProviderId()));
        assertThat(numberDto.getType(), equalTo(numberEntity.getType()));
        assertThat(numberDto.getCountry(), equalTo(numberEntity.getCountry()));
        assertThat(numberDto.getClassification(), equalTo(numberEntity.getClassification()));
        assertThat(numberDto.getCreated(), equalTo(numberEntity.getCreated()));
        assertThat(numberDto.getUpdated(), equalTo(numberEntity.getUpdated()));
        assertThat(numberDto.getAvailableAfter(), equalTo(numberEntity.getAvailableAfter()));
        assertThat(numberDto.getCapabilities(), equalTo(numberEntity.getCapabilities()));
    }
}
