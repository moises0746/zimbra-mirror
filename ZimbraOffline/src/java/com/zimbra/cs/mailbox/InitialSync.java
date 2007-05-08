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
 * Portions created by Zimbra are Copyright (C) 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.MessagingException;

import org.apache.commons.httpclient.Header;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.OfflineMailbox.OfflineContext;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.offline.Offline;
import com.zimbra.cs.offline.OfflineLC;
import com.zimbra.cs.offline.OfflineLog;
import com.zimbra.cs.redolog.op.CreateChat;
import com.zimbra.cs.redolog.op.CreateContact;
import com.zimbra.cs.redolog.op.CreateFolder;
import com.zimbra.cs.redolog.op.CreateMessage;
import com.zimbra.cs.redolog.op.CreateMountpoint;
import com.zimbra.cs.redolog.op.CreateSavedSearch;
import com.zimbra.cs.redolog.op.CreateTag;
import com.zimbra.cs.redolog.op.SaveChat;
import com.zimbra.cs.redolog.op.SaveDraft;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.formatter.SyncFormatter;
import com.zimbra.cs.service.mail.FolderAction;
import com.zimbra.cs.service.mail.Sync;
import com.zimbra.cs.session.PendingModifications.Change;

public class InitialSync {

    static final String A_RELOCATED = "relocated";

    private static final OfflineContext sContext = new OfflineContext();

    private final OfflineMailbox ombx;
    private DeltaSync dsync;
    private Element syncResponse;
    private boolean interrupted;

    InitialSync(OfflineMailbox mbox) {
        ombx = mbox;
    }

    InitialSync(DeltaSync delta) {
        ombx = delta.getMailbox();
        dsync = delta;
    }

    OfflineMailbox getMailbox() {
        return ombx;
    }

    private DeltaSync getDeltaSync() {
        if (dsync == null)
            dsync = new DeltaSync(this);
        return dsync;
    }


    public static String sync(OfflineMailbox ombx) throws ServiceException {
        return new InitialSync(ombx).sync();
    }

    public String sync() throws ServiceException {
        Element request = new Element.XMLElement(MailConstants.SYNC_REQUEST);
        syncResponse = ombx.sendRequest(request);
        String token = syncResponse.getAttribute(MailConstants.A_TOKEN);

        OfflineLog.offline.debug("starting initial sync");
        ombx.updateInitialSync(syncResponse);
        initialFolderSync(syncResponse.getElement(MailConstants.E_FOLDER));
        ombx.recordSyncComplete(token);
        OfflineLog.offline.debug("ending initial sync");

        return token;
    }

    public static String resume(OfflineMailbox ombx) throws ServiceException {
        return new InitialSync(ombx).resume();
    }

    public String resume() throws ServiceException {
        // do a NOOP before resuming to make sure the link is viable
        ombx.sendRequest(new Element.XMLElement(MailConstants.NO_OP_REQUEST));

        syncResponse = ombx.getInitialSyncResponse();
        String token = syncResponse.getAttribute(MailConstants.A_TOKEN);
        interrupted = true;

        OfflineLog.offline.debug("resuming initial sync");
        initialFolderSync(syncResponse.getElement(MailConstants.E_FOLDER));
        ombx.recordSyncComplete(token);
        OfflineLog.offline.debug("ending initial sync");

        return token;
    }

    static final Set<String> KNOWN_FOLDER_TYPES = new HashSet<String>(Arrays.asList(
            MailConstants.E_FOLDER, MailConstants.E_SEARCH, MailConstants.E_MOUNT
    ));

