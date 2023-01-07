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
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity_;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest;
import com.messagemedia.numbers.service.client.models.ServiceType;
import com.messagemedia.numbers.service.client.models.Status;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.UUID;

import static com.messagemedia.numbers.service.client.models.NumberAssignmentSearchRequest.NumberAssignmentSearchRequestBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class NumberAssignmentSearchSpecificationTest {

    @Mock
    private Root<NumberEntity> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate predicate;

    @Mock
    private Join<NumberEntity, AssignmentEntity> assignmentJoin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFindByCriteria() {
        when(criteriaBuilder.and(anyVararg())).thenReturn(predicate);
        when(root.join(NumberEntity_.assignedTo)).thenReturn(assignmentJoin);

        NumberAssignmentSearchRequest searchRequest = NumberAssignmentSearchRequestBuilder.aNumberAssignmentSearchRequestBuilder()
                .withVendorId("vendorId")
                .withAccountId("accountId")
                .withPageSize(5)
                .withToken(UUID.randomUUID())
                .withCountry("AU")
                .withLabel("MyLabel%")
                .withClassification(Classification.BRONZE)
                .withMatching("+13_6%")
                .withServiceTypes(new ServiceType[]{ServiceType.TTS, ServiceType.SMS})
                .withStatus(Status.PENDING)
                .build();

        NumberAssignmentSearchSpecification specification = new NumberAssignmentSearchSpecification(searchRequest);
        assertNotNull(specification);
        assertThat(specification.toPredicate(root, query, criteriaBuilder), equalTo(predicate));
        verify(criteriaBuilder).and(anyVararg());
    }

    @DataProvider
    public static Object[][] accountsData() {
        return new Object[][]{
                {new String[]{}},
                {new String[]{"all"}},
                {new String[]{"all, accountIdTest1"}},
                {new String[]{"accountIdTest1", "accountIdTest2, accountIdTest4"}}
        };
    }
    @Test
    @UseDataProvider("accountsData")
    public void testFindByCriteriaWithAccounts(String[] accounts) {
        when(criteriaBuilder.and(anyVararg())).thenReturn(predicate);
        when(root.join(NumberEntity_.assignedTo)).thenReturn(assignmentJoin);

        NumberAssignmentSearchRequest searchRequest = NumberAssignmentSearchRequestBuilder.aNumberAssignmentSearchRequestBuilder()
                .withVendorId("vendorId")
                .withAccountId("accountId")
                .withPageSize(5)
                .withToken(UUID.randomUUID())
                .withCountry("AU")
                .withLabel("MyLabel%")
                .withClassification(Classification.BRONZE)
                .withMatching("+13_6%")
                .withServiceTypes(new ServiceType[]{ServiceType.TTS, ServiceType.SMS})
                .withStatus(Status.PENDING)
                .withAccounts(accounts)
                .build();

        NumberAssignmentSearchSpecification specification = new NumberAssignmentSearchSpecification(searchRequest);
        assertNotNull(specification);
        assertThat(specification.toPredicate(root, query, criteriaBuilder), equalTo(predicate));
        verify(criteriaBuilder).and(anyVararg());
    }

    @DataProvider
    public static Object[][] matchingsData() {
        return new Object[][]{
                {new String[]{}},
                {new String[]{"1800"}},
                {new String[]{"+13_6%", "+172 "}}
        };
    }

    @Test
    @UseDataProvider("matchingsData")
    public void testFindByCriteriaWithMatchings(String[] matchings) {
        when(criteriaBuilder.and(anyVararg())).thenReturn(predicate);
        when(root.join(NumberEntity_.assignedTo)).thenReturn(assignmentJoin);

        NumberAssignmentSearchRequest searchRequest = NumberAssignmentSearchRequestBuilder.aNumberAssignmentSearchRequestBuilder()
                .withVendorId("vendorId")
                .withAccountId("accountId")
                .withPageSize(5)
                .withToken(UUID.randomUUID())
                .withCountry("AU")
                .withLabel("MyLabel%")
                .withClassification(Classification.BRONZE)
                .withMatching("+13_6%")
                .withServiceTypes(new ServiceType[]{ServiceType.TTS, ServiceType.SMS})
                .withStatus(Status.PENDING)
                .withMatchings(matchings)
                .build();

        NumberAssignmentSearchSpecification specification = new NumberAssignmentSearchSpecification(searchRequest);
        assertNotNull(specification);
        assertThat(specification.toPredicate(root, query, criteriaBuilder), equalTo(predicate));
        verify(criteriaBuilder).and(anyVararg());
    }

    @Test(expected = VendorAccountRequiredException.class)
    public void shouldThrowExceptionOnFindByCriteriaWithNoVendorAccount() {
        new NumberAssignmentSearchSpecification(new NumberAssignmentSearchRequest());
    }
}
