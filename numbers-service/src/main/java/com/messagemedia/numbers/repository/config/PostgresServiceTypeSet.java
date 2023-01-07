/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.config;

import com.messagemedia.numbers.service.client.models.ServiceType;

public class PostgresServiceTypeSet extends PostgresEnumSetType<ServiceType> {

    public PostgresServiceTypeSet() {
        super("service_type");
    }
}
