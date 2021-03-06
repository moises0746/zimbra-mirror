/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013 Zimbra Software, LLC.
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
package com.zimbra.cs.session;

import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.SoapSessionFactory;
import com.zimbra.soap.ZimbraSoapContext;

public class OfflineSoapSessionFactory extends SoapSessionFactory {
    @Override
    public SoapSession getSoapSession(ZimbraSoapContext zsc) throws ServiceException {
        if (zsc.isAuthUserOnLocalhost()) {
            return new OfflineSoapSession(zsc);
        } else {
            return new OfflineRemoteSoapSession(zsc);
        }
    }
}
