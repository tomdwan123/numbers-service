/*
 * Copyright (c) Message4U Pty Ltd 2014-2020
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.service.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BillingNotificationRequest {

    @JsonProperty
    private final String productRatePlanId;

    public BillingNotificationRequest(String productRatePlanId) {
        this.productRatePlanId = productRatePlanId;
    }

    public String getProductRatePlanId() {
        return productRatePlanId;
    }
}
