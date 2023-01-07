/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.messagemedia.numbers.TestData;
import com.messagemedia.service.accountmanagement.client.model.account.Account;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AccountsHolderTest {

    private AccountsHolder accountsHolder;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        accountsHolder = new AccountsHolder();
        objectMapper.registerModule(new JodaModule());
    }

    @Test
    public void testAddAndLast() throws Exception {
        Account accountM2 = objectMapper.readValue(TestData.loadJson("ams/account_m2.json"), Account.class);
        Assert.assertFalse(accountsHolder.getLast().isPresent());
        accountsHolder.add(accountM2);
        Account last = accountsHolder.getLast().get();
        Assert.assertEquals(last.getVendorAccountId(), accountM2.getVendorAccountId());
        accountsHolder.add(accountM2);
        Account accountM1 = objectMapper.readValue(TestData.loadJson("ams/account_m1.json"), Account.class);
        accountsHolder.add(accountM1);
        last = accountsHolder.getLast().get();
        assertSameAccounts(last, accountM1);
    }

    @Test
    public void testEnd() throws Exception {
        accountsHolder.end();
        Assert.assertTrue(accountsHolder.isEnd());
        Account account = objectMapper.readValue(TestData.loadJson("ams/account_m2.json"), Account.class);
        accountsHolder.add(account);
        Assert.assertFalse(accountsHolder.getLast().isPresent());
    }

    @Test
    public void testEndWhenAccountIsSpecialAccount() throws Exception {
        Account account = objectMapper.readValue(TestData.loadJson("ams/account_root.json"), Account.class);
        accountsHolder.add(account);
        Account accountM2 = objectMapper.readValue(TestData.loadJson("ams/account_m2.json"), Account.class);
        accountsHolder.add(accountM2);
        Assert.assertTrue(accountsHolder.getLast().isPresent());
        Account last = accountsHolder.getLast().get();
        assertSameAccounts(account, last);
    }

    @Test
    public void testIntersect() throws Exception {
        Account accountM1 = objectMapper.readValue(TestData.loadJson("ams/account_m1.json"), Account.class);
        Account accountM2 = objectMapper.readValue(TestData.loadJson("ams/account_m2.json"), Account.class);
        AccountsHolder other = new AccountsHolder().add(accountM1).add(accountM2);

        Account accountM11 = objectMapper.readValue(TestData.loadJson("ams/account_m1_1.json"), Account.class);
        Account accountM2Bis = objectMapper.readValue(TestData.loadJson("ams/account_m2.json"), Account.class);
        accountsHolder = accountsHolder.add(accountM11).add(accountM2Bis);

        final Optional<Account> intersect = accountsHolder.intersect(other);
        Assert.assertTrue(intersect.isPresent());
        assertSameAccounts(accountM2Bis, intersect.get());
        assertSameAccounts(accountM2, intersect.get());
    }

    @Test
    public void testNoIntersect() throws Exception {
        Account accountM12 = objectMapper.readValue(TestData.loadJson("ams/account_m1_2.json"), Account.class);
        Account accountM2 = objectMapper.readValue(TestData.loadJson("ams/account_m2.json"), Account.class);
        AccountsHolder other = new AccountsHolder().add(accountM12).add(accountM2);

        Account accountM11 = objectMapper.readValue(TestData.loadJson("ams/account_m1_1.json"), Account.class);
        Account accountM2Bis = objectMapper.readValue(TestData.loadJson("ams/account_m1.json"), Account.class);
        accountsHolder = accountsHolder.add(accountM11).add(accountM2Bis);

        Assert.assertFalse(accountsHolder.intersect(other).isPresent());
    }

    private void assertSameAccounts(Account expected, Account account) {
        Assert.assertEquals(expected.getVendorAccountId(), account.getVendorAccountId());
    }
}