    private void initialFolderSync(Element elt) throws ServiceException {
        int folderId = (int) elt.getAttributeLong(MailConstants.A_ID);

        // first, sync the container itself
        syncContainer(elt, folderId);

        // next, sync the leaf-node contents
        if (elt.getName().equals(MailConstants.E_FOLDER)) {
            if (folderId == Mailbox.ID_FOLDER_TAGS) {
                for (Element eTag : elt.listElements(MailConstants.E_TAG)) {
                    syncTag(eTag);
                    eTag.detach();
                }
            }

            Element eMessageIds = elt.getOptionalElement(MailConstants.E_MSG);
            if (eMessageIds != null) {
                syncMessagelikeItems(eMessageIds.getAttribute(MailConstants.A_IDS).split(","), folderId, MailItem.TYPE_MESSAGE);
                eMessageIds.detach();
                ombx.updateInitialSync(syncResponse);
            }

            Element eChatIds = elt.getOptionalElement(MailConstants.E_CHAT);
            if (eChatIds != null) {
                syncMessagelikeItems(eChatIds.getAttribute(MailConstants.A_IDS).split(","), folderId, MailItem.TYPE_CHAT);
                eChatIds.detach();
                ombx.updateInitialSync(syncResponse);
            }

            Element eContactIds = elt.getOptionalElement(MailConstants.E_CONTACT);
            if (eContactIds != null) {
                String ids = eContactIds.getAttribute(MailConstants.A_IDS);
                for (Element eContact : fetchContacts(ombx, ids)) {
                    if (!isAlreadySynced((int) eContact.getAttributeLong(MailConstants.A_ID), MailItem.TYPE_CONTACT))
                        syncContact(eContact, folderId);
                }
                eContactIds.detach();
                ombx.updateInitialSync(syncResponse);
            }
        }

        // now, sync the children (with special priority given to Tags, Inbox, and Sent)
        for (Element child : elt.listElements()) {
            if (KNOWN_FOLDER_TYPES.contains(child.getName())) {
                switch ((int) child.getAttributeLong(MailConstants.A_ID)) {
                    case Mailbox.ID_FOLDER_TAGS:
                    case Mailbox.ID_FOLDER_SENT:
                    case Mailbox.ID_FOLDER_INBOX:  initialFolderSync(child);
                }
            }
        }

        for (Element child : elt.listElements()) {
            if (KNOWN_FOLDER_TYPES.contains(child.getName()))
                initialFolderSync(child);
        }

        // finally, remove the node from the folder hierarchy to note that it's been processed
        elt.detach();
        ombx.updateInitialSync(syncResponse);
    }

    private boolean isAlreadySynced(int id, byte type) throws ServiceException {
        if (!interrupted)
            return false;

        try {
            ombx.getItemById(sContext, id, type);
            return true;
        } catch (NoSuchItemException nsie) {
            boolean synced = ombx.isPendingDelete(sContext, id, type);
            if (!synced)
                interrupted = false;
            return synced;
        }
    }

    private void syncMessagelikeItems(String[] ids, int folderId, byte type) throws ServiceException {
        int counter = 0, lastItem = ombx.getLastSyncedItem();
        List<Integer> itemList = new ArrayList<Integer>();
        for (String msgId : ids) {
            int id = Integer.parseInt(msgId);
            if (interrupted && lastItem > 0) {
                if (id != lastItem)  continue;
                else                 lastItem = 0;
            }
            if (isAlreadySynced(id, MailItem.TYPE_MESSAGE))
                continue;

            int batchSize = OfflineLC.zdesktop_sync_batch_size.intValue();
            if (ombx.getRemoteServerVersion().getMajor() < 5 || batchSize == 1) {
                syncMessage(id, folderId, type);
                if (++counter % 100 == 0)
                    ombx.updateInitialSync(syncResponse, id);
            } else {
                itemList.add(id);
                if ((++counter % batchSize) == 0) {
                    syncMessages(itemList, type);
                    ombx.updateInitialSync(syncResponse, id);
                    itemList.clear();
                }
            }
        }
        if (!itemList.isEmpty())
            syncMessages(itemList, type);
    }

    private void syncContainer(Element elt, int id) throws ServiceException {
        String type = elt.getName();
        if (type.equalsIgnoreCase(MailConstants.E_SEARCH))
            syncSearchFolder(elt, id);
        else if (type.equalsIgnoreCase(MailConstants.E_MOUNT))
            syncMountpoint(elt, id);
        else if (type.equalsIgnoreCase(MailConstants.E_FOLDER))
            syncFolder(elt, id);
    }

    void syncSearchFolder(Element elt, int id) throws ServiceException {
        int parentId = (int) elt.getAttributeLong(MailConstants.A_FOLDER);
        String name = MailItem.normalizeItemName(elt.getAttribute(MailConstants.A_NAME));
        byte color = (byte) elt.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);
        int flags = Flag.flagsToBitmask(elt.getAttribute(MailConstants.A_FLAGS, null));

