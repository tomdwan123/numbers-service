/*
 * Copyright (c) Message4U Pty Ltd 2014-2022
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

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallableNumberCreateResponse {

    private final String id;
    private final String customerId;
    private final String number;
    private final String name;
    private final String routingProfileId;
    private final CallableNumberRoutingProfile routingProfile;

    @JsonCreator
    public CallableNumberCreateResponse(@JsonProperty("id") String id,
                                        @JsonProperty("customerId") String customerId,
                                        @JsonProperty("number") String number,
                                        @JsonProperty("name") String name,
                                        @JsonProperty("routingProfileId") String routingProfileId,
                                        @JsonProperty("routingProfile") CallableNumberRoutingProfile routingProfile) {
        this.id = id;
        this.customerId = customerId;
        this.number = number;
        this.name = name;
        this.routingProfileId = routingProfileId;
        this.routingProfile = routingProfile;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getRoutingProfileId() {
        return routingProfileId;
    }

    public CallableNumberRoutingProfile getRoutingProfile() {
        return routingProfile;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("customerId", customerId)
                .append("number", number)
                .append("name", name)
                .append("routingProfileId", routingProfileId)
                .append("routingProfile", routingProfile)
                .toString();
    }
}
