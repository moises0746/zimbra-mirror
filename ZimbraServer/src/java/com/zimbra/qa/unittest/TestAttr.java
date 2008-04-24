/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProvisioning;


public class TestAttr extends TestCase {
    private String TEST_ID = TestProvisioningUtil.genTestId();;
    private static String TEST_NAME = "test-attr";
    private static String PASSWORD = "test123";

    private String DOMAIN_NAME;
    private String ACCT_EMAIL;
    private String SERVER_NAME;
    
    private Provisioning mProv;
    
    public void setUp() throws Exception {
        DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
        ACCT_EMAIL = "user1" + "@" + DOMAIN_NAME;
        SERVER_NAME = "server-" + TEST_ID;
        
        mProv = Provisioning.getInstance();        
    }
    
    private Account createAccount() throws Exception {
        Domain domain = getDomain();
        
        Account acct = mProv.createAccount(ACCT_EMAIL, PASSWORD, new HashMap<String, Object>());
        assertNotNull(acct);
        return acct;
    }
    
    private Domain createDomain() throws Exception{
        Domain domain = mProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
        return domain;
    }
    
    private Server createServer() throws Exception {
        Server server = mProv.createServer(SERVER_NAME, new HashMap<String, Object>());
        assertNotNull(server);
        return server;
    }
    
    private Account getAccount() throws Exception {
        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        if (acct == null)
            acct = createAccount();
        assertNotNull(acct);
        return acct;        
    }
    
    private Domain getDomain() throws Exception {
        Domain domain = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME);
        if (domain == null)
            domain = createDomain();
        assertNotNull(domain);
        return domain;        
    }
    
    private Server getServer() throws Exception {
        Server server = mProv.get(Provisioning.ServerBy.name, SERVER_NAME);
        if (server == null)
            server = createServer();
        assertNotNull(server);
        return server;        
    }
    
    private void cannotUnset(Entry entry, String attrName) {
        boolean good = false;
        
        try {
            unsetByEmptyString(entry, attrName);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode()) && 
                e.getMessage().contains("is a required attribute"))
                good = true;
        }
        assertTrue(good);        
        
        try {
            unsetByNull(entry, attrName);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode()) && 
                e.getMessage().contains("is a required attribute"))
                good = true;
        }
        assertTrue(good); 
    }
    
    private void unsetByEmptyString(Entry entry, String attrName) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, "");
        mProv.modifyAttrs(entry, attrs);
        String newValue = entry.getAttr(attrName, false);
        assertNull(newValue);
    }
    
    private void unsetByNull(Entry entry, String attrName) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, null);
        mProv.modifyAttrs(entry, attrs);
        String newValue = entry.getAttr(attrName, false);
        assertNull(newValue);
    }
        
    private void unsetTest(Entry entry, String attrName) throws ServiceException {
        unsetByEmptyString(entry, attrName);
        unsetByNull(entry, attrName);
    }
    
    private void setAttr(Entry entry, String attrName, String value) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, value);
        mProv.modifyAttrs(entry, attrs);
        String newValue = entry.getAttr(attrName, false);
        assertEquals(value, newValue);
    }
    
    public void testSingleValuedAttr() throws Exception {
        Account acct = getAccount();
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        String attrName = Provisioning.A_zimbraInterceptAddress;
        String V = "test@example.com";
        
        String value = acct.getAttr(attrName);
        assertNull(value);
        
        // set a value
        attrs.clear();
        attrs.put(attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertEquals(V, value);
        
        // delete the value by passing ""
        unsetByEmptyString(acct, attrName);
        
        // set a value
        attrs.clear();
        attrs.put(attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertEquals(V, value);
        
        // delete the value by passing null
        unsetByNull(acct, attrName);
        
        // set a value
        attrs.clear();
        attrs.put(attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertEquals(V, value);
        
        // delete the value by passing -{attr anme}
        attrs.clear();
        attrs.put("-" + attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertNull(value);
    }
    
    public void testMultiValuedAttr() throws Exception {
        Account acct = getAccount();
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        String attrName = Provisioning.A_zimbraMailForwardingAddress;
        String V1 = "test-1@example.com";
        String V2 = "test-2@example.com";
        
        String value;
        Set<String> values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 0);
      
        // set a value
        attrs.clear();
        attrs.put(attrName, V1);
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 1 && values.contains(V1));
        
        // add a value
        attrs.clear();
        attrs.put("+" + attrName, V2);
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 2 && values.contains(V1) && values.contains(V2));

        // removing a value
        attrs.clear();
        attrs.put("-" + attrName, V1);
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 1 && values.contains(V2));
        
        // delete all values by passing ""
        attrs.clear();
        attrs.put(attrName, "");
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertNull(value);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 0);
        
        // set 2 values
        attrs.clear();
        attrs.put(attrName, new String[] {V1, V2});
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 2 && values.contains(V1) && values.contains(V2));
        
        // delete all values by passing null
        attrs.clear();
        attrs.put(attrName, null);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertNull(value);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 0);
    }
    
    public void testCallbackAccountStatus() throws Exception {
        Account acct = getAccount();
        String attrName = Provisioning.A_zimbraAccountStatus;
        
        String value = acct.getAttr(attrName);
        assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE, value);
        
        cannotUnset(acct, attrName);       
        
        setAttr(acct, attrName, Provisioning.ACCOUNT_STATUS_CLOSED);
        assertEquals(acct.getAttr(Provisioning.A_zimbraMailStatus), Provisioning.MAIL_STATUS_DISABLED);
    }
    
    public void testCallbackCheckPortConflict() throws Exception {
        Server server = getServer();    
        String attrName = Provisioning.A_zimbraLmtpBindPort;
        
        unsetTest(server, attrName);
    }
    
    public void testCallbackDataSource() throws Exception {
        Account acct = getAccount();        
        String attrName = Provisioning.A_zimbraDataSourcePollingInterval;
        
        unsetTest(acct, attrName);
    }
    
    public void testCallbackDisplayName() throws Exception {
        Account acct = getAccount();
        String attrName = Provisioning.A_displayName;
        
        unsetTest(acct, attrName);
    }
    
    public void testCallbackDomainStatus() throws Exception {
        Domain domain = getDomain();
        String attrName = Provisioning.A_zimbraDomainStatus;
        
        // TODO
        // unsetTest(domain, attrName);
    }
 
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        try {
            TestUtil.runTest(new TestSuite(TestAttr.class));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

