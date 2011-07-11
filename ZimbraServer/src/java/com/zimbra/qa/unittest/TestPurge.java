/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import java.util.Arrays;

import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.mail.message.FolderActionRequest;
import com.zimbra.soap.mail.message.FolderActionResponse;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;

import junit.framework.TestCase;

public class TestPurge extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestPurge.class.getSimpleName();

    @Override
    public void setUp() throws Exception {
        cleanUp();
    }

    /**
     * Tests the SOAP API for setting retention policy on a folder.
     */
    public void testFolderRetentionPolicy()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(mbox, "/" + NAME_PREFIX + "-testFolderRetentionPolicy");
        
        // Set user keep policy for folder. 
        FolderActionSelector action = new FolderActionSelector(folder.getId(), "retentionpolicy");
        RetentionPolicy rp = new RetentionPolicy();
        rp.setKeepPolicy(Arrays.asList(Policy.newUserPolicy("30d")));
        action.setRetentionPolicy(rp);
        FolderActionRequest req = new FolderActionRequest(action);
        
        FolderActionResponse res = mbox.invokeJaxb(req);
        assertEquals("retentionpolicy", res.getAction().getOperation());
        assertEquals(folder.getId(), res.getAction().getId());
        
        // Make sure that the retention policy is now set.
        folder = mbox.getFolderById(folder.getId());
        rp = folder.getRetentionPolicy();
        assertEquals(1, rp.getKeepPolicy().size());
        assertEquals(0, rp.getPurgePolicy().size());
        Policy p = rp.getKeepPolicy().get(0);
        assertEquals(Policy.Type.USER, p.getType());
        assertEquals("30d", p.getLifetime());
        
        // Turn off keep policy and set purge policy.
        action = new FolderActionSelector(folder.getId(), "retentionpolicy");
        rp = new RetentionPolicy();
        rp.setPurgePolicy(Arrays.asList(Policy.newUserPolicy("45d")));
        action.setRetentionPolicy(rp);
        req = new FolderActionRequest(action);
        
        res = mbox.invokeJaxb(req);
        assertEquals("retentionpolicy", res.getAction().getOperation());
        assertEquals(folder.getId(), res.getAction().getId());
        
        // Make sure that the retention policy is now set.
        folder = mbox.getFolderById(folder.getId());
        rp = folder.getRetentionPolicy();
        assertEquals(0, rp.getKeepPolicy().size());
        assertEquals(1, rp.getPurgePolicy().size());
        p = rp.getPurgePolicy().get(0);
        assertEquals(Policy.Type.USER, p.getType());
        assertEquals("45d", p.getLifetime());
        
        // Start a new session and make sure that the retention policy is still returned.
        mbox = TestUtil.getZMailbox(USER_NAME);
        folder = mbox.getFolderById(folder.getId());
        assertNotNull(folder.getRetentionPolicy());
    }
    
    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestPurge.class);
    }
}
