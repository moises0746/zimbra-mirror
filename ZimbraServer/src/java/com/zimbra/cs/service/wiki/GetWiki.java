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

package com.zimbra.cs.service.wiki;

import java.io.IOException;
import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.soap.ZimbraSoapContext;

public class GetWiki extends WikiDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		checkEnabled(zsc);
		Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element wElem = request.getElement(MailConstants.E_WIKIWORD);
        String word = wElem.getAttribute(MailConstants.A_NAME, null);
        String id = wElem.getAttribute(MailConstants.A_ID, null);
        int traverse = (int)wElem.getAttributeLong(MailConstants.A_TRAVERSE, 0);
        int version = (int) wElem.getAttributeLong(MailConstants.A_VERSION, -1);
        int count = (int) wElem.getAttributeLong(MailConstants.A_COUNT, -1);

        Element response = zsc.createElement(MailConstants.GET_WIKI_RESPONSE);

        WikiItem wikiItem;

        if (word != null) {
        	ItemId fid = getRequestedFolder(request, zsc);
        	if (fid == null)
        		fid = new ItemId("", Mailbox.ID_FOLDER_USER_ROOT);
        	WikiContext wctxt = new WikiContext(octxt, zsc.getRawAuthToken());
        	WikiPage wikiPage = Wiki.findWikiPageByPath(wctxt, zsc.getRequestedAccountId(), fid.getId(), word, traverse == 1);
        	try {
        		Document doc = wikiPage.getWikiItem(wctxt);
            	if (doc.getType() != MailItem.TYPE_WIKI) {
            		throw WikiServiceException.NOT_WIKI_ITEM(word);
            	}
            	wikiItem = (WikiItem) doc;
        	} catch (Exception e) {
        		if (wikiPage == null) {
        			throw new WikiServiceException.NoSuchWikiException(word);
        		}
        		Element wikiElem = ToXML.encodeWikiPage(response, wikiPage);
        		String contents = wikiPage.getContents(wctxt);
        		if (contents != null && contents != "") {
        			wikiElem.addAttribute(MailConstants.A_BODY, contents, Element.Disposition.CONTENT);
        		}
        		return response;
        	}
        } else if (id != null) {
        	ItemId iid = new ItemId(id, zsc);
        	wikiItem = mbox.getWikiById(octxt, iid.getId());
        } else {
        	throw ServiceException.FAILURE("missing attribute w or id", null);
        }

        Element wikiElem = ToXML.encodeWiki(response, ifmt, octxt, wikiItem, version);
        
        if (count > 1) {
    		count--;  // head version was already printed
        	if (version <= 0) {
        		version = wikiItem.getVersion();
        	}
        	while (--version > 0) {
                ToXML.encodeWiki(response, ifmt, octxt, wikiItem, version);
        		count--;
        		if (count == 0) {
        			break;
        		}
        	}
        } else {
        	Document revision = (version > 0 ? (Document) mbox.getItemRevision(octxt, wikiItem.getId(), wikiItem.getType(), version) : wikiItem); 
        	try {
        		// when the revisions get pruned after each save, the contents of
        		// old revision is gone, and revision.getContent() returns null.
        		if (revision != null) {
        			byte[] raw = revision.getContent();
        			wikiElem.addAttribute(MailConstants.A_BODY, new String(raw, "UTF-8"), Element.Disposition.CONTENT);
        		}
        	} catch (IOException ioe) {
        		ZimbraLog.wiki.error("cannot read the wiki message body", ioe);
        		throw WikiServiceException.CANNOT_READ(wikiItem.getWikiWord());
        	}
        }
        return response;
	}
}
