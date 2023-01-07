/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.specification;

import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.AssignmentEntity_;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity_;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberSearchRequest;
import com.messagemedia.numbers.service.client.models.ServiceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.persistence.criteria.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.messagemedia.numbers.service.client.models.NumberSearchRequest.NumberSearchRequestBuilder.aNumberSearchRequestBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class NumberSearchSpecificationTest {
    @Mock
    private Root<NumberEntity> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate predicate;

    @Mock
    private Path<AssignmentEntity> assignmentPath;

    @Mock
    private Path<UUID> assignmentIdPath;

    @Test
    public void testFindByCriteria() {
        when(criteriaBuilder.and(anyVararg())).thenReturn(predicate);
        when(root.get(NumberEntity_.assignedTo)).thenReturn(assignmentPath);
        when(assignmentPath.get(AssignmentEntity_.id)).thenReturn(assignmentIdPath);

        NumberSearchRequest searchCriteria =
                aNumberSearchRequestBuilder()
                        .withPageSize(25)
                        .withToken(UUID.randomUUID())
                        .withCountry("AU")
                        .withAssigned(true)
                        .withClassification(Classification.BRONZE)
                        .withMatching("+13_6%")
                        .withServiceTypes(new ServiceType[]{ServiceType.TTS, ServiceType.SMS})
                        .withAvailableBy(OffsetDateTime.now())
                        .build();
        NumberSearchSpecification specification = new NumberSearchSpecification(searchCriteria);
        assertNotNull(specification);
        assertEquals(predicate, specification.toPredicate(root, query, criteriaBuilder));
        verify(criteriaBuilder).and(anyVararg());
    }

    @Test
    public void testFindByCriteriaWithCapability() {
        when(criteriaBuilder.and(anyVararg())).thenReturn(predicate);
        when(root.get(NumberEntity_.assignedTo)).thenReturn(assignmentPath);
        when(assignmentPath.get(AssignmentEntity_.id)).thenReturn(assignmentIdPath);

        NumberSearchRequest searchCriteria =
                aNumberSearchRequestBuilder()
                        .withPageSize(25)
                        .withToken(UUID.randomUUID())
                        .withCountry("AU")
                        .withAssigned(true)
                        .withClassification(Classification.BRONZE)
                        .withMatching("+13_6%")
                        .withServiceTypes(new ServiceType[]{ServiceType.TTS, ServiceType.SMS})
                        .withExactServiceTypes(Boolean.TRUE)
                        .withAvailableBy(OffsetDateTime.now())
                        .build();
        NumberSearchSpecification specification = new NumberSearchSpecification(searchCriteria);
        assertNotNull(specification);
        assertEquals(predicate, specification.toPredicate(root, query, criteriaBuilder));
        verify(criteriaBuilder).and(anyVararg());
    }
}
