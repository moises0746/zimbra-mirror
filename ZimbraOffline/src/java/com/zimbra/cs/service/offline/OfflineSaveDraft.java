/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.offline;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.service.mail.SaveDraft;

public class OfflineSaveDraft extends SaveDraft {
    @Override
    public void preProxy(Element request, Map<String, Object> context) throws ServiceException {        
        OfflineProxyHelper.uploadAttachments(request, getZimbraSoapContext(context).getRequestedAccountId());
    }

    @Override
    protected boolean scheduleAutoSendTask() {
        return false;
    }
}

