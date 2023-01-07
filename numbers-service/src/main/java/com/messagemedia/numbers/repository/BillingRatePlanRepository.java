/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository;

import com.messagemedia.numbers.repository.entities.BillingRatePlanEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BillingRatePlanRepository extends JpaRepository<BillingRatePlanEntity, UUID> {

    @Query("select p from BillingRatePlanEntity p "
            + "where p.country = :#{#number.country} "
            + "and p.classification = :#{#number.classification} "
            + "and p.type = :#{#number.type} ")
    List<BillingRatePlanEntity> findByNumber(@Param("number") NumberEntity number);
}
