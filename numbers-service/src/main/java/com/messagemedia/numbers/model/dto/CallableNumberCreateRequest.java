/*
 * Copyright (c) Message4U Pty Ltd 2014-2021
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallableNumberCreateRequest {

    private final String customerId;

    private String name;

    private final String number;

    private final String routingProfileId;

    @JsonCreator
    public CallableNumberCreateRequest(@JsonProperty("customerId") String customerId,
                                       @JsonProperty("number") String number,
                                       @JsonProperty("routingProfileId") String routingProfileId) {
        this.customerId = customerId;
        this.number = number;
        this.routingProfileId = routingProfileId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public String getNumber() {
        return number;
    }

    public String getRoutingProfileId() {
        return routingProfileId;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("customerId", this.customerId)
                .append("name", this.name)
                .append("number", this.number)
                .append("routingProfileId", this.routingProfileId)
                .toString();
    }
}
