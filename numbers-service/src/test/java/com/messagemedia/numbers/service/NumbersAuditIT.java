/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.messagemedia.numbers.repository.NumbersRepository;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import java.time.OffsetDateTime;
import java.util.List;

import static com.messagemedia.numbers.TestData.randomUnassignedNumberEntity;
import static org.junit.Assert.assertEquals;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(classes = {ServiceTestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class NumbersAuditIT {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    @Autowired
    private NumbersRepository numbersRepository;

    @Test
    public void shouldSaveNumbersAuditCorrectly() {
        //add new number & verify audit
        NumberEntity numberEntity = numbersRepository.saveAndFlush(randomUnassignedNumberEntity());
        AuditReader reader = AuditReaderFactory.get(entityManager);
        AuditQuery auditQuery = reader.createQuery().forRevisionsOfEntity(NumberEntity.class, true, true);
        auditQuery.add(AuditEntity.id().eq(numberEntity.getId()));
        List<NumberEntity> auditNumbers = auditQuery.getResultList();

        assertEquals(1, auditNumbers.size());
        assertEquals(numberEntity, auditNumbers.get(0));

        //update availableAfter of number & verify
        OffsetDateTime newAvailableAfter = OffsetDateTime.now().plusDays(1);
        numberEntity.setAvailableAfter(newAvailableAfter);
        numbersRepository.save(numberEntity);

        auditNumbers = auditQuery.getResultList();
        assertEquals(2, auditNumbers.size());
        assertEquals(newAvailableAfter, auditNumbers.get(1).getAvailableAfter());

        //delete number & verify
        numbersRepository.deleteById(numberEntity.getId());
        auditNumbers = auditQuery.getResultList();
        assertEquals(3, auditNumbers.size());
        assertEquals(newAvailableAfter, auditNumbers.get(2).getAvailableAfter());
    }
}
