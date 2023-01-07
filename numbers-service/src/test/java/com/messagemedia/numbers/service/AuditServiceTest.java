/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.mappers.AssignmentMapper;
import com.messagemedia.numbers.service.client.models.AssignmentAuditSearchRequest;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static com.messagemedia.numbers.TestData.randomAssignmentAuditSearchRequest;
import static com.messagemedia.numbers.TestData.randomAssignmentEntity;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AuditServiceTest {

    @Mock
    private AuditReader auditReader;

    @Mock
    private AuditQueryCreator auditQueryCreator;

    @Mock
    private AuditQuery auditQuery;

    @Mock
    private AssignmentMapper assignmentMapper;

    @InjectMocks
    private AuditService auditService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetAssignmentAudits() {
        List<Object[]> resultList = new ArrayList<>();
        resultList.add(new Object[]{randomAssignmentEntity(), new DefaultRevisionEntity(), RevisionType.DEL});
        resultList.add(new Object[]{randomAssignmentEntity(), new DefaultRevisionEntity(), RevisionType.ADD});

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(AssignmentEntity.class, false, true)).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(resultList);
        when(assignmentMapper.toAssignmentDtoList(any())).thenReturn(new ArrayList<>());

        AssignmentAuditSearchRequest request = randomAssignmentAuditSearchRequest();
        request.setPageSize(1);

        auditService.getAssignmentAudits(request);
        verify(auditQuery, times(11)).add(any());
        verify(auditQuery).getResultList();
    }
}