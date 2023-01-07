/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.exception;

import com.messagemedia.domainmodels.accounts.VendorAccountId;

public class InvalidAccountRelationshipException extends RuntimeException {

    public InvalidAccountRelationshipException(VendorAccountId assignee) {
        super(String.format("Vendor account id {%s} has an invalid relationship with the number owner account", assignee));
    }
}
