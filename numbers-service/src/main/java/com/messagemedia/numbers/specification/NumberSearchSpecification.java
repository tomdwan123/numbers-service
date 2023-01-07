/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.specification;

import com.messagemedia.numbers.repository.entities.AssignmentEntity_;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity_;
import com.messagemedia.numbers.service.client.models.NumberSearchRequest;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NumberSearchSpecification extends BaseNumberSearchSpecification<NumberSearchRequest> {

    public NumberSearchSpecification(NumberSearchRequest request) {
        super(request);
    }

    @Override
    public Predicate toPredicate(Root<NumberEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        addToken(root, cb, predicates);
        addCountry(root, cb, predicates);
        addClassification(root, cb, predicates);
        addAssigned(root, predicates);
        addMatching(root, cb, predicates);
        addServiceType(root, cb, predicates);
        addAvailableBy(root, cb, predicates);
        return cb.and(predicates.toArray(new Predicate[]{}));
    }

    private void addAssigned(Root<NumberEntity> root, List<Predicate> predicates) {
        if (getSearchRequest().getAssigned() != null) {
            root.join(NumberEntity_.assignedTo, JoinType.LEFT);
            Path<UUID> assignmentIdPath = root.get(NumberEntity_.assignedTo).get(AssignmentEntity_.id);
            predicates.add(getSearchRequest().getAssigned() ? assignmentIdPath.isNotNull() : assignmentIdPath.isNull());
        }
    }

    private void addAvailableBy(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (getSearchRequest().getAvailableBy() != null) {
            predicates.add(
                    cb.or(
                            cb.isNull(root.get(NumberEntity_.availableAfter)),
                            cb.lessThanOrEqualTo(root.get(NumberEntity_.availableAfter), getSearchRequest().getAvailableBy())
                    )
            );
        }
    }
}
