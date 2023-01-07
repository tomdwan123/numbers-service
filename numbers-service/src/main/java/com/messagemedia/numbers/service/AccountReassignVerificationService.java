/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.service;

import com.messagemedia.domainmodels.accounts.AccountId;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.service.helper.AccountsHolder;
import com.messagemedia.service.accountmanagement.client.ContextualServiceAccountManagementClientFactory;
import com.messagemedia.service.accountmanagement.client.exception.ServiceAccountManagementException;
import com.messagemedia.service.accountmanagement.client.model.account.Account;
import com.messagemedia.service.accountmanagement.client.model.account.Type;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountReassignVerificationService {

    private final ContextualServiceAccountManagementClientFactory contextualServiceAccountManagementClientFactory;

    @Autowired
    public AccountReassignVerificationService(ContextualServiceAccountManagementClientFactory contextualServiceAccountManagementClientFactory) {
        this.contextualServiceAccountManagementClientFactory = contextualServiceAccountManagementClientFactory;
    }

    public boolean verifyAccountRelationship(VendorAccountId vendorAccountId, VendorAccountId otherVendorAccountId)
        throws ServiceAccountManagementException {
        if (!vendorAccountId.getVendorId().equals(otherVendorAccountId.getVendorId())
                || vendorAccountId.equals(otherVendorAccountId)) {
            return false;
        }
        final Account account = this.getAccount(vendorAccountId);
        final Account theOther = this.getAccount(otherVendorAccountId);
        return verify(account, theOther, new AccountsHolder().add(account), new AccountsHolder().add(theOther)).isPresent();
    }

    private Optional<Account> verify(Account account, Account theOther, AccountsHolder ancestorsCurrent, AccountsHolder ancestorsTheOther)
        throws ServiceAccountManagementException {
        if (ancestorsCurrent.isEnd() && ancestorsTheOther.isEnd()) {
            return Optional.empty();
        }
        final AccountsHolder ancestorsOfCurrent = getAncestors(account, ancestorsCurrent);
        final AccountsHolder ancestorsOfTheOther = getAncestors(theOther, ancestorsTheOther);
        final Optional<Account> common = ancestorsOfCurrent.intersect(ancestorsOfTheOther);
        if (!common.isPresent()) {
            return verify(ancestorsCurrent.getLast().get(), ancestorsOfTheOther.getLast().get(), ancestorsOfCurrent, ancestorsOfTheOther);
        } else if (common.get().getType() == Type.INTERNAL) {
            return verifyCommonAncestors(common.get());
        }
        return common;
    }

    private Optional<Account> verifyCommonAncestors(Account account) throws ServiceAccountManagementException {
        final Optional<AccountId> parentAccountId = account.getParentAccount();
        if (parentAccountId.isPresent()) {
            final VendorAccountId parentVendorAccountId = new VendorAccountId(account.getVendorAccountId().getVendorId().getVendorId(),
                parentAccountId.get().getAccountId());
            final Account acct = getAccount(parentVendorAccountId);
            if (isInternalAndNotRootAccounts(acct)) {
                return verifyCommonAncestors(acct);
            } else if (acct.getType() != Type.INTERNAL) {
                return Optional.of(acct);
            }
        }
        return Optional.empty();
    }

    private boolean isInternalAndNotRootAccounts(Account acct) {
        VendorAccountId vendorAccountId = acct.getVendorAccountId();
        final boolean isSpecial = vendorAccountId.getVendorId().getVendorId().equals(vendorAccountId.getAccountId().getAccountId());
        return acct.getType() == Type.INTERNAL && !isSpecial;
    }

    private AccountsHolder getAncestors(Account account, AccountsHolder accountsHolder) throws ServiceAccountManagementException {
        if (accountsHolder.isEnd()) {
            return accountsHolder;
        }
        final Optional<AccountId> optParentId = account.getParentAccount();
        if (optParentId.isPresent()) {
            final Account parent = this.getAccount(new VendorAccountId(account.getVendorAccountId().getVendorId().getVendorId(),
                optParentId.get().getAccountId()));
            accountsHolder.add(parent);
        } else {
            accountsHolder.end();
        }
        return accountsHolder;
    }

    private Account getAccount(VendorAccountId vendorAccountId) throws ServiceAccountManagementException {
        return contextualServiceAccountManagementClientFactory.createContextualClient(
            new VendorAccountId(vendorAccountId.getVendorId().getVendorId(),
                vendorAccountId.getAccountId().getAccountId())).getAccount(vendorAccountId.getAccountId());
    }

}
