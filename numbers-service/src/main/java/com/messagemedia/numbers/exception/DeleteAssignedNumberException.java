/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.exception;

import java.util.UUID;

public class DeleteAssignedNumberException extends RuntimeException {

    public DeleteAssignedNumberException(UUID numberId) {
        super(String.format("Cannot delete number with id {%s} while assigned to an account", numberId));
    }
}
