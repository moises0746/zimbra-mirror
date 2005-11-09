/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

import com.zimbra.cs.util.BuildInfo;

public class GetVersionInfo extends AdminDocumentHandler 
{

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);

        Element response = lc.createElement(AdminService.GET_VERSION_INFO_RESPONSE);
        Element infoEl = response.addElement(AdminService.A_VERSION_INFO_INFO);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_VERSION, BuildInfo.VERSION);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_RELEASE, BuildInfo.RELEASE);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_DATE, BuildInfo.DATE);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_HOST, BuildInfo.HOST);
        return response;
    }

    public boolean needsAdminAuth(Map context) {
        return false;
    }

    public boolean needsAuth(Map context) {
        return false;
    }

}
