/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.util.Arrays;
import java.util.Collections;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.lmtp.SmtpToLmtp;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;

/**
 * Validates recipients and expands distribution lists for the dev
 * SMTP server.
 */
public class SmtpRecipientValidator
implements SmtpToLmtp.RecipientValidator {

    private static final Log log = LogFactory.getLog(SmtpRecipientValidator.class);

    @Override
    public Iterable<String> validate(String recipient) {
        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = prov.get(AccountBy.name, recipient);
            if (account != null) {
                return Arrays.asList(account.getName());
            } else {
                Group group = prov.getGroup(Key.DistributionListBy.name, recipient);
                if (group != null) {
                    String[] members;
                    if (group instanceof DynamicGroup) {
                        members = ((DynamicGroup)group).getAllMembers(true);
                    } else {
                        members = group.getAllMembers();
                    }
                    return Arrays.asList(members);
                }
            }
        } catch (ServiceException e) {
            log.error("Unable to validate recipient %s", recipient, e);
        }
        return Collections.emptyList();
    }
}
