/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.mappers;

import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.service.client.models.AssignNumberRequest;
import com.messagemedia.numbers.service.client.models.AssignmentDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

@Component
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AssignmentMapper {

    @Mapping(source = "metadata", target = "externalMetadata")
    AssignmentEntity toAssignmentEntity(AssignNumberRequest assignNumberRequest);

    @Mapping(source = "externalMetadata", target = "metadata")
    @Mapping(source = "numberEntity.id", target = "numberId")
    AssignmentDto toAssignmentDto(AssignmentEntity assignmentEntity);

    List<AssignmentDto> toAssignmentDtoList(List<AssignmentEntity> assignmentEntityList);
}
