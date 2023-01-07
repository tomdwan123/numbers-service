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
import com.messagemedia.numbers.service.client.models.AssignmentAuditSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.messagemedia.numbers.controller.AuditController.AUDITING_URL;

@RestController
@RequestMapping(value = AUDITING_URL, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public final class AuditController {

    static final String AUDITING_URL = "/v1/auditing";

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping(path = "/assignments")
    public AssignmentAuditListResponse getAuditingAssignments(@Valid AssignmentAuditSearchRequest request) {
        return auditService.getAssignmentAudits(request);
    }
}
