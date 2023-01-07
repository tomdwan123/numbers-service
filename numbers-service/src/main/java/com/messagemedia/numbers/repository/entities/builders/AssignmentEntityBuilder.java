/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.entities.builders;

import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;

import java.time.OffsetDateTime;
import java.util.Map;

public final class AssignmentEntityBuilder {

    private NumberEntity numberEntity;
    private String vendorId;
    private String accountId;
    private OffsetDateTime deleted;
    private String callbackUrl;
    private Map<String, String> externalMetadata;
    private String label;

    private AssignmentEntityBuilder() {
    }

    public static AssignmentEntityBuilder anAssignmentEntity() {
        return new AssignmentEntityBuilder();
    }

    public AssignmentEntityBuilder withNumberEntity(NumberEntity aNumberEntity) {
        this.numberEntity = aNumberEntity;
        return this;
    }

    public AssignmentEntityBuilder withVendorId(String aVendorId) {
        this.vendorId = aVendorId;
        return this;
    }

    public AssignmentEntityBuilder withAccountId(String anAccountId) {
        this.accountId = anAccountId;
        return this;
    }

    public AssignmentEntityBuilder withDeleted(OffsetDateTime aDeletedDate) {
        this.deleted = aDeletedDate;
        return this;
    }

    public AssignmentEntityBuilder withCallbackUrl(String aCallbackUrl) {
        this.callbackUrl = aCallbackUrl;
        return this;
    }

    public AssignmentEntityBuilder withExternalMetadata(Map<String, String> anExternalMetadata) {
        this.externalMetadata = anExternalMetadata;
        return this;
    }

    public AssignmentEntityBuilder withLabel(String labelValue) {
        this.label = labelValue;
        return this;
    }

    public AssignmentEntity build() {
        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setNumberEntity(numberEntity);
        assignment.setVendorId(vendorId);
        assignment.setAccountId(accountId);
        assignment.setDeleted(deleted);
        assignment.setCallbackUrl(callbackUrl);
        assignment.setExternalMetadata(externalMetadata);
        assignment.setLabel(label);
        return assignment;
    }
}
