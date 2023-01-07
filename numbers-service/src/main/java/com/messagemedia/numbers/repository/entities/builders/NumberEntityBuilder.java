/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.entities.builders;

import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.Classification;
import com.messagemedia.numbers.service.client.models.NumberType;
import com.messagemedia.numbers.service.client.models.ServiceType;

import java.util.Set;
import java.util.UUID;

public final class NumberEntityBuilder {

    private String phoneNumber;
    private UUID providerId;
    private String country;
    private NumberType type;
    private Classification classification;
    private Set<ServiceType> capabilities;
    private boolean dedicatedReceiver;

    public static NumberEntityBuilder aNumberEntity() {
        return new NumberEntityBuilder();
    }

    public NumberEntityBuilder withPhoneNumber(String aPhoneNumber) {
        this.phoneNumber = aPhoneNumber;
        return this;
    }

    public NumberEntityBuilder withProviderId(UUID aProviderId) {
        this.providerId = aProviderId;
        return this;
    }

    public NumberEntityBuilder withCountry(String aCountry) {
        this.country = aCountry;
        return this;
    }

    public NumberEntityBuilder withType(NumberType aType) {
        this.type = aType;
        return this;
    }

    public NumberEntityBuilder withClassification(Classification aClassification) {
        this.classification = aClassification;
        return this;
    }

    public NumberEntityBuilder withCapabilities(Set<ServiceType> capabilitiyList) {
        this.capabilities = capabilitiyList;
        return this;
    }

    public NumberEntityBuilder withDedicatedReceiver(Boolean dedicatedReceiverFlag) {
        this.dedicatedReceiver = Boolean.TRUE.equals(dedicatedReceiverFlag);
        return this;
    }

    public NumberEntity build() {
        NumberEntity numberEntity = new NumberEntity();
        numberEntity.setPhoneNumber(phoneNumber);
        numberEntity.setProviderId(providerId);
        numberEntity.setCountry(country);
        numberEntity.setType(type);
        numberEntity.setClassification(classification);
        numberEntity.setCapabilities(capabilities);
        numberEntity.setDedicatedReceiver(dedicatedReceiver);
        return numberEntity;
    }
}
