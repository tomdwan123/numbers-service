/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.entities;

import com.messagemedia.numbers.repository.config.PostgresHstoreType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.messagemedia.numbers.repository.config.HibernateTypes.HSTORE_TYPE;

@Entity
@Table(name = "assignment")
@TypeDefs({
        @TypeDef(name = HSTORE_TYPE, typeClass = PostgresHstoreType.class)
})
@SQLDelete(sql = "UPDATE assignment SET deleted = NOW() WHERE id = ?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted is NULL")
@Audited
public class AssignmentEntity {

    @Id
    private UUID id;

    @OneToOne
    @JoinColumn(name = "numberId")
    private NumberEntity numberEntity;

    //use for searchable audit assignment
    @Column(insertable = false, updatable = false)
    private UUID numberId;

    @Column(nullable = false)
    private String vendorId;

    @Column(nullable = false)
    private String accountId;

    @Column
    private String callbackUrl;

    @Type(type = HSTORE_TYPE)
    @Column
    private Map<String, String> externalMetadata;

    @Column(nullable = false)
    private OffsetDateTime created;

    @Column
    @NotAudited
    private OffsetDateTime deleted;

    @Column
    private String label;

    public AssignmentEntity() {
        this.id = UUID.randomUUID();
    }

    @PrePersist
    public void onPrePersist() {
        created = OffsetDateTime.now();
    }

    @PreRemove
    public void onRemove() {
        deleted = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNumberId() {
        return numberId;
    }

    public void setNumberId(UUID numberId) {
        this.numberId = numberId;
    }

    public NumberEntity getNumberEntity() {
        return numberEntity;
    }

    public void setNumberEntity(NumberEntity numberEntity) {
        this.numberEntity = numberEntity;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public OffsetDateTime getDeleted() {
        return deleted;
    }

    public void setDeleted(OffsetDateTime deleted) {
        this.deleted = deleted;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Map<String, String> getExternalMetadata() {
        return externalMetadata;
    }

    public void setExternalMetadata(Map<String, String> externalMetadata) {
        this.externalMetadata = externalMetadata;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AssignmentEntity that = (AssignmentEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("numberEntity", numberEntity)
                .append("vendorId", vendorId)
                .append("accountId", accountId)
                .append("callbackUrl", callbackUrl)
                .append("externalMetadata", externalMetadata)
                .append("created", created)
                .append("deleted", deleted)
                .append("label", label)
                .toString();
    }
}
