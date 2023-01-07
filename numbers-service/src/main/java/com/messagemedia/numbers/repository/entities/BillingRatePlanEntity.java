/*
 * Copyright (c) Message4U Pty Ltd 2014-2020
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.repository.entities;

import com.messagemedia.numbers.repository.config.PostgresEnumType;
import com.messagemedia.numbers.repository.config.PostgresServiceTypeSet;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberType;
import com.messagemedia.numbers.service.client.models.ServiceType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.messagemedia.numbers.repository.config.HibernateTypes.ENUM_ARRAY_TYPE;
import static com.messagemedia.numbers.repository.config.HibernateTypes.ENUM_TYPE;

@Entity
@Table(name = "billing_rate_plan")
@TypeDefs({
        @TypeDef(name = ENUM_TYPE, typeClass = PostgresEnumType.class),
        @TypeDef(name = ENUM_ARRAY_TYPE, typeClass = PostgresServiceTypeSet.class),
})
@Immutable
public class BillingRatePlanEntity {

    @Id
    private UUID id;

    @Column
    @NotNull
    private String country;

    @Enumerated(EnumType.STRING)
    @Type(type = ENUM_TYPE)
    @Column
    @NotNull
    private Classification classification;

    @Type(type = ENUM_ARRAY_TYPE)
    @Column
    @NotNull
    private Set<ServiceType> capabilities;

    @Enumerated(EnumType.STRING)
    @Type(type = ENUM_TYPE)
    @Column
    @NotNull
    private NumberType type;

    @Column(name = "rate_plan_id")
    @NotNull
    private String ratePlanId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public Set<ServiceType> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<ServiceType> capabilities) {
        this.capabilities = capabilities;
    }

    public NumberType getType() {
        return type;
    }

    public void setType(NumberType type) {
        this.type = type;
    }

    public String getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(String ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BillingRatePlanEntity that = (BillingRatePlanEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("country", country)
                .append("classification", classification)
                .append("capabilities", capabilities)
                .append("type", type)
                .append("ratePlanId", ratePlanId)
                .toString();
    }
}
