/*
 * Copyright (c) Message4U Pty Ltd 2014-2020
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.specification;

import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity_;
import com.messagemedia.numbers.service.client.models.BaseNumberSearchRequest;
import com.messagemedia.numbers.service.client.models.ServiceType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

abstract class BaseNumberSearchSpecification<T extends BaseNumberSearchRequest> implements Specification<NumberEntity> {

    private final T searchRequest;

    BaseNumberSearchSpecification(T searchRequest) {
        this.searchRequest = searchRequest;
    }

    void addToken(Root<NumberEntity> root, CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        if (null != searchRequest.getToken()) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(NumberEntity_.id), searchRequest.getToken()));
        }
    }

    void addServiceType(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (searchRequest.getExactServiceTypes() == null || !searchRequest.getExactServiceTypes()) {
            addFuzzyServiceType(root, cb, predicates);
            return;
        }

        addExactServiceType(root, cb, predicates);
    }

    private void addFuzzyServiceType(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (ArrayUtils.isNotEmpty(searchRequest.getServiceTypes())) {
            predicates.add(cb.or(Arrays.stream(searchRequest.getServiceTypes()).map(
                    serviceType -> cb.equal(
                            cb.function("castServiceType", ServiceType.class, cb.literal(serviceType.name())),
                            cb.function("any", ServiceType.class, root.get("capabilities"))
                    )).toArray(Predicate[]::new)));
        }
    }

    private void addExactServiceType(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (ArrayUtils.isNotEmpty(searchRequest.getServiceTypes())) {
            final String caps = Arrays.stream(searchRequest.getServiceTypes()).map(Enum::name).collect(Collectors.joining(","));
            predicates.add(cb.equal(cb.function("array_sort", java.sql.Array.class, root.get("capabilities")),
                    cb.function("array_sort", java.sql.Array.class, cb.function("castservicetypes", java.sql.Array.class, cb.literal(caps)))
            ));
        }
    }

    void addMatching(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (StringUtils.isNotEmpty(searchRequest.getMatching())) {
            String sanitisedRegex = searchRequest.getMatching().trim();
            // Numbers start with a +, a search for +123456789 should find that number. A regular expression starting with a + is invalid
            // let's do the right thing and not make users have to care about escaping the leading plus.
            if (sanitisedRegex.startsWith("+")) {
                sanitisedRegex = sanitisedRegex.replace("+", "[+]");
            }
            predicates.add(
                    cb.equal(cb.function("regex", Boolean.class, root.get("phoneNumber"), cb.literal(sanitisedRegex)), true)
            );
        }
    }

    void addMatchings(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (ArrayUtils.isNotEmpty(searchRequest.getMatchings())) {
            String sanitisedRegex = Arrays.stream(searchRequest.getMatchings())
                    .map(value -> {
                        value = value.trim();
                        return value.startsWith("+") ? value.replace("+", "[+]") : value;
                    })
                    .collect(Collectors.joining("|"));
            predicates.add(
                    cb.equal(cb.function("regex", Boolean.class, root.get("phoneNumber"), cb.literal(sanitisedRegex)), true)
            );
        }
    }

    void addClassification(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (searchRequest.getClassification() != null) {
            predicates.add(cb.equal(root.get(NumberEntity_.classification), searchRequest.getClassification()));
        }
    }

    void addCountry(Root<NumberEntity> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (StringUtils.isNotEmpty(searchRequest.getCountry())) {
            Predicate equal = cb.equal(root.get(NumberEntity_.country), searchRequest.getCountry().toUpperCase());
            predicates.add(equal);
        }
    }

    void addStatus(Root<NumberEntity> root, CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        if (searchRequest.getStatus() != null) {
            predicates.add(criteriaBuilder.equal(root.get(NumberEntity_.status), searchRequest.getStatus()));
        }
    }

    T getSearchRequest() {
        return searchRequest;
    }
}
