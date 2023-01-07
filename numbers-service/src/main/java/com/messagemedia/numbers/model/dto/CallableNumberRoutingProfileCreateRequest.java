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

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallableNumberRoutingProfileCreateRequest {

    private final String customerId;
    private final String name;
    private final List<CallableNumberRoutingProfileStep> steps;

    @JsonCreator
    public CallableNumberRoutingProfileCreateRequest(@JsonProperty("customerId") String customerId,
                                                     @JsonProperty("name") String name,
                                                     @JsonProperty("steps") List<CallableNumberRoutingProfileStep> steps) {
        this.customerId = customerId;
        this.name = name;
        this.steps = steps;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public List<CallableNumberRoutingProfileStep> getSteps() {
        return steps;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("customerId", customerId)
                .append("name", name)
                .append("steps", steps)
                .toString();
    }
}
