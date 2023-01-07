/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
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
import com.messagemedia.numbers.service.client.models.Status;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.messagemedia.numbers.repository.config.HibernateTypes.*;

@Entity
@Table(name = "number")
@TypeDefs({
        @TypeDef(name = ENUM_TYPE, typeClass = PostgresEnumType.class),
        @TypeDef(name = ENUM_ARRAY_TYPE, typeClass = PostgresServiceTypeSet.class),
})
@SQLDelete(sql = "UPDATE number SET deleted = NOW() WHERE id = ?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted is NULL")
@Audited
public class NumberEntity {

    @Id
    @Type(type = UUID_TYPE)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    @Type(type = UUID_TYPE)
    private UUID providerId;

    @Column(nullable = false)
    private String country;

    @Enumerated(EnumType.STRING)
    @Type(type = ENUM_TYPE)
    @Column(nullable = false)
    private NumberType type;

    @Enumerated(EnumType.STRING)
    @Type(type = ENUM_TYPE)
    @Column(nullable = false)
    private Classification classification;

    @Type(type = ENUM_ARRAY_TYPE)
    @Column(nullable = false)
    private Set<ServiceType> capabilities;

    @Column
    private OffsetDateTime availableAfter;

    @Column(nullable = false)
    private OffsetDateTime created;

    @Column(nullable = false)
    private OffsetDateTime updated;

    @OneToOne(mappedBy = "numberEntity")
    @NotAudited
    private AssignmentEntity assignedTo;

    @Column(nullable = false)
    private boolean dedicatedReceiver;

    @Enumerated(EnumType.STRING)
    @Type(type = ENUM_TYPE)
    @Column
    private Status status;

    public NumberEntity() {
        this.id = UUID.randomUUID();
    }

    /**
     * make sure availableAfter, created, updated are same in miliseconds
     */
    @PrePersist
    public void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        availableAfter = now;
        created = now;
        updated = now;
    }

    @PreUpdate
    public void onPreUpdate() {
        updated = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public NumberType getType() {
        return type;
    }

    public void setType(NumberType type) {
        this.type = type;
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public OffsetDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(OffsetDateTime updated) {
        this.updated = updated;
    }

    public OffsetDateTime getAvailableAfter() {
        return availableAfter;
    }

    public void setAvailableAfter(OffsetDateTime availableAfter) {
        this.availableAfter = availableAfter;
    }

    public Set<ServiceType> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<ServiceType> capabilities) {
        this.capabilities = capabilities;
    }

    public AssignmentEntity getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(AssignmentEntity assignedTo) {
        this.assignedTo = assignedTo;
    }

    public boolean isDedicatedReceiver() {
        return dedicatedReceiver;
    }

    public void setDedicatedReceiver(Boolean dedicatedReceiver) {
        this.dedicatedReceiver = Boolean.TRUE.equals(dedicatedReceiver);
    }

    public String getVendorAccountIdString() {
        return this.assignedTo.getVendorId() + ":" + this.assignedTo.getAccountId();
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status numberStatus) {
        this.status = numberStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NumberEntity entity = (NumberEntity) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("phoneNumber", phoneNumber)
                .append("providerId", providerId)
                .append("country", country)
                .append("type", type)
                .append("classification", classification)
                .append("created", created)
                .append("updated", updated)
                .append("availableAfter", availableAfter)
                .append("capabilities", capabilities)
                .append("assignedTo", assignedTo)
                .append("dedicatedReceiver", dedicatedReceiver)
                .append("status", status)
                .toString();
    }
}
