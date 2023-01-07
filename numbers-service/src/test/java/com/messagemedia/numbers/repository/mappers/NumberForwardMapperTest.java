/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.mappers;

import com.messagemedia.numbers.model.dto.CallableNumberDto;
import com.messagemedia.numbers.service.client.models.NumberForwardDto;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

import static com.messagemedia.numbers.TestData.randomCallableNumberDto;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NumberForwardMapperTest {

    private final NumberForwardMapper numberForwardMapper = Mappers.getMapper(NumberForwardMapper.class);

    @Test
    public void testNulls() {
        CallableNumberDto callableNumberDto = randomCallableNumberDto();
        callableNumberDto.setRoutingProfile(null);
        assertNull(numberForwardMapper.toNumberForwardDto(null).getDestination());
        assertNull(numberForwardMapper.toNumberForwardDto(callableNumberDto).getDestination());
    }

    @Test
    public void shouldMapCallableNumberDtoToNumberForwardDto() {
        CallableNumberDto callableNumberDto = randomCallableNumberDto();
        NumberForwardDto numberForwardDto = numberForwardMapper.toNumberForwardDto(callableNumberDto);
        assertEquals(callableNumberDto.getRoutingProfile().getNumber(), numberForwardDto.getDestination());
    }
}