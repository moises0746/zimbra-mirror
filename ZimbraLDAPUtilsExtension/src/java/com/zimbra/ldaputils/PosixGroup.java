/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
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
package com.zimbra.ldaputils;

import java.util.Map;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

/**
 * @author Greg Solovyev
 */
public class PosixGroup extends LDAPUtilEntry {
	private static final String A_gidNumber = "gidNumber";
	
	public PosixGroup(String dn, ZAttributes attrs,
            Map<String, Object> defaults) throws LdapException {
        super(dn, attrs, defaults);
        mId = attrs.getAttrString(A_gidNumber);
    }

    public String getId() {
        return getAttr(A_gidNumber);
    }
}
