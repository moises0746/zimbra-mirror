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
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.service.ServiceException;

public class Collection extends MailItemResource {

	protected int mId;
	protected int mMboxId;
	protected byte mView;
	protected byte mType;
	
	public Collection(Folder f) throws DavException, ServiceException {
		super(f);
		setCreationDate(f.getDate());
		setLastModifiedDate(f.getChangeDate());
		setProperty(DavElements.P_DISPLAYNAME, f.getName());
		setProperty(DavElements.P_GETCONTENTLENGTH, "0");
		mId = f.getId();
		mMboxId = f.getMailboxId();
		mView = f.getDefaultView();
		mType = f.getType();
		addProperties(Acl.getAclProperties(this, f));
	}
	
	private Collection(String name, String acct) throws DavException {
		super(name, acct);
		long now = System.currentTimeMillis();
		setCreationDate(now);
		setLastModifiedDate(now);
		setProperty(DavElements.P_DISPLAYNAME, name.substring(1));
		setProperty(DavElements.P_GETCONTENTLENGTH, "0");
		try {
			addProperties(Acl.getAclProperties(this, null));
		} catch (ServiceException se) {
		}
	}
	
	@Override
	public InputStream getContent() throws IOException, DavException {
		return null;
	}

	@Override
	public boolean isCollection() {
		return true;
	}
	
	@Override
	public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		ArrayList<DavResource> children = new ArrayList<DavResource>();

		try {
			List<MailItem> items = getChildrenMailItem(ctxt);
			for (MailItem item : items) {
				DavResource rs = UrlNamespace.getResourceFromMailItem(ctxt, item);
				if (rs != null)
					children.add(rs);
			}
		} catch (ServiceException e) {
			ZimbraLog.dav.error("can't get children from folder: id="+mId, e);
		}
		// this is where we add the phantom folder for attachment browser.
		if (mId == Mailbox.ID_FOLDER_USER_ROOT) {
			children.add(new Collection(UrlNamespace.ATTACHMENTS_PREFIX, getOwner()));
		}
		return children;
	}
	
	private List<MailItem> getChildrenMailItem(DavContext ctxt) throws ServiceException {
		Mailbox mbox = getMailbox();

		List<MailItem> ret = new ArrayList<MailItem>();
		
		// XXX aggregate into single call
		for (MailItem f : mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_FOLDER, mId)) {
			byte view = ((Folder)f).getDefaultView();
			if (view == MailItem.TYPE_DOCUMENT || view == MailItem.TYPE_WIKI)
				ret.add(f);
		}
		//ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_MOUNTPOINT, mId));
		ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_DOCUMENT, mId));
		ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_WIKI, mId));
		return ret;
	}
	
	protected Mailbox getMailbox() throws ServiceException {
		return MailboxManager.getInstance().getMailboxById(mMboxId);
	}
	
	public void mkCol(DavContext ctxt, String name) throws DavException {
		mkCol(ctxt, name, mView);
	}
	
	public void mkCol(DavContext ctxt, String name, byte view) throws DavException {
		try {
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(AccountBy.name, ctxt.getUser());
			if (account == null)
				throw new DavException("no such account "+ctxt.getUser(), HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			mbox.createFolder(ctxt.getOperationContext(), name, mId, view, 0, (byte)0, null);
		} catch (ServiceException e) {
			if (e.getCode().equals(MailServiceException.ALREADY_EXISTS))
				throw new DavException("item already exists", HttpServletResponse.SC_CONFLICT, e);
			else if (e.getCode().equals(MailServiceException.PERM_DENIED))
				throw new DavException("permission denied", HttpServletResponse.SC_FORBIDDEN, e);
			else
				throw new DavException("can't create", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}
	
	// create Document at the URI
	public DavResource createItem(DavContext ctxt, String user, String name) throws DavException, IOException {
		Provisioning prov = Provisioning.getInstance();
		Mailbox mbox = null;
		try {
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			mbox = MailboxManager.getInstance().getMailboxByAccount(account);
		} catch (ServiceException e) {
			throw new DavException("cannot get mailbox", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}

		String author = ctxt.getAuthAccount().getName();
		String ctype = ctxt.getRequest().getContentType();
		int clen = ctxt.getRequest().getContentLength();
		byte[] data = ByteUtil.getContent(ctxt.getRequest().getInputStream(), clen);
		if (ctype == null)
			ctype = URLConnection.getFileNameMap().getContentTypeFor(name);
		if (ctype == null)
			ctype = DavProtocol.DEFAULT_CONTENT_TYPE;
		try {
			// add a revision if the resource already exists
			MailItem item = mbox.getItemByPath(ctxt.getOperationContext(), ctxt.getPath(), 0, false);
			if (item.getType() != MailItem.TYPE_DOCUMENT && item.getType() != MailItem.TYPE_WIKI)
				throw new DavException("no DAV resource for "+MailItem.getNameForType(item.getType()), HttpServletResponse.SC_NOT_ACCEPTABLE, null);
			Document doc = mbox.addDocumentRevision(ctxt.getOperationContext(), (Document)item, data, author);
			return new Notebook(ctxt, doc);
		} catch (ServiceException e) {
			if (!(e instanceof NoSuchItemException))
				throw new DavException("cannot get item ", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}
		
		// create
		try {
			Document doc = mbox.createDocument(ctxt.getOperationContext(), mId, name, ctype, author, data, null);
			return new Notebook(ctxt, doc);
		} catch (ServiceException se) {
			throw new DavException("cannot create ", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
		}
	}
	
	public void delete(DavContext ctxt) throws DavException {
		String user = ctxt.getUser();
		String path = ctxt.getPath();
		if (user == null || path == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		try {
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			mbox.delete(ctxt.getOperationContext(), mId, mType);
		} catch (ServiceException e) {
			throw new DavException("cannot get item", HttpServletResponse.SC_NOT_FOUND, e);
		}
	}
}
