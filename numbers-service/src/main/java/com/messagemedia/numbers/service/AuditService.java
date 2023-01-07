/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.mappers.AssignmentMapper;
import com.messagemedia.numbers.service.client.models.AssignmentAuditListResponse;
import com.messagemedia.numbers.service.client.models.AssignmentAuditSearchRequest;
import com.messagemedia.numbers.service.client.models.AuditPageMetadata;
import com.messagemedia.numbers.service.client.models.AuditToken;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static org.hibernate.envers.query.AuditEntity.*;

@Service
public class AuditService {

    private final AuditReader auditReader;
    private final AssignmentMapper assignmentMapper;

    @Autowired
    public AuditService(AuditReader auditReader, AssignmentMapper assignmentMapper) {
        this.auditReader = auditReader;
        this.assignmentMapper = assignmentMapper;
    }

    public AssignmentAuditListResponse getAssignmentAudits(AssignmentAuditSearchRequest request) {
        Objects.requireNonNull(request);
        AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity(AssignmentEntity.class, false, true);

        ofNullable(request.getId()).ifPresent(id -> auditQuery.add(AuditEntity.id().eq(id)));
        ofNullable(request.getNumberId()).ifPresent(numberId -> auditQuery.add(property("numberId").eq(numberId)));
        ofNullable(request.getCreatedBefore()).ifPresent(created -> auditQuery.add(property("created").le(created)));
        ofNullable(request.getCreatedAfter()).ifPresent(created -> auditQuery.add(property("created").gt(created)));
        ofNullable(request.getDeletedBefore()).ifPresent(deleted -> {
            auditQuery.add(revisionType().eq(RevisionType.DEL));
            auditQuery.add(revisionProperty("timestamp").le(deleted.toInstant().toEpochMilli()));
        });
        ofNullable(request.getDeletedAfter()).ifPresent(deleted -> {
            auditQuery.add(revisionType().eq(RevisionType.DEL));
            auditQuery.add(revisionProperty("timestamp").gt(deleted.toInstant().toEpochMilli()));
        });
        addVendorAccountIdFilter(request, auditQuery);
        addTokenFilter(request, auditQuery);
        addOrders(auditQuery);

        List<AssignmentEntity> assignmentEntityList = new ArrayList<>();
        List<Object[]> resultList = auditQuery.getResultList();
        for (Object[] objects : resultList) {
            AssignmentEntity assignmentEntity = (AssignmentEntity) objects[0];
            RevisionType revisionType = (RevisionType) objects[2];
            if (revisionType == RevisionType.DEL) {
                DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) objects[1];
                assignmentEntity.setDeleted(OffsetDateTime.ofInstant(revisionEntity.getRevisionDate().toInstant(), ZoneOffset.UTC));
            }
            assignmentEntityList.add(assignmentEntity);
        }

        //using the request page size + 1 record to create the audit token
        AuditToken token = null;
        if (resultList.size() == request.getPageSize() + 1) {
            Object[] nextObjects = resultList.get(request.getPageSize());
            AssignmentEntity nextEntity = (AssignmentEntity) nextObjects[0];
            DefaultRevisionEntity nextRevisionEntity = (DefaultRevisionEntity) nextObjects[1];
            token = new AuditToken(nextEntity.getId(), nextRevisionEntity.getId());
            assignmentEntityList = assignmentEntityList.subList(0, request.getPageSize());
        }

        AuditPageMetadata pageMetadata = new AuditPageMetadata(request.getPageSize(), token);
        return new AssignmentAuditListResponse(assignmentMapper.toAssignmentDtoList(assignmentEntityList), pageMetadata);
    }

    private void addOrders(AuditQuery auditQuery) {
        auditQuery.addOrder(AuditEntity.id().desc());
        auditQuery.addOrder(AuditEntity.revisionNumber().desc());
    }

    private void addTokenFilter(AssignmentAuditSearchRequest request, AuditQuery auditQuery) {
        if (request.getToken() != null) {
            auditQuery.add(AuditEntity.disjunction()
                    .add(AuditEntity.id().lt(request.getToken().getTokenId()))
                    .add(AuditEntity.conjunction()
                            .add(AuditEntity.id().eq(request.getToken().getTokenId()))
                            .add(AuditEntity.revisionNumber().le(Integer.valueOf(request.getToken().getRevNumber())))
                    ));
        }
        auditQuery.setMaxResults(request.getPageSize() + 1);
    }

    private void addVendorAccountIdFilter(AssignmentAuditSearchRequest request, AuditQuery auditQuery) {
        if (request.getVendorAccountId() != null) {
            VendorAccountId vendorAccountId = request.getVendorAccountId();
            ofNullable(vendorAccountId.getAccountId())
                    .ifPresent(accountId -> auditQuery.add(property("accountId").eq(accountId.getAccountId())));
            ofNullable(vendorAccountId.getVendorId())
                    .ifPresent(vendorId -> auditQuery.add(property("vendorId").eq(vendorId.getVendorId())));
        }
    }
}
