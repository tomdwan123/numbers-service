/*
 * Copyright (c) Message4U Pty Ltd 2014-2019
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.specification;

import com.messagemedia.numbers.exception.VendorAccountRequiredException;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.AssignmentEntity_;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity_;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NumberAssignmentSearchSpecification extends BaseNumberSearchSpecification<NumberAssignmentSearchRequest> {

    private static final String ALL_ACCOUNTS = "all";

    public NumberAssignmentSearchSpecification(NumberAssignmentSearchRequest searchRequest) {
        super(searchRequest);
        if (!(StringUtils.isNotBlank(searchRequest.getVendorId()) && StringUtils.isNotBlank(searchRequest.getAccountId()))) {
            throw new VendorAccountRequiredException();
        }
    }

    @Override
    public Predicate toPredicate(Root<NumberEntity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(getSearchRequest().getAccounts())) {
            addVendorAccounts(root, criteriaBuilder, predicates);
        } else {
            addVendorAccount(root, criteriaBuilder, predicates);
        }
        addToken(root, criteriaBuilder, predicates);
        addClassification(root, criteriaBuilder, predicates);
        addCountry(root, criteriaBuilder, predicates);
        addLabel(root, criteriaBuilder, predicates);
        if (ArrayUtils.isNotEmpty(getSearchRequest().getMatchings())) {
            addMatchings(root, criteriaBuilder, predicates);
        } else {
            addMatching(root, criteriaBuilder, predicates);
        }
        addServiceType(root, criteriaBuilder, predicates);
        addStatus(root, criteriaBuilder, predicates);
        return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
    }

    private void addVendorAccount(Root<NumberEntity> root, CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        Join<NumberEntity, AssignmentEntity> assignmentJoin =  root.join(NumberEntity_.assignedTo);
        predicates.add(criteriaBuilder.equal(assignmentJoin.get(AssignmentEntity_.vendorId), getSearchRequest().getVendorId()));
        predicates.add(criteriaBuilder.equal(assignmentJoin.get(AssignmentEntity_.accountId), getSearchRequest().getAccountId()));
    }

    private void addVendorAccounts(Root<NumberEntity> root, CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        Join<NumberEntity, AssignmentEntity> assignmentJoin =  root.join(NumberEntity_.assignedTo);
        predicates.add(criteriaBuilder.equal(assignmentJoin.get(AssignmentEntity_.vendorId), getSearchRequest().getVendorId()));
        if (getSearchRequest().getAccounts().length == 1 && ALL_ACCOUNTS.equals(getSearchRequest().getAccounts()[0])) {
            return;
        }
        predicates.add(criteriaBuilder.or(Arrays.stream(getSearchRequest().getAccounts()).map(
                accountId -> criteriaBuilder.equal(
                        assignmentJoin.get(AssignmentEntity_.accountId),
                        accountId
                )).toArray(Predicate[]::new)));
    }

    private void addLabel(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (StringUtils.isNotEmpty(getSearchRequest().getLabel())) {
            Join<NumberEntity, AssignmentEntity> assignmentJoin =  root.join(NumberEntity_.assignedTo);
            String sanitisedRegex = getSearchRequest().getLabel().trim();
            predicates.add(
                    cb.equal(cb.function("regex", Boolean.class, assignmentJoin.get("label"), cb.literal(sanitisedRegex)), true)
            );
        }
    }
}
