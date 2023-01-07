/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service.helper;

import com.google.common.collect.Sets;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.service.accountmanagement.client.model.account.Account;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;

public class AccountsHolder {
    private final Map<VendorAccountId, Account> accounts = new LinkedHashMap<>();
    private boolean end = false;

    public void end() {
        this.end = true;
    }

    public AccountsHolder add(Account account) {
        if (!end) {
            this.accounts.putIfAbsent(account.getVendorAccountId(), account);
        }
        if (isSpecialAccount(account)) {
            this.end();
        }
        return this;
    }

    public Optional<Account> getLast() {
        if (this.accounts.isEmpty()) {
            return Optional.empty();
        }
        VendorAccountId vendorAccountId = CollectionUtils.get(accounts.keySet(), accounts.size() - 1);
        return Optional.of(accounts.get(vendorAccountId));
    }

    public Optional<Account> intersect(AccountsHolder accountsHolder) {
        Collection<VendorAccountId> vendorAccountIds = Sets.intersection(this.accounts.keySet(), accountsHolder.accounts.keySet());
        return vendorAccountIds.stream().map(v -> accounts.get(v)).findFirst();
    }

    public boolean isEnd() {
        return end;
    }

    /**
     * account that has pattern vendor_id: vendor and account_id: vendor
     */
    private boolean isSpecialAccount(Account account) {
        final VendorAccountId vendorAccountId = account.getVendorAccountId();
        return vendorAccountId.getVendorId().getVendorId().equals(vendorAccountId.getAccountId().getAccountId());
    }
}
