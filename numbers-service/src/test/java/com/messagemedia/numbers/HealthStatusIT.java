/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers;

import com.messagemedia.framework.config.BuildVersion;
import org.junit.Test;

import static com.messagemedia.framework.test.HealthPageAsserter.assertActive;
import static com.messagemedia.numbers.TestData.WEB_CONTAINER_PORT;

public class HealthStatusIT {

    @Test
    public void shouldReturnHealthPage() throws Exception {
        assertActive("localhost", Integer.parseInt(new BuildVersion().getValue(WEB_CONTAINER_PORT)));
    }
}
