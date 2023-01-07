/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssignmentVerificationService {

    private final AuditReader auditReader;

    @Autowired
    public AssignmentVerificationService(AuditReader auditReader) {
        this.auditReader = auditReader;
    }

    public boolean isValidForNewAssignment(NumberEntity numberEntity, String vendorId, String accountId) {
        OffsetDateTime availableAfter = numberEntity.getAvailableAfter();
        return availableAfter != null
            && (OffsetDateTime.now().isAfter(availableAfter)
            || isLastAccountAssignedToNumber(numberEntity, vendorId, accountId));
    }

    private boolean isLastAccountAssignedToNumber(NumberEntity numberEntity, String vendorId, String accountId) {
        final List<AssignmentEntity> resultList = getAllAssignmentEntityInCreationEvent(numberEntity);
        if (CollectionUtils.isEmpty(resultList)) {
            return false;
        }
        return vendorId.equals(resultList.get(0).getVendorId()) && accountId.equals(resultList.get(0).getAccountId());
    }

    private List<AssignmentEntity> getAllAssignmentEntityInCreationEvent(NumberEntity numberEntity) {
        return this.auditReader
                .createQuery()
                .forRevisionsOfEntity(AssignmentEntity.class, true, true)
                .add(AuditEntity.property("numberEntity").eq(numberEntity))
                .add(AuditEntity.revisionType().eq(RevisionType.ADD))
                .addOrder(AuditEntity.property("created").desc())
                .setMaxResults(1)
                .getResultList();
    }
}
