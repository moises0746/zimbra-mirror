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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetPrefs extends AccountDocumentHandler  {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        Element response = zsc.createElement(AccountConstants.GET_PREFS_RESPONSE);
        handle(request, response, account);
        return response;
    }

    /**
     * Pass in a request that optional has &lt;pref&gt; items as a filter, and
     * fills in the response document with gathered prefs.
     * 
     * @param request 
     * @param acct
     * @param response
     * @throws ServiceException
     */
    public static void handle(Element request, Element response, Account acct) throws ServiceException {
        HashSet<String> specificPrefs = null;
        for (Element epref : request.listElements(AccountConstants.E_PREF)) {
            if (specificPrefs == null)
                specificPrefs = new HashSet<String>();
            specificPrefs.add(epref.getAttribute(AccountConstants.A_NAME));
        }

        Map<String, Object> map = acct.getAttrs();
        if (map != null) {
            Locale locale = Provisioning.getInstance().getLocale(acct);
            doPrefs(acct, locale.toString(), response, map, specificPrefs);
        }
    }
    
    public static void doPrefs(Account acct, String locale, Element prefs, Map<String, Object> attrsMap, HashSet<String> specificPrefs) throws ServiceException {
        boolean needLocale = (specificPrefs == null || specificPrefs.contains(Provisioning.A_zimbraPrefLocale));
        if (needLocale)
            prefs.addKeyValuePair(Provisioning.A_zimbraPrefLocale, locale, AccountConstants.E_PREF, AccountConstants.A_NAME);

        AttributeManager attrMgr = AttributeManager.getInstance();
        for (Map.Entry<String, Object> entry : attrsMap.entrySet()) {
            String key = entry.getKey();

            if (specificPrefs != null && !specificPrefs.contains(key))
                continue;
            if (!key.startsWith("zimbraPref") || key.equals(Provisioning.A_zimbraPrefLocale))
                continue;

            boolean isIDN = attrMgr.isEmailOrIDN(key);
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++)
                    ToXML.encodeAttr(prefs, key, sa[i], AccountConstants.E_PREF, AccountConstants.A_NAME, isIDN);
            } else {
                ToXML.encodeAttr(prefs, key, (String) value, AccountConstants.E_PREF, AccountConstants.A_NAME, isIDN);
            }
        }
    }   

}
