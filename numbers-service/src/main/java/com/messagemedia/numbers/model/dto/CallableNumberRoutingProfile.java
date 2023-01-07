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

import java.util.Objects;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallableNumberRoutingProfile {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("steps")
    private List<CallableNumberRoutingProfileStep> steps;

    @JsonCreator
    public CallableNumberRoutingProfile(@JsonProperty("id") String id,
                                        @JsonProperty("name") String name,
                                        @JsonProperty("steps") List<CallableNumberRoutingProfileStep> steps) {
        this.id = id;
        this.name = name;
        this.steps = steps;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CallableNumberRoutingProfileStep> getSteps() {
        return steps;
    }

    public void setSteps(List<CallableNumberRoutingProfileStep> steps) {
        this.steps = steps;
    }

    public String getNumber() {
        return this.steps.get(0).getDirectDetails().getNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CallableNumberRoutingProfile callableNumberRoutingProfile = (CallableNumberRoutingProfile) o;
        return Objects.equals(id, callableNumberRoutingProfile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("steps", steps)
                .toString();
    }
}
