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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.taglib.tag.contact;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.taglib.bean.ZContactBean;
import com.zimbra.cs.taglib.tag.ZimbraSimpleTag;
import com.zimbra.cs.zclient.ZContact;
import com.zimbra.cs.zclient.ZMailbox;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.JspTagException;
import java.io.IOException;
import java.util.List;

public class GetContactTag extends ZimbraSimpleTag {
    
    private String mVar;
    private String mId;
    private boolean mSync;
    
    public void setVar(String var) { this.mVar = var; }
    public void setId(String id) { this.mId = id; }    
    public void setSync(boolean sync) { this.mSync = sync; }

    public void doTag() throws JspException, IOException {
        JspContext jctxt = getJspContext();
        try {
            ZMailbox mbox = getMailbox();
            List<ZContact> contacts = mbox.getContacts(mId, null, mSync, null);
            if (contacts.size() == 1) {
                ZContact c = contacts.get(0);
                jctxt.setAttribute(mVar, new ZContactBean(c),  PageContext.PAGE_SCOPE);
            }
        } catch (ServiceException e) {
            throw new JspTagException(e);
        }
    }
}
