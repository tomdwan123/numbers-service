/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.controller;

import com.messagemedia.numbers.service.AuditService;
import com.messagemedia.numbers.service.client.models.AssignmentAuditListResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static com.messagemedia.numbers.TestData.randomAuditPageMetadata;
import static com.messagemedia.numbers.controller.AuditController.AUDITING_URL;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuditController(auditService))
                .build();
    }

    @Test
    public void shouldGetAssignmentAudits() throws Exception {
        AssignmentAuditListResponse response = new AssignmentAuditListResponse(new ArrayList<>(), randomAuditPageMetadata());

        when(auditService.getAssignmentAudits(any())).thenReturn(response);
        mockMvc.perform(MockMvcRequestBuilders
                .get(AUDITING_URL + "/assignments")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());

        verify(auditService).getAssignmentAudits(any());
    }
}