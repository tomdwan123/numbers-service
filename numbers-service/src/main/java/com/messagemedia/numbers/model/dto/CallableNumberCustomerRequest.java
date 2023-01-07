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

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallableNumberCustomerRequest {

    private final String id;

    private final String company;

    @JsonCreator
    public CallableNumberCustomerRequest(@JsonProperty("id") String id,
                                         @JsonProperty("company") String company) {
        this.id = id;
        this.company = company;
    }

    public String getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", this.id)
                .append("company", this.company)
                .toString();
    }
}
