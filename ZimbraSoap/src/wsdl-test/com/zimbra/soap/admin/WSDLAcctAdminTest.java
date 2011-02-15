/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin;

import java.util.List;

import com.sun.xml.ws.developer.WSBindingProvider;

import com.zimbra.soap.admin.wsimport.generated.*;

import com.zimbra.soap.Utility;

import org.junit.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WSDLAcctAdminTest {

    // The AdminService interface is the Java type bound to
    // the portType section of the WSDL document.
    private final static String testAcctDomain =
            "wsdl.acct.domain.example.test";
    private final static String testAcct = "wsdl1@" + testAcctDomain;
    private final static String testCos = "wsdl.cos.example.test";
    private static AdminService eif = null;

    @BeforeClass
    public static void init() throws Exception {
        Utility.setUpToAcceptAllHttpsServerCerts();
        eif = Utility.getAdminSvcEIF();
        oneTimeTearDown();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        // one-time cleanup code
        try {
            Utility.deleteAccountIfExists(testAcct);
            Utility.deleteAccountIfExists("foobar" + testAcct);
            Utility.deleteDomainIfExists(testAcctDomain);
            Utility.deleteCosIfExists(testCos);
        } catch (Exception ex) {
            System.err.println("Exception " + ex.toString() + 
            " thrown inside oneTimeTearDown");
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void accountAliasTest() throws Exception {
        String testAccountId = Utility.ensureAccountExists(testAcct);
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        AddAccountAliasRequest req = new AddAccountAliasRequest();
        req.setId(testAccountId);
        req.setAlias("alias1@" + testAcctDomain);
        AddAccountAliasResponse resp = eif.addAccountAliasRequest(req);
        Assert.assertNotNull("AddAccountAliasResponse object", resp);
        RemoveAccountAliasRequest removeReq = new RemoveAccountAliasRequest();
        removeReq.setId(testAccountId);
        removeReq.setAlias("alias1@" + testAcctDomain);
        RemoveAccountAliasResponse removeResp =
                eif.removeAccountAliasRequest(removeReq);
        Assert.assertNotNull("RemoveAccountAliasResponse object", removeResp);
    }

    @Test
    public void foreignPrincGetAccountTest() throws Exception {
        GetAccountRequest req = new GetAccountRequest();
        AccountSelector acct = new AccountSelector();
        acct.setBy(AccountBy.NAME);
        acct.setValue("user1");
        req.setAccount(acct);
        req.setAttrs("zimbraForeignPrincipal");
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        GetAccountResponse resp = eif.getAccountRequest(req);
        Assert.assertNotNull("GetAccountResponse object", resp);
        AccountInfo acctInfo = resp.getAccount();
        Assert.assertNotNull("AccountInfo object", acctInfo);
        Assert.assertTrue("value of <account> 'name' attribute should start with 'user1@'",
                acctInfo.getName().startsWith("user1@"));
        int len = acctInfo.getId().length();
        Assert.assertTrue("length of <account> 'id' attribute length is " + len +
                " - should be longer than 10", len > 10);
        List <Attr> attrs = acctInfo.getA();
        len = attrs.size();
        Assert.assertTrue("<account> has " + len +
                " <a> children - should have only 1", len == 1);
        Assert.assertEquals("'n' attribute of <a> -", "zimbraForeignPrincipal",
                attrs.get(0).getN());
    }

    @Test
    public void createAccountTest() throws Exception {
        int len;
        Utility.deleteAccountIfExists(testAcct);
        Utility.ensureDomainExists(testAcctDomain);
        CreateAccountRequest createAcctReq = new CreateAccountRequest();
        createAcctReq.setName(testAcct);
        createAcctReq.setPassword("test123");
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        CreateAccountResponse resp = eif.createAccountRequest(createAcctReq);
        Assert.assertNotNull("CreateAccountResponse object", resp);
        AccountInfo accountInfo = resp.getAccount();
        Assert.assertNotNull("AccountInfo object", accountInfo);
        Assert.assertEquals("createAccountResponse <account> 'name' attribute",
                testAcct, accountInfo.getName());
        String testAccountId = accountInfo.getId();
        len = testAccountId.length();
        Assert.assertTrue("length of CreateAccountResponse <account> 'id' attribute length is " +
                len + " - should be longer than 10", len > 10);
        len = accountInfo.getA().size();
        Assert.assertTrue("CreateAccountResponse <account> has " +
                len + " <a> children - should have at least 12", len >= 12);
    }

    @Test
    public void getAccountInfoTest() throws Exception {
        int len;
        String testAccountId = Utility.ensureAccountExists(testAcct);
        GetAccountInfoRequest getInfoReq = new GetAccountInfoRequest();
        AccountSelector accountSel = new AccountSelector();
        accountSel.setBy(AccountBy.ID);
        accountSel.setValue(testAccountId);
        getInfoReq.setAccount(accountSel);
        GetAccountInfoResponse getInfoResp = eif.getAccountInfoRequest(getInfoReq);
        Assert.assertNotNull("GetAccountInfoResponse object", getInfoResp);
        CosInfo cos = getInfoResp.getCos();
        String acctName = getInfoResp.getName();
        List <String> soapURL = getInfoResp.getSoapURL();
        String adminSoapURL = getInfoResp.getAdminSoapURL();
        String publicMailURL = getInfoResp.getPublicMailURL();
        Assert.assertEquals("<name> child of GetAccountInfoResponse", testAcct, acctName);
        List < Attr> attrs = getInfoResp.getA();
        len = attrs.size();
        Assert.assertTrue("number of <a> children of GetAccountInfoResponse should be at least 2",
                len >= 2);
        Assert.assertEquals("<cos> child of GetAccountInfoResponse 'name' attribute",
                "default", cos.getName());
        len = soapURL.size();
        Assert.assertEquals("number of <soapURL> children of GetAccountInfoResponse", 1, len);
        Assert.assertTrue(
                "value of <soapURL> child of GetAccountInfoResponse should start with 'http://'",
                soapURL.get(0).startsWith("http://"));
        Assert.assertTrue(
                "value of <adminSoapURL> child of GetAccountInfoResponse should start with 'https://'",
                adminSoapURL.startsWith("https://"));
        Assert.assertTrue(
                "value of <publicMailURL> child of GetAccountInfoResponse should start with 'http://'",
                publicMailURL.startsWith("http://"));
    }


    @Test
    public void getAccountByNameTest() throws Exception {
        String testAccountId = Utility.ensureAccountExists(testAcct);
        GetAccountRequest req = new GetAccountRequest();
        AccountSelector acct = new AccountSelector();
        acct.setBy(AccountBy.NAME);
        acct.setValue(testAcct);
        req.setAccount(acct);
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        GetAccountResponse resp = eif.getAccountRequest(req);
        Assert.assertNotNull("GetAccountResponse object", resp);
        AccountInfo acctInfo = resp.getAccount();
        Assert.assertNotNull("AccountInfo object", acctInfo);
        Assert.assertEquals("getAccountResponse <account> 'name' attribute",
                testAcct, acctInfo.getName());
        Assert.assertEquals("getAccountResponse <account> 'id' attribute",
                testAccountId, acctInfo.getId());
        int len = acctInfo.getA().size();
        Assert.assertTrue("<account> has " + len +
                " <a> children - should have at least 12", len >= 12);
    }

    @Test
    public void getAccountByIdTest() throws Exception {
        String testAccountId = Utility.ensureAccountExists(testAcct);
        GetAccountRequest getReq = new GetAccountRequest();
        AccountSelector accountSel = new AccountSelector();
        accountSel.setBy(AccountBy.ID);
        accountSel.setValue(testAccountId);
        getReq.setAccount(accountSel);
        GetAccountResponse getResp = eif.getAccountRequest(getReq);
        Assert.assertNotNull("GetAccountResponse object", getResp);
        AccountInfo accountInfo = getResp.getAccount();
        Assert.assertNotNull("AccountInfo object", accountInfo);
        Assert.assertEquals("getAccountResponse <account> 'name' attribute",
                testAcct, accountInfo.getName());
        String respId = accountInfo.getId();
        Assert.assertEquals("getAccountResponse <account> 'id' attribute",
                testAccountId, respId);
        int len = accountInfo.getA().size();
        Assert.assertTrue("GetAccountResponse <account> has " + len +
                " <a> children - should have at least 12", len >= 12);
    }

    @Test
    public void modifyAccountTest() throws Exception {
        int len;
        String testAccountId = Utility.ensureAccountExists(testAcct);
        String respId;
        ModifyAccountRequest modReq = new ModifyAccountRequest();
        modReq.setId(testAccountId);
        Attr modAttr = new Attr();
        modAttr.setN("displayName");
        modAttr.setValue("Modified Displayname");
        modReq.getA().add(modAttr);
        ModifyAccountResponse modResp = eif.modifyAccountRequest(modReq);
        Assert.assertNotNull("ModifyAccountResponse object", modResp);
        AccountInfo accountInfo = modResp.getAccount();
        Assert.assertNotNull("AccountInfo object", accountInfo);
        Assert.assertEquals("modifyAccountResponse <account> 'name' attribute", 
                testAcct, accountInfo.getName());
        respId = accountInfo.getId();
        Assert.assertEquals("modifyAccountResponse <account> 'id' attribute",
                testAccountId, respId);
        len = accountInfo.getA().size();
        Assert.assertTrue("modifyAccountResponse <account> has " + len +
                " <a> children - should have at least 50", len >= 50);
    }

    @Test
    public void renameAccountTest() throws Exception {
        int len;
        String testAccountId = Utility.ensureAccountExists(testAcct);
        String respId;
        RenameAccountRequest renameAccountReq = new RenameAccountRequest();
        renameAccountReq.setId(testAccountId);
        renameAccountReq.setNewName("foobar" + testAcct);
        RenameAccountResponse renameAccountResp = eif.renameAccountRequest(renameAccountReq);
        Assert.assertNotNull(renameAccountResp);
        AccountInfo accountInfo = renameAccountResp.getAccount();
        Assert.assertNotNull(accountInfo);
        Assert.assertEquals("renameAccountResponse <account> 'name' attribute",
                "foobar" + testAcct, accountInfo.getName());
        respId = accountInfo.getId();
        Assert.assertEquals("renameAccountResponse <account> 'id' attribute",
                testAccountId, respId);
        len = accountInfo.getA().size();
        Assert.assertTrue("renameAccountResponse <account> has " + len +
                " <a> children - should have at least 50", len >= 50);
        Utility.deleteAccountIfExists("foobar" + testAcct);
    }

    @Test
    public void getAccountMembershipTest() throws Exception {
        int len;
        String testAccountId = Utility.ensureAccountExists(testAcct);
        GetAccountMembershipRequest membershipReq = new GetAccountMembershipRequest();
        AccountSelector adminAcct = new AccountSelector();
        adminAcct.setBy(AccountBy.NAME);
        adminAcct.setValue("admin");
        membershipReq.setAccount(adminAcct);
        GetAccountMembershipResponse accountMembershipResponse =
            eif.getAccountMembershipRequest(membershipReq);
        Assert.assertNotNull("GetAccountMembershipResponse object", accountMembershipResponse);
        // TODO: test an account where the response actually has children
        len = accountMembershipResponse.getDl().size();
        Assert.assertEquals("GetAccountMembershipResponse object has " + len +
                " <dl> children - expecting 0", 0, len);

        // check that name did get changed.
        GetAccountRequest getReq = new GetAccountRequest();
        AccountSelector accountSel = new AccountSelector();
        accountSel.setBy(AccountBy.ID);
        accountSel.setValue(testAccountId);
        getReq.setAccount(accountSel);
        getReq.setAttrs("zimbraMailStatus,zimbraMailHost");
        GetAccountResponse getResp = eif.getAccountRequest(getReq);
        Assert.assertNotNull(getResp);
        AccountInfo accountInfo = getResp.getAccount();
        Assert.assertNotNull(accountInfo);
        Assert.assertEquals("getAccountResponse <account> 'name' attribute",
                testAcct, accountInfo.getName());
        String respId = accountInfo.getId();
        Assert.assertEquals("getAccountResponse <account> 'id' attribute",
                testAccountId, respId);
        len = accountInfo.getA().size();
        Assert.assertEquals("Number of GetAccountResponse <account> <a> children", 2, len);
    }

    @Test
    public void deleteAccountTest() throws Exception {
        String testAccountId = Utility.ensureAccountExists(testAcct);
        DeleteAccountRequest delReq = new DeleteAccountRequest();
        delReq.setId(testAccountId);
        DeleteAccountResponse delResp = eif.deleteAccountRequest(delReq);
        Assert.assertNotNull(delResp);
    }

    @Test
    public void getAllAccountsTest() throws Exception {
        GetAllAccountsRequest req = new GetAllAccountsRequest();
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        GetAllAccountsResponse resp = eif.getAllAccountsRequest(req);
        Assert.assertNotNull("GetAllAccountsResponse object", resp);
        List <AccountInfo> accountInfoList = resp.getAccount();
        int len;
        Assert.assertNotNull("GetAllAccountsResponse list of Accounts", accountInfoList);
        len = accountInfoList.size();
        Assert.assertTrue("Number of GetAllAccountsResponse <account> children is " +
                len + " - should be at least 2", len >= 2);
    }

    @Test
    public void getAllAdminAccountsTest() throws Exception {
        GetAllAdminAccountsRequest req = new GetAllAdminAccountsRequest();
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        GetAllAdminAccountsResponse resp = eif.getAllAdminAccountsRequest(req);
        Assert.assertNotNull("GetAllAdminAccountsResponse object", resp);
        List <AccountInfo> accountInfoList = resp.getAccount();
        int len;
        Assert.assertNotNull("GetAllAdminAccountsResponse list of Accounts", accountInfoList);
        len = accountInfoList.size();
        Assert.assertTrue("Number of GetAllAdminAccountsResponse <account> children is " + len
                + " - should be at least 1", len >= 1);
    }

    @Test
    public void countAccountTest() throws Exception {
        int len;
        String testDomainId = Utility.ensureDomainExists(testAcctDomain);
        String testAccountId = Utility.ensureMailboxExistsForAccount(testAcct);
        String testCosId = Utility.ensureCosExists(testCos);
        ModifyAccountRequest modReq = new ModifyAccountRequest();
        modReq.setId(testAccountId);
        Attr modAttr = new Attr();
        modAttr.setN("zimbraCOSId");
        modAttr.setValue(testCosId);
        modReq.getA().add(modAttr);
        ModifyAccountResponse modResp = eif.modifyAccountRequest(modReq);
        Assert.assertNotNull("ModifyAccountResponse object", modResp);
        AccountInfo accountInfo = modResp.getAccount();
        Assert.assertNotNull("AccountInfo object", accountInfo);
        Assert.assertEquals("modifyAccountResponse <account> 'name' attribute", 
                testAcct, accountInfo.getName());
        String respId = accountInfo.getId();
        Assert.assertEquals("modifyAccountResponse <account> 'id' attribute",
                testAccountId, respId);
        len = accountInfo.getA().size();
        Assert.assertTrue("modifyAccountResponse <account> has " + len +
                " <a> children - should have at least 50", len >= 50);
        CountAccountRequest req = new CountAccountRequest();
        DomainSelector domainSel = new DomainSelector();
        domainSel.setBy(DomainBy.ID);
        domainSel.setValue(testDomainId);
        req.setDomain(domainSel);
        CountAccountResponse resp = eif.countAccountRequest(req);
        Assert.assertNotNull(resp);
        List <CosCountInfo> cosList = resp.getCos();
        Assert.assertNotNull("cos list", cosList);
        len = cosList.size();
        Assert.assertTrue(len + "<cos> children present expect at least 1", len >= 1);
        CosCountInfo firstCos = cosList.get(0);
        Assert.assertTrue(
                "First <cos> id [" + firstCos.getId() + "] len should be longer than 10 chars",
                firstCos.getId().length() > 10);
        Assert.assertTrue(
                "First <cos> name [" + firstCos.getName() + "] len should be longer than 2 chars",
                firstCos.getName().length() > 2);
        long val = firstCos.getValue();
        Assert.assertTrue( "First <cos> value [" + val + "] should be >=0", val >= 0);
    }

    @Test
    public void getAllMailboxesTest() throws Exception {
        GetAllMailboxesRequest req = new GetAllMailboxesRequest();
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        GetAllMailboxesResponse resp = eif.getAllMailboxesRequest(req);
        Assert.assertNotNull("GetAllMailboxesResponse object", resp);
        List <MailboxInfo> mboxInfoList = resp.getMbox();
        int len;
        Assert.assertNotNull("GetAllMailboxesResponse list of Mailboxes", mboxInfoList);
        len = mboxInfoList.size();
        Assert.assertTrue("Number of GetAllMailboxesResponse <mbox> children is " +
                len + " - should be at least 2", len >= 2);
    }

    @Test
    public void recalculateMailboxCountsTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        String accountId = Utility.ensureMailboxExistsForAccount(testAcct);
        RecalculateMailboxCountsRequest req = new RecalculateMailboxCountsRequest();
        MailboxByAccountIdSelector sel = new MailboxByAccountIdSelector();
        sel.setId(accountId);
        req.setMbox(sel);
        RecalculateMailboxCountsResponse resp = eif.recalculateMailboxCountsRequest(req);
        Assert.assertNotNull("RecalculateMailboxCountsResponse object", resp);
        MailboxQuotaInfo quotaInfo = resp.getMbox();
        Assert.assertEquals("<mbox> 'id' attribute", accountId, quotaInfo.getId());
        Assert.assertTrue("RecalculateMailboxCountsResponse <mbox> 'quotaUsed' attribute=" +
                quotaInfo.getUsed() + " should be 0 or more", quotaInfo.getUsed() >= 0);
    }

    @Test
    public void getQuotaUsageTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        Utility.ensureMailboxExistsForAccount(testAcct);
        GetQuotaUsageRequest req = new GetQuotaUsageRequest();
        req.setDomain(testAcctDomain);
        req.setLimit(125);
        req.setOffset(0);
        req.setSortAscending(true);
        req.setSortBy("totalUsed");
        GetQuotaUsageResponse resp = eif.getQuotaUsageRequest(req);
        Assert.assertNotNull("GetQuotaUsageResponse object", resp);
        int total = resp.getSearchTotal();
        Assert.assertTrue("searchTotal=" + total + " should be 1 or more",
                total >= 1);
        Assert.assertFalse("more", resp.isMore());
        List <AccountQuotaInfo> acctQuotas = resp.getAccount();
        Assert.assertNotNull("list of accounts object", acctQuotas);
        Assert.assertEquals("Number of account objects",
                total, acctQuotas.size());
        AccountQuotaInfo first = acctQuotas.get(0);
        Assert.assertNotNull("1st account's id", first.getId());
        Assert.assertNotNull("1st account's name", first.getName());
        Assert.assertTrue("1st account's used=" + first.getUsed() +
                " should be 0 or more", first.getUsed() >= 0);
        Assert.assertTrue("1st account's limit=" + first.getLimit() +
                " should be 0 or more", first.getLimit() >= 0);
    }

    private void addAcctLogger(String acctId, String category, String level)
    throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        AddAccountLoggerRequest req = new AddAccountLoggerRequest();
        AccountSelector acct = new AccountSelector();
        acct.setBy(AccountBy.ID);
        acct.setValue(acctId);
        req.setAccount(acct);
        LoggerInfo newLogger = new LoggerInfo();
        newLogger.setCategory(category);
        newLogger.setLevel(level);
        req.setLogger(newLogger);
        AddAccountLoggerResponse resp = eif.addAccountLoggerRequest(req);
        Assert.assertNotNull("AddAccountLoggerResponse object", resp);
        List <LoggerInfo> logger = resp.getLogger();
        int total = logger.size();
        Assert.assertTrue("number of account logger=" + total +
                " should be 1 or more", total >= 1);
        LoggerInfo loggerInfo = logger.get(0);
        Assert.assertNotNull("1st accountLogger's logger", loggerInfo);
        Assert.assertNotNull("category", loggerInfo.getCategory());
        Assert.assertNotNull("level", loggerInfo.getLevel());
    }

    private void removeAcctLogger(String acctId, String category)
    throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        RemoveAccountLoggerRequest req = new RemoveAccountLoggerRequest();
        if (acctId != null) {
            AccountSelector acct = new AccountSelector();
            acct.setBy(AccountBy.ID);
            acct.setValue(acctId);
            req.setAccount(acct);
        }
        if (category != null) {
            LoggerInfo newLogger = new LoggerInfo();
            newLogger.setCategory(category);
            req.setLogger(newLogger);
        }
        RemoveAccountLoggerResponse resp = eif.removeAccountLoggerRequest(req);
        Assert.assertNotNull("RemoveAccountLoggerResponse object", resp);
    }

    @Test
    public void addAccountLoggerTest() throws Exception {
        String testAccountId = Utility.ensureMailboxExistsForAccount(testAcct);
        addAcctLogger(testAccountId, "zimbra.account", "info");
        addAcctLogger(testAccountId, "zimbra.lmtp", "info");
    }

    @Test
    public void getAllAccountLoggersTest() throws Exception {
        String testAccountId = Utility.ensureMailboxExistsForAccount(testAcct);
        addAcctLogger(testAccountId, "zimbra.ldap", "error");
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        GetAllAccountLoggersRequest req = new GetAllAccountLoggersRequest();
        GetAllAccountLoggersResponse resp = eif.getAllAccountLoggersRequest(req);
        Assert.assertNotNull("GetAllAccountLoggersResponse object", resp);
        List <AccountLoggerInfo> acctLoggers = resp.getAccountLogger();
        int total = acctLoggers.size();
        Assert.assertTrue("number of account loggers=" + total +
                " should be 1 or more", total >= 1);
        AccountLoggerInfo first = acctLoggers.get(0);
        Assert.assertNotNull("1st accountLogger's id", first.getId());
        Assert.assertNotNull("1st accountLogger's name", first.getName());
        List <LoggerInfo> loggers = first.getLogger();
        Assert.assertNotNull("1st accountLogger's loggers", loggers);
        Assert.assertNotNull("category", loggers.get(0).getCategory());
        Assert.assertNotNull("level", loggers.get(0).getLevel());
    }

    @Test
    public void getAccountLoggersTest() throws Exception {
        String testAccountId = Utility.ensureMailboxExistsForAccount(testAcct);
        addAcctLogger(testAccountId, "zimbra.xsync", "debug");
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        GetAccountLoggersRequest req = new GetAccountLoggersRequest();
        AccountSelector acct = new AccountSelector();
        acct.setBy(AccountBy.ID);
        acct.setValue(testAccountId);
        req.setAccount(acct);
        GetAccountLoggersResponse resp = eif.getAccountLoggersRequest(req);
        Assert.assertNotNull("GetAccountLoggersResponse object", resp);
        List <LoggerInfo> loggers = resp.getLogger();
        int total = loggers.size();
        Assert.assertTrue("number of account loggers=" + total +
                " should be 1 or more", total >= 1);
        LoggerInfo loggerInfo = loggers.get(0);
        Assert.assertNotNull("1st accountLogger's logger", loggerInfo);
        Assert.assertNotNull("category", loggerInfo.getCategory());
        Assert.assertNotNull("level", loggerInfo.getLevel());
    }

    @Test
    public void removeAccountLoggerTest() throws Exception {
        String testAccountId = Utility.ensureMailboxExistsForAccount(testAcct);
        addAcctLogger(testAccountId, "zimbra.misc", "info");
        addAcctLogger(testAccountId, "zimbra.im", "info");
        removeAcctLogger(testAccountId, "zimbra.im");
        removeAcctLogger(testAccountId, null);
        // Adding an AccountLogger with a null account is disallowed,
        // so, how would such a logger get created?  No exception thrown
        // for this though and ok response received... 
        removeAcctLogger(null, "zimbra.tnef");
    }

    @Test
    public void getAccountLoggersByIdTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        String testAccountId = Utility.ensureMailboxExistsForAccount(testAcct);
        addAcctLogger(testAccountId, "zimbra.misc", "debug");
        GetAccountLoggersRequest req = new GetAccountLoggersRequest();
        req.setId(testAccountId);
        GetAccountLoggersResponse resp = eif.getAccountLoggersRequest(req);
        Assert.assertNotNull("GetAccountLoggersResponse object", resp);
        List <LoggerInfo> loggers = resp.getLogger();
        int total = loggers.size();
        Assert.assertTrue("number of account loggers=" + total +
                " should be 1 or more", total >= 1);
        LoggerInfo loggerInfo = loggers.get(0);
        Assert.assertNotNull("1st accountLogger's logger", loggerInfo);
        Assert.assertNotNull("category", loggerInfo.getCategory());
        Assert.assertNotNull("level", loggerInfo.getLevel());
    }

    @Test
    public void reindexMailboxTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        String accountId = Utility.ensureMailboxExistsForAccount(testAcct);
        ReIndexRequest req = new ReIndexRequest();
        ReindexMailboxInfo sel = new ReindexMailboxInfo();
        sel.setId(accountId);
        req.setMbox(sel);
        req.setAction("start");
        ReIndexResponse resp = eif.reIndexRequest(req);
        Assert.assertNotNull("ReIndexResponse object", resp);
        ReindexProgressInfo progress = resp.getProgress();
        Assert.assertNull("ReIndexResponse progress object for start", progress);
        req.setAction("status");
        resp = eif.reIndexRequest(req);
        Assert.assertNotNull("ReIndexResponse object", resp);
        progress = resp.getProgress();
        // if status is idle, won't get a progress sub-element
        if (progress != null) {
            int numRemaining = progress.getNumRemaining();
            int numSucceeded = progress.getNumSucceeded();
            int numFailed = progress.getNumFailed();
            Assert.assertTrue("NumRemaining=" + numRemaining
                    + " should be -1 or more", numRemaining >= -1);
            Assert.assertTrue("NumSucceeded=" + numSucceeded
                    + " should be 0 or more", numSucceeded >= 0);
            Assert.assertTrue("NumFailed=" + numFailed
                    + " should be 0 or more", numFailed >= 0);
        }
    }

    @Test
    public void verifyIndexTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        String accountId = Utility.ensureMailboxExistsForAccount(testAcct);
        VerifyIndexRequest req = new VerifyIndexRequest();
        MailboxByAccountIdSelector sel = new MailboxByAccountIdSelector();
        sel.setId(accountId);
        req.setMbox(sel);
        VerifyIndexResponse resp = eif.verifyIndexRequest(req);
        Assert.assertNotNull("VerifyIndexResponse object", resp);
        resp.getMessage();
        resp.isStatus();
    }

    @Test
    public void purgeMessagesTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        String accountId = Utility.ensureMailboxExistsForAccount(testAcct);
        PurgeMessagesRequest req = new PurgeMessagesRequest();
        MailboxByAccountIdSelector sel = new MailboxByAccountIdSelector();
        sel.setId(accountId);
        req.setMbox(sel);
        PurgeMessagesResponse resp = eif.purgeMessagesRequest(req);
        Assert.assertNotNull("PurgeMessagesResponse object", resp);
        List <MailboxWithMailboxId> mboxids = resp.getMbox();
        Assert.assertNotNull("List of <mbox> elements", mboxids);
        Assert.assertEquals("Number of <mbox> elements", 1, mboxids.size());
        long mboxid = mboxids.get(0).getMbxid();
        Assert.assertTrue("mboxid = " + mboxid + " should be >0", mboxid > 0);
    }

    @Test
    public void getMailboxTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        String accountId = Utility.ensureMailboxExistsForAccount(testAcct);
        GetMailboxRequest req = new GetMailboxRequest();
        MailboxByAccountIdSelector sel = new MailboxByAccountIdSelector();
        sel.setId(accountId);
        req.setMbox(sel);
        GetMailboxResponse resp = eif.getMailboxRequest(req);
        Assert.assertNotNull("GetMailboxResponse object", resp);
        MailboxWithMailboxId mboxid = resp.getMbox();
        Assert.assertNotNull("Object for <mbox> element", mboxid);
        Assert.assertTrue("mboxid = " + mboxid + " should be >0", mboxid.getMbxid() > 0);
    }

    @Test
    public void deleteMailboxTest() throws Exception {
        Utility.addSoapAdminAuthHeader((WSBindingProvider)eif);
        String accountId = Utility.ensureMailboxExistsForAccount(testAcct);
        DeleteMailboxRequest req = new DeleteMailboxRequest();
        MailboxByAccountIdSelector sel = new MailboxByAccountIdSelector();
        sel.setId(accountId);
        req.setMbox(sel);
        DeleteMailboxResponse resp = eif.deleteMailboxRequest(req);
        Assert.assertNotNull("DeleteMailboxResponse object", resp);
        MailboxWithMailboxId mboxid = resp.getMbox();
        Assert.assertNotNull("Object for <mbox> element", mboxid);
        Assert.assertTrue("mboxid = " + mboxid + " should be >0", mboxid.getMbxid() > 0);
    }
}
