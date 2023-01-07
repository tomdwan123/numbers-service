/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.exception.VendorAccountRequiredException;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.NumberListResult;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.messagemedia.numbers.TestData.randomAssignedNumberEntity;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NumbersControllerNumberAssignmentTest extends NumbersControllerTest {

    @Test
    @UseDataProvider("providePageSize")
    public void shouldGetNumberAssignmentListOk(int pageSize) throws Exception {
        VendorAccountId vendorAccountId = new VendorAccountId("vendorId", "accountId");
        List<NumberEntity> numbers = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            numbers.add(randomAssignedNumberEntity(vendorAccountId));
        }
        UUID token = UUID.randomUUID();
        NumberListResult numberListResult = new NumberListResult(numbers, token);
        when(getNumbersService().getNumberAssignments(any(NumberAssignmentSearchRequest.class))).thenReturn(numberListResult);

        checkNumberAssignmentListWithPageSize(vendorAccountId, pageSize, numbers, token);
    }

    private void checkNumberAssignmentListWithPageSize(VendorAccountId vendorAccountId, int pageSize,
                                                       List<NumberEntity> numbers, UUID token) throws Exception {
        ResultActions resultActions = getMockMvc().perform(MockMvcRequestBuilders.get("/v1/numbers/assignments")
                .param("pageSize", String.valueOf(pageSize))
                .param("vendorId", vendorAccountId.getVendorId().getVendorId())
                .param("accountId", vendorAccountId.getAccountId().getAccountId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberAssignments", hasSize(pageSize)))
                .andExpect(jsonPath("$.pageMetadata.pageSize", is(pageSize)))
                .andExpect(jsonPath("$.pageMetadata.token", is(token.toString())));

        for (int i = 0; i < pageSize; i++) {
            checkNumberAssignmentInArrayResult(resultActions, i, numbers.get(i));
        }
    }

    private void checkNumberAssignmentInArrayResult(ResultActions resultActions, int index, NumberEntity numberEntity) throws Exception {
        String numberPrefix = String.format("$.numberAssignments[%d].number", index);
        String assignmentPrefix = String.format("$.numberAssignments[%d].assignment", index);
        checkNumber(resultActions, numberEntity, numberPrefix);
        checkAssignment(resultActions, numberEntity.getAssignedTo(), assignmentPrefix);
    }

    @DataProvider
    public static Object[][] badRequestParams() {
        return new Object[][]{
                {"token", "invalid token"},
                {"pageSize", "not number"}
        };
    }

    @Test
    @UseDataProvider("badRequestParams")
    public void shouldGetNumberAssignmentListFailBadRequest(String name, String value) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders
                .get("/v1/numbers/assignments")
                .param(name, value)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    public void shouldGetNumberAssignmentListFailNoVendorAccount() throws Exception {
        when(getNumbersService().getNumberAssignments(any(NumberAssignmentSearchRequest.class))).thenThrow(new VendorAccountRequiredException());
        getMockMvc().perform(MockMvcRequestBuilders
                .get("/v1/numbers/assignments")
                .param("vendorId", "")
                .param("accountId", "")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
