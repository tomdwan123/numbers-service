/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.config.impl.SpringProfileCalculator;
import com.messagemedia.framework.test.DataProviderSpringRunner;
import com.messagemedia.numbers.config.ServiceTestConfig;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static com.messagemedia.numbers.TestData.*;

@ActiveProfiles(SpringProfileCalculator.DEFAULT_ENVIRONMENT)
@ContextConfiguration(
    classes = {ServiceTestConfig.class})
@RunWith(DataProviderSpringRunner.class)
public class AccountReassignVerificationServiceIT {

    private static final int AMS_API_PORT = 12345;

    @Autowired
    private AccountReassignVerificationService accountReassignVerificationService;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(AMS_API_PORT);

    @Before
    public void setup() throws Exception {
        mockUpAmsEndpoint();
    }

    @DataProvider
    public static Object[][] sameParentAccounts() {
        return new Object[][] {
            {"Parent and child", "MessageMedia", "AccountM1", "MessageMedia", "AccountM11"},
            {"Parent and child", "MessageMedia", "AccountM1", "MessageMedia", "AccountM12"},
            {"Two children", "MessageMedia", "AccountM11", "MessageMedia", "AccountM12"},
            {"Grand Parent and nephew", "MessageMedia", "AccountM11", "MessageMedia", "AccountM121"},
            {"Grand Parent and nephew", "MessageMedia", "AccountM11", "MessageMedia", "AccountM122"},
            {"Grand Parent is non-internal", "MessageMedia", "AccountM311", "MessageMedia", "AccountM312"}
        };
    }

    @Test
    @UseDataProvider("sameParentAccounts")
    public void testVerifyAccountRelationshipForTwoRelatedAccount(String message, String vendor1, String account1, String vendor2, String account2)
        throws Exception {
        Assert.assertTrue(message, verify(vendor1, account1, vendor2, account2));
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
        Assert.assertFalse(message, verify(vendor1, account1, vendor2, account2));
    }

    private boolean verify(String vendor1, String account1, String vendor2, String account2) throws Exception {
        return accountReassignVerificationService.verifyAccountRelationship(new VendorAccountId(vendor1,
            account1), new VendorAccountId(vendor2, account2));
    }

}
