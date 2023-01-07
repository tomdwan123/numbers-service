/*
 * Copyright (c) Message4U Pty Ltd 2014-2021
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.mappers;

import com.messagemedia.numbers.model.dto.CallableNumberDto;
import com.messagemedia.numbers.service.client.models.NumberForwardDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

@Component
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class NumberForwardMapper {

    public NumberForwardDto toNumberForwardDto(CallableNumberDto callableNumberDto) {
        NumberForwardDto numberForwardDto = new NumberForwardDto();

        if (callableNumberDto == null || callableNumberDto.getRoutingProfile() == null) {
            numberForwardDto.setDestination(null);
        } else {
            numberForwardDto.setDestination(callableNumberDto.getRoutingProfile().getNumber());
        }

        return numberForwardDto;
    }
}