        int timestamp = (int) elt.getAttributeLong(MailConstants.A_CHANGE_DATE);
        int changeId = (int) elt.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE);
        int date = (int) (elt.getAttributeLong(MailConstants.A_DATE, -1000) / 1000);
        int mod_content = (int) elt.getAttributeLong(MailConstants.A_REVISION, -1);

        String query = elt.getAttribute(MailConstants.A_QUERY);
        String searchTypes = elt.getAttribute(MailConstants.A_SEARCH_TYPES);
        String sort = elt.getAttribute(MailConstants.A_SORTBY);

        boolean relocated = elt.getAttributeBool(A_RELOCATED, false) || !name.equals(elt.getAttribute(MailConstants.A_NAME));

        CreateSavedSearch redo = new CreateSavedSearch(ombx.getId(), parentId, name, query, searchTypes, sort, color);
        redo.setSearchId(id);
        redo.setChangeId(mod_content);
        redo.start(timestamp * 1000L);

        try {
            // XXX: FLAGS should be settable in the SearchFolder create...
            ombx.createSearchFolder(new OfflineContext(redo), parentId, name, query, searchTypes, sort, color);
            if (relocated)
                ombx.setChangeMask(sContext, id, MailItem.TYPE_SEARCHFOLDER, Change.MODIFIED_FOLDER | Change.MODIFIED_NAME);
            ombx.setTags(sContext, id, MailItem.TYPE_SEARCHFOLDER, flags, MailItem.TAG_UNCHANGED);
            ombx.syncChangeIds(sContext, id, MailItem.TYPE_SEARCHFOLDER, date, mod_content, timestamp, changeId);
            OfflineLog.offline.debug("initial: created search folder (" + id + "): " + name);
        } catch (ServiceException e) {
            if (e.getCode() != MailServiceException.ALREADY_EXISTS)
                throw e;
            getDeltaSync().syncSearchFolder(elt, id);
        }
    }

    void syncMountpoint(Element elt, int id) throws ServiceException {
        int parentId = (int) elt.getAttributeLong(MailConstants.A_FOLDER);
        String name = MailItem.normalizeItemName(elt.getAttribute(MailConstants.A_NAME));
        int flags = Flag.flagsToBitmask(elt.getAttribute(MailConstants.A_FLAGS, null));
        byte color = (byte) elt.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);
        byte view = MailItem.getTypeForName(elt.getAttribute(MailConstants.A_DEFAULT_VIEW, null));

        int timestamp = (int) elt.getAttributeLong(MailConstants.A_CHANGE_DATE);
        int changeId = (int) elt.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE);
        int date = (int) (elt.getAttributeLong(MailConstants.A_DATE, -1000) / 1000);
        int mod_content = (int) elt.getAttributeLong(MailConstants.A_REVISION, -1);

        String zid = elt.getAttribute(MailConstants.A_ZIMBRA_ID);
        int rid = (int) elt.getAttributeLong(MailConstants.A_REMOTE_ID);

        boolean relocated = elt.getAttributeBool(A_RELOCATED, false) || !name.equals(elt.getAttribute(MailConstants.A_NAME));

        CreateMountpoint redo = new CreateMountpoint(ombx.getId(), parentId, name, zid, rid, view, flags, color);
        redo.setId(id);
        redo.setChangeId(mod_content);
        redo.start(timestamp * 1000L);

        try {
            ombx.createMountpoint(new OfflineContext(redo), parentId, name, zid, rid, view, flags, color);
            if (relocated)
                ombx.setChangeMask(sContext, id, MailItem.TYPE_MOUNTPOINT, Change.MODIFIED_FOLDER | Change.MODIFIED_NAME);
            ombx.syncChangeIds(sContext, id, MailItem.TYPE_MOUNTPOINT, date, mod_content, timestamp, changeId);
            OfflineLog.offline.debug("initial: created mountpoint (" + id + "): " + name);
        } catch (ServiceException e) {
            if (e.getCode() != MailServiceException.ALREADY_EXISTS)
                throw e;
            getDeltaSync().syncMountpoint(elt, id);
        }
    }

    void syncFolder(Element elt, int id) throws ServiceException {
        if (id <= Mailbox.HIGHEST_SYSTEM_ID) {
            // we know the system folders already exist...
            getDeltaSync().syncFolder(elt, id);
            return;
        }

        int parentId = (id == Mailbox.ID_FOLDER_ROOT) ? id : (int) elt.getAttributeLong(MailConstants.A_FOLDER);
        String name = (id == Mailbox.ID_FOLDER_ROOT) ? "ROOT" : MailItem.normalizeItemName(elt.getAttribute(MailConstants.A_NAME));
        int flags = Flag.flagsToBitmask(elt.getAttribute(MailConstants.A_FLAGS, null));
        byte color = (byte) elt.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);
        byte view = MailItem.getTypeForName(elt.getAttribute(MailConstants.A_DEFAULT_VIEW, null));

        int timestamp = (int) elt.getAttributeLong(MailConstants.A_CHANGE_DATE);
        int changeId = (int) elt.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE);
        int date = (int) (elt.getAttributeLong(MailConstants.A_DATE, -1000) / 1000);
        int mod_content = (int) elt.getAttributeLong(MailConstants.A_REVISION, -1);

        ACL acl = parseACL(elt.getOptionalElement(MailConstants.E_ACL));
        String url = elt.getAttribute(MailConstants.A_URL, null);

        boolean relocated = elt.getAttributeBool(A_RELOCATED, false) || (id != Mailbox.ID_FOLDER_ROOT && !name.equals(elt.getAttribute(MailConstants.A_NAME)));

        CreateFolder redo = new CreateFolder(ombx.getId(), name, parentId, view, flags, color, url);
        redo.setFolderId(id);
        redo.setChangeId(mod_content);
        redo.start(timestamp * 1000L);

        try {
            // don't care about current feed syncpoint; sync can't be done offline
            ombx.createFolder(new OfflineContext(redo), name, parentId, view, flags, color, url);
            if (relocated)
                ombx.setChangeMask(sContext, id, MailItem.TYPE_FOLDER, Change.MODIFIED_FOLDER | Change.MODIFIED_NAME);
            if (acl != null)
                ombx.setPermissions(sContext, id, acl);
            ombx.syncChangeIds(sContext, id, MailItem.TYPE_FOLDER, date, mod_content, timestamp, changeId);
            OfflineLog.offline.debug("initial: created folder (" + id + "): " + name);
        } catch (ServiceException e) {
            if (e.getCode() != MailServiceException.ALREADY_EXISTS)
                throw e;
            getDeltaSync().syncFolder(elt, id);
        }
    }

    ACL parseACL(Element eAcl) throws ServiceException {
        if (eAcl == null)
            return null;
        ACL acl = new ACL();
        for (Element eGrant : eAcl.listElements(MailConstants.E_GRANT)) {
            short rights = ACL.stringToRights(eGrant.getAttribute(MailConstants.A_RIGHTS));
            byte gtype = FolderAction.stringToType(eGrant.getAttribute(MailConstants.A_GRANT_TYPE));
            String zid = eGrant.getAttribute(MailConstants.A_ZIMBRA_ID, null);
            // FIXME: does not support passwords for external user access
            acl.grantAccess(zid, gtype, rights, null);
        }
        return acl;
    }

    void syncTag(Element elt) throws ServiceException {
        int id = (int) elt.getAttributeLong(MailConstants.A_ID);
        String name = MailItem.normalizeItemName(elt.getAttribute(MailConstants.A_NAME));
        byte color = (byte) elt.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);

        int timestamp = (int) elt.getAttributeLong(MailConstants.A_CHANGE_DATE);
        int changeId = (int) elt.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE);
        int date = (int) (elt.getAttributeLong(MailConstants.A_DATE) / 1000);
        int mod_content = (int) elt.getAttributeLong(MailConstants.A_REVISION);

        boolean renamed = elt.getAttributeBool(A_RELOCATED, false) || !name.equals(elt.getAttribute(MailConstants.A_NAME));

        CreateTag redo = new CreateTag(ombx.getId(), name, color);
        redo.setTagId(id);
        redo.setChangeId(mod_content);
        redo.start(date * 1000L);

        try {
            // don't care about current feed syncpoint; sync can't be done offline
            ombx.createTag(new OfflineContext(redo), name, color);
            if (renamed)
                ombx.setChangeMask(sContext, id, MailItem.TYPE_TAG, Change.MODIFIED_NAME);
            ombx.syncChangeIds(sContext, id, MailItem.TYPE_TAG, date, mod_content, timestamp, changeId);
            OfflineLog.offline.debug("initial: created tag (" + id + "): " + name);
        } catch (ServiceException e) {
            if (e.getCode() != MailServiceException.ALREADY_EXISTS)
                throw e;
            getDeltaSync().syncTag(elt);
        }
    }

    static List<Element> fetchContacts(OfflineMailbox ombx, String ids) throws ServiceException {
        try {
            Element request = new Element.XMLElement(MailConstants.GET_CONTACTS_REQUEST);
            request.addAttribute(MailConstants.A_SYNC, true);
            request.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, ids);
            return ombx.sendRequest(request).listElements(MailConstants.E_CONTACT);
        } catch (SoapFaultException sfe) {
            if (!sfe.getCode().equals(MailServiceException.NO_SUCH_CONTACT))
                throw sfe;

            String[] contactIds = ids.split(",");
            if (contactIds.length <= 1)
                return Collections.emptyList();

            Element batch = new Element.XMLElement(ZimbraNamespace.E_BATCH_REQUEST);
            for (String id : contactIds) {
                Element request = batch.addElement(MailConstants.GET_CONTACTS_REQUEST);
                request.addAttribute(MailConstants.A_SYNC, true).addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, id);
            }
            List<Element> contacts = new ArrayList<Element>(contactIds.length - 1);
            for (Element response : ombx.sendRequest(batch).listElements(MailConstants.GET_CONTACTS_RESPONSE.getName()))
                contacts.addAll(response.listElements(MailConstants.E_CONTACT));
            return contacts;
        }
    }

    void syncContact(Element elt, int folderId) throws ServiceException {
        int id = (int) elt.getAttributeLong(MailConstants.A_ID);
        byte color = (byte) elt.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);
        int flags = Flag.flagsToBitmask(elt.getAttribute(MailConstants.A_FLAGS, null));
        String tags = elt.getAttribute(MailConstants.A_TAGS, null);

        Map<String, String> fields = new HashMap<String, String>();
        for (Element eField : elt.listElements())
            fields.put(eField.getAttribute(Element.XMLElement.A_ATTR_NAME), eField.getText());

        int timestamp = (int) elt.getAttributeLong(MailConstants.A_CHANGE_DATE);
        int changeId = (int) elt.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE);
        int date = (int) (elt.getAttributeLong(MailConstants.A_DATE) / 1000);
        int mod_content = (int) elt.getAttributeLong(MailConstants.A_REVISION);

        byte[] blob = null;
        if ((flags & Flag.BITMASK_ATTACHED) != 0) {
            String url = Offline.getServerURI(ombx.getAccount(), UserServlet.SERVLET_PATH + "/~/?fmt=native&id=" + id);
            OfflineLog.request.debug("GET " + url);
            try {
                String hostname = new URL(url).getHost();
                blob = UserServlet.getRemoteContent(ombx.getAuthToken(), hostname, url);
            } catch (MailServiceException.NoSuchItemException nsie) {
                OfflineLog.offline.warn("initial: no blob available for contact " + id);
            } catch (MalformedURLException e) {
                OfflineLog.offline.error("initial: base URI is invalid; aborting: " + url, e);
                throw ServiceException.FAILURE("base URI is invalid: " + url, e);
            }
        }
        ParsedContact pc = new ParsedContact(fields, blob);

        CreateContact redo = new CreateContact(ombx.getId(), folderId, pc, tags);
        redo.setContactId(id);
        redo.setChangeId(mod_content);
        redo.start(date * 1000L);

        try {
            Contact cn = ombx.createContact(new OfflineContext(redo), pc, folderId, tags);
            if (flags != 0)
                ombx.setTags(sContext, id, MailItem.TYPE_CONTACT, flags, MailItem.TAG_UNCHANGED);
            if (color != MailItem.DEFAULT_COLOR)
                ombx.setColor(sContext, id, MailItem.TYPE_CONTACT, color);
            ombx.syncChangeIds(sContext, id, MailItem.TYPE_CONTACT, date, mod_content, timestamp, changeId);
            OfflineLog.offline.debug("initial: created contact (" + id + "): " + cn.getFileAsString());
        } catch (ServiceException e) {
            if (e.getCode() != MailServiceException.ALREADY_EXISTS)
                throw e;
            getDeltaSync().syncContact(elt, folderId);
        }
    }


    private static final Map<String, String> USE_SYNC_FORMATTER = new HashMap<String, String>();
        static {
            USE_SYNC_FORMATTER.put(UserServlet.QP_FMT, "sync");
            USE_SYNC_FORMATTER.put(SyncFormatter.QP_NOHDR, "1");
        }

    private Map<String, String> recoverHeadersFromBytes(byte[] hdrBytes) {
    	Map<String, String> headers = new HashMap<String, String>();
    	if (hdrBytes != null && hdrBytes.length > 0) {
    		String[] keyVals = new String(hdrBytes).split("\r\n");
    		for (String hdr : keyVals) {
    			int delim = hdr.indexOf(": ");
    			headers.put(hdr.substring(0, delim), hdr.substring(delim + 2));
    		}
    	}
    	return headers;
    }
    
    private void syncMessages(List<Integer> ids, byte type) throws ServiceException {
    	UserServlet.HttpInputStream in = null;
    	try {
	    	String url = Offline.getServerURI(ombx.getAccount(), UserServlet.SERVLET_PATH + "/~/?fmt=zip&list=" + StringUtil.join(",", ids));
	    	OfflineLog.request.debug("GET " + url);
	        try {
	            String hostname = new URL(url).getHost();
	            Pair<Header[], UserServlet.HttpInputStream> response = UserServlet.getRemoteResourceAsStream(ombx.getAuthToken(), hostname, url);
	            in = response.getSecond();
	        } catch (MailServiceException.NoSuchItemException nsie) {
	            OfflineLog.offline.info("initial: messages have been deleted; skipping");
	            return;
	        } catch (MalformedURLException e) {
	            OfflineLog.offline.error("initial: base URI is invalid; aborting: " + url, e);
	            throw ServiceException.FAILURE("base URI is invalid: " + url, e);
	        } catch (IOException x) {
	        	OfflineLog.offline.error("initial: can't read sync response: " + url, x);
	        	throw ServiceException.FAILURE("can't read sync response: " + url, x);
	        }
	        
	        ZipInputStream zin = new ZipInputStream(in);
	        ZipEntry entry = null;
	        try {
		        while ((entry = zin.getNextEntry()) != null) {
		        	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	                byte[] buffer = new byte[4096];
	                int len;

                    while ((len = zin.read(buffer)) > 0)
	                    bout.write(buffer, 0, len);
		        	Map<String, String> headers = recoverHeadersFromBytes(entry.getExtra());
		        	int id = Integer.parseInt(headers.get("X-Zimbra-ItemId"));
		        	int folderId = Integer.parseInt(headers.get("X-Zimbra-FolderId"));
		        	
		        	saveMessage(bout.toByteArray(), headers, id, folderId, type);
		        }
	        } catch (IOException x) {
	        	OfflineLog.offline.error("Invalid sync format", x);
	        }
    	} finally {
    		if (in != null)
    			in.close();
    	}
    }
        
    void syncMessage(int id, int folderId, byte type) throws ServiceException {
        byte[] content = null;
        Map<String, String> headers = new HashMap<String, String>();

        String url = Offline.getServerURI(ombx.getAccount(), UserServlet.SERVLET_PATH + "/~/?fmt=sync&nohdr=1&id=" + id);
        OfflineLog.request.debug("GET " + url);
        try {
            String hostname = new URL(url).getHost();
            Pair<Header[], byte[]> response = UserServlet.getRemoteResource(ombx.getAuthToken(), hostname, url);
            content = response.getSecond();
            for (Header hdr : response.getFirst())
                headers.put(hdr.getName(), hdr.getValue());
        } catch (MailServiceException.NoSuchItemException nsie) {
            OfflineLog.offline.info("initial: message " + id + " has been deleted; skipping");
            return;
        } catch (MalformedURLException e) {
            OfflineLog.offline.error("initial: base URI is invalid; aborting: " + url, e);
            throw ServiceException.FAILURE("base URI is invalid: " + url, e);
        }
        
        saveMessage(content, headers, id, folderId, type);
    }
    
    private void saveMessage(byte[] content, Map<String, String> headers, int id, int folderId, byte type) throws ServiceException {
        int received = (int) (Long.parseLong(headers.get("X-Zimbra-Received")) / 1000);
        int timestamp = (int) (Long.parseLong(headers.get("X-Zimbra-Modified")) / 1000);
        int mod_content = Integer.parseInt(headers.get("X-Zimbra-Revision"));
        int changeId = Integer.parseInt(headers.get("X-Zimbra-Change"));
        int flags = Flag.flagsToBitmask(headers.get("X-Zimbra-Flags"));
        String tags = headers.get("X-Zimbra-Tags");
        int convId = Integer.parseInt(headers.get("X-Zimbra-Conv"));
        if (convId < 0)
            convId = Mailbox.ID_AUTO_INCREMENT;

        ParsedMessage pm = null;
        String digest = null;
        int size;
        try {
            pm = new ParsedMessage(content, received, false);
            digest = pm.getRawDigest();
            size = pm.getRawSize();
        } catch (MessagingException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        } catch (IOException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        }

        CreateMessage redo;
        if (type == MailItem.TYPE_CHAT)
            redo = new CreateChat(ombx.getId(), digest, size, folderId, flags, tags);
        else
            redo = new CreateMessage(ombx.getId(), null, received, false, digest, size, folderId, true, flags, tags);
        redo.setMessageId(id);
        redo.setConvId(convId);
        redo.setChangeId(mod_content);
        redo.start(received * 1000L);

        try {
            // FIXME: not syncing COLOR
            // XXX: need to call with noICal = false
            Message msg;
            if (type == MailItem.TYPE_CHAT)
                msg = ombx.createChat(new OfflineContext(redo), pm, folderId, flags, tags);
            else
                msg = ombx.addMessage(new OfflineContext(redo), pm, folderId, true, flags, tags, convId);
            ombx.syncChangeIds(sContext, id, type, received, mod_content, timestamp, changeId);
            OfflineLog.offline.debug("initial: created " + MailItem.getNameForType(type) + " (" + id + "): " + msg.getSubject());
            return;
        } catch (IOException e) {
            throw ServiceException.FAILURE("storing " + MailItem.getNameForType(type) + " " + id, e);
        } catch (ServiceException e) {
            if (e.getCode() != MailServiceException.ALREADY_EXISTS)
                throw e;
            // fall through...
        }

        // if we're here, the message already exists; save new draft if needed, then update metadata
        try {
            Message msg = ombx.getMessageById(sContext, id);
            if (!digest.equals(msg.getDigest())) {
                pm.analyze();

                // FIXME: should check msg.isDraft() before doing this...
                CreateMessage redo2;
                if (type == MailItem.TYPE_CHAT)
                    redo2 = new SaveChat(ombx.getId(), id, digest, size, folderId, flags, tags);
                else
                    redo2 = new SaveDraft(ombx.getId(), id, digest, size);
                redo2.setChangeId(mod_content);
                redo2.start(received * 1000L);

                synchronized (ombx) {
                    int change_mask = ombx.getChangeMask(sContext, id, type);
                    if ((change_mask & Change.MODIFIED_CONTENT) == 0) {
                        if (type == MailItem.TYPE_CHAT)
                            ombx.updateChat(new OfflineContext(redo2), pm, id);
                        else
                            ombx.saveDraft(new OfflineContext(redo2), pm, id);
                        ombx.syncChangeIds(sContext, id, type, received, mod_content, timestamp, changeId);
                    }
                }
                OfflineLog.offline.debug("initial: re-saved draft " + MailItem.getNameForType(type) + " (" + id + "): " + msg.getSubject());
            }
        } catch (MailServiceException.NoSuchItemException nsie) {
            OfflineLog.offline.debug("initial: " + MailItem.getNameForType(type) + " " + id + " has been deleted; no need to sync draft");
            return;
        } catch (IOException e) {
            throw ServiceException.FAILURE("storing message " + id, e);
        }

        // use this data to generate the XML entry used in message delta sync
        Element sync = new Element.XMLElement(Sync.elementNameForType(type)).addAttribute(MailConstants.A_ID, id);
        sync.addAttribute(MailConstants.A_FLAGS, Flag.bitmaskToFlags(flags)).addAttribute(MailConstants.A_TAGS, tags).addAttribute(MailConstants.A_CONV_ID, convId);
        sync.addAttribute(MailConstants.A_CHANGE_DATE, timestamp).addAttribute(MailConstants.A_MODIFIED_SEQUENCE, changeId);
        sync.addAttribute(MailConstants.A_DATE, received * 1000L).addAttribute(MailConstants.A_REVISION, mod_content);
        getDeltaSync().syncMessage(sync, folderId, type);
    }
}
