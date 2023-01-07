/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.config;

import com.messagemedia.framework.web.config.BaseWebInitializer;

/**
 * This is where spring hooks into the servlet container. Allows us to configure without a web.xml.
 */
public class WorkerInitializer extends BaseWebInitializer {

    @Override
    protected Class<WorkerContext> getWorkerConfigClass() {
        return WorkerContext.class;
    }
}
