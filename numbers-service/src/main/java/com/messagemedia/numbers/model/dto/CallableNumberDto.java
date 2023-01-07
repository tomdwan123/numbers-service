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

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallableNumberDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("number")
    private String number;

    @JsonProperty("routingProfile")
    private CallableNumberRoutingProfile routingProfile;

    @JsonCreator
    public CallableNumberDto(@JsonProperty("id") String id,
                             @JsonProperty("name") String name,
                             @JsonProperty("number") String number,
                             @JsonProperty("routingProfile") CallableNumberRoutingProfile routingProfile) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.routingProfile = routingProfile;
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public CallableNumberRoutingProfile getRoutingProfile() {
        return routingProfile;
    }

    public void setRoutingProfile(CallableNumberRoutingProfile routingProfile) {
        this.routingProfile = routingProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CallableNumberDto callableNumberDto = (CallableNumberDto) o;
        return Objects.equals(id, callableNumberDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", this.id)
                .append("name", this.name)
                .append("number", this.number)
                .append("routingProfile", this.routingProfile)
                .toString();
    }
}
