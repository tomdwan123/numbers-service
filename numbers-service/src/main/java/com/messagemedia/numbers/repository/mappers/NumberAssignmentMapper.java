/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.mappers;

import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.NumberAssignmentDto;
import com.messagemedia.numbers.service.client.models.NumberDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = AssignmentMapper.class)
public interface NumberAssignmentMapper {

    @Mapping(source = "numberEntity", target = "number")
    @Mapping(source = "numberEntity.assignedTo", target = "assignment")
    NumberAssignmentDto toNumberAssignmentDto(NumberEntity numberEntity);

    List<NumberAssignmentDto> toNumberAssignmentDtoList(List<NumberEntity> list);

    @Mapping(ignore = true, target = "assignedTo")
    NumberDto toNumberDto(NumberEntity numberEntity);
}
