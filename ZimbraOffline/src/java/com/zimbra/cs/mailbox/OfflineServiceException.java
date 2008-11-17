/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;

public class OfflineServiceException extends ServiceException {
    private static final long serialVersionUID = -6070768925605337011L;

    public static final String MISCONFIGURED         = "offline.MISCONFIGURED";
    public static final String FOLDER_NOT_EMPTY      = "offline.FOLDER_NOT_EMPTY";
    public static final String UNSUPPORTED_OPERATION = "offline.UNSUPPORTED";
    
    public static final String UNEXPECTED = "offline.UNEXPECTED";
    public static final String AUTH_FAILED = "offline.AUTH_FAILED";
    public static final String OUT_OF_SYNC = "offline.OUT_OF_SYNC";
    public static final String MISSING_GAL_MAILBOX = "offline.MISSING_GAL_MAILBOX";
    public static final String ONLINE_ONLY_OP = "offline.ONLINE_ONLY_OP";
    public static final String MOUNT_OP_UNSUPPORTED = "offline.MOUNT_OP_UNSUPPORTED";
    public static final String MOUNT_EXISTING_ACCT = "offline.MOUNT_EXISTING_ACCT";
    
    public static final String CALDAV_LOGIN_FAILED ="offlline.CALDAV_LOGIN_FAILED";
    public static final String YCALDAV_NEED_UPGRADE = "offline.YCALDAV_NEED_UPGRADE";
    public static final String GCALDAV_NEED_ENABLE = "offline.GCALDAV_NEED_ENABLE";
    
    public static final String ITEM_ID = "itemId";

    private OfflineServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }

    public static OfflineServiceException MISCONFIGURED(String error) {
        return new OfflineServiceException("configuration error: " + error, MISCONFIGURED, RECEIVERS_FAULT);
    }

    public static OfflineServiceException FOLDER_NOT_EMPTY(int id) {
        return new OfflineServiceException("cannot delete non-empty folder: "+ id, FOLDER_NOT_EMPTY, RECEIVERS_FAULT, new Argument(ITEM_ID, id, Argument.Type.IID));
    }

    public static OfflineServiceException UNSUPPORTED(String op) {
        return new OfflineServiceException("operation not supported by offline client: " + op, UNSUPPORTED_OPERATION, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException UNEXPECTED(String error) {
    	return new OfflineServiceException("unexpected failure: " + error, UNEXPECTED, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException AUTH_FAILED(String username, String message) {
    	return new OfflineServiceException("authentication failed for " + username + ": " + message, AUTH_FAILED, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException OUT_OF_SYNC() {
    	return new OfflineServiceException("out of sync", OUT_OF_SYNC, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException MISSING_GAL_MAILBOX(String acctName) {
        return new OfflineServiceException("unable to access GAL mailbox for " + acctName, MISSING_GAL_MAILBOX, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException ONLINE_ONLY_OP(String op) {
        return new OfflineServiceException("operation only supported when client is online: " + op, ONLINE_ONLY_OP, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException MOUNT_OP_UNSUPPORTED() {
        return new OfflineServiceException("sharing the same resource by more than one account is unsupported", MOUNT_OP_UNSUPPORTED, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException MOUNT_EXISTING_ACCT() {
        return new OfflineServiceException("can not mount resource from an existing account", MOUNT_EXISTING_ACCT, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException CALDAV_LOGIN_FAILED() {
        return new OfflineServiceException("CalDAV login failed", CALDAV_LOGIN_FAILED, RECEIVERS_FAULT);
    }
    
    public static OfflineServiceException YCALDAV_NEED_UPGRADE() {
        return new OfflineServiceException("must upgrade to all-new yahoo calendar service", YCALDAV_NEED_UPGRADE, RECEIVERS_FAULT); 
    }
    
    public static OfflineServiceException GCALDAV_NEED_ENABLE() {
        return new OfflineServiceException("must enable google calendar service", GCALDAV_NEED_ENABLE, RECEIVERS_FAULT); 
    }
}

