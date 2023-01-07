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
public class CallableNumberUpdateRequest {

    private final String routingProfileId;
    private final String name;

    @JsonCreator
    public CallableNumberUpdateRequest(@JsonProperty("routingProfileId") String routingProfileId,
                                       @JsonProperty("name") String name) {
        this.routingProfileId = routingProfileId;
        this.name = name;
    }

    public String getRoutingProfileId() {
        return routingProfileId;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("routingProfileId", this.routingProfileId)
                .append("name", this.name)
                .toString();
    }
}
