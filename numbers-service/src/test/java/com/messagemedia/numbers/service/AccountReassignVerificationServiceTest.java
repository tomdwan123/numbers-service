/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.messagemedia.domainmodels.accounts.AccountId;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.numbers.TestData;
import com.messagemedia.service.accountmanagement.client.ContextualServiceAccountManagementClientFactory;
import com.messagemedia.service.accountmanagement.client.ServiceAccountManagementClient;
import com.messagemedia.service.accountmanagement.client.exception.ServiceAccountManagementException;
import com.messagemedia.service.accountmanagement.client.model.account.Account;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

@RunWith(DataProviderRunner.class)
public class AccountReassignVerificationServiceTest {

    private AccountReassignVerificationService accountReassignVerificationService;

    @Mock
    private ContextualServiceAccountManagementClientFactory contextualServiceAccountManagementClientFactory;

    @Mock
    private ServiceAccountManagementClient serviceAccountManagementClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        objectMapper.registerModule(new JodaModule());
        accountReassignVerificationService = new AccountReassignVerificationService(contextualServiceAccountManagementClientFactory);
        when(contextualServiceAccountManagementClientFactory.createContextualClient(any(VendorAccountId.class)))
                .thenReturn(serviceAccountManagementClient);

        mockAmsClient("ams/account_root.json", "MessageMedia");
        mockAmsClient("ams/account_m1.json", "AccountM1");
        mockAmsClient("ams/account_m2.json", "AccountM2");
        mockAmsClient("ams/account_m1_1.json", "AccountM11");
        mockAmsClient("ams/account_m1_2.json", "AccountM12");
        mockAmsClient("ams/account_m1_2_1.json", "AccountM121");
        mockAmsClient("ams/account_m1_2_2.json", "AccountM122");
        mockAmsClient("ams/account_m2.json", "AccountM2");
        mockAmsClient("ams/account_c1.json", "AccountC1");
        mockAmsClient("ams/account_m3.json", "AccountM3");
        mockAmsClient("ams/account_m3_1.json", "AccountM31");
        mockAmsClient("ams/account_m3_1_1.json", "AccountM311");
        mockAmsClient("ams/account_m3_1_2.json", "AccountM312");
        mockAmsClient("ams/account_m4.json", "AccountM4");
    }

    @DataProvider
    public static Object[][] sameParentAccounts() {
        return new Object[][] {
            {"Parent and child", "MessageMedia", "AccountM1", "MessageMedia", "AccountM11"},
            {"Parent and child", "MessageMedia", "AccountM1", "MessageMedia", "AccountM12"},
            {"Two children", "MessageMedia", "AccountM11", "MessageMedia", "AccountM12"},
            {"Grand Parent and nephew", "MessageMedia", "AccountM11", "MessageMedia", "AccountM121"},
            {"Grand Parent and nephew", "MessageMedia", "AccountM11", "MessageMedia", "AccountM122"},
            {"Grand Parent is non-internal", "MessageMedia", "AccountM311", "MessageMedia", "AccountM312"},
        };
    }

    @Test
    @UseDataProvider("sameParentAccounts")
    public void testVerifyAccountRelationshipForTwoRelatedAccount(String message, String vendor1, String account1, String vendor2, String account2)
        throws Exception {
        Assert.assertTrue(message, verifyRelation(vendor1, account1, vendor2, account2));
    }

    @DataProvider
    public static Object[][] noRelationAccounts() {
        return new Object[][] {
            {"Same Internal Parent", "MessageMedia", "AccountM1", "MessageMedia", "AccountM2"},
            {"Not the same vendor", "MessageMediaDiff", "AccountC1", "MessageMedia", "AccountM12"},
            {"Not the same parent", "MessageMedia", "AccountM311", "MessageMedia", "AccountM4"},
            {"Not the same parent", "MessageMedia", "AccountM311", "MessageMedia", "AccountM311"}
        };
    }

    @Test
    @UseDataProvider("noRelationAccounts")
    public void testVerifyAccountWithoutRelationship(String message, String vendor1, String account1, String vendor2,
        String account2) throws Exception {
        Assert.assertFalse(message, verifyRelation(vendor1, account1, vendor2, account2));
    }

    @Test(expected = ServiceAccountManagementException.class)
    public void testThrowExceptionInAmsClientSupport() throws Exception {
        when(serviceAccountManagementClient.getAccount(any())).thenThrow(new ServiceAccountManagementException("Cannot get account"));
        verifyRelation("vendor1", "account1", "vendor1", "account2");
    }

    private boolean verifyRelation(String vendor1, String account1, String vendor2, String account2)
        throws Exception {
        return accountReassignVerificationService.verifyAccountRelationship(new VendorAccountId(vendor1,
            account1), new VendorAccountId(vendor2, account2));
    }

    private void mockAmsClient(String path, String name) throws Exception {
        when(serviceAccountManagementClient.getAccount(new AccountId(name))).thenReturn(objectMapper.readValue(TestData.loadJson(path),
            Account.class));
    }
}
