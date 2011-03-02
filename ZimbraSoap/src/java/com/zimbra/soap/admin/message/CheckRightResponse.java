/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.RightViaInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_CHECK_RIGHT_RESPONSE)
public class CheckRightResponse {

    @XmlAttribute(name=AdminConstants.A_ALLOW, required=true)
    private final boolean allow;

    @XmlElement(name=AdminConstants.E_VIA, required=false)
    private final RightViaInfo via;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckRightResponse() {
        this(false, (RightViaInfo) null);
    }

    public CheckRightResponse(boolean allow, RightViaInfo via) {
        this.allow = allow;
        this.via = via;
    }

    public boolean getAllow() { return allow; }
    public RightViaInfo getVia() { return via; }
}
