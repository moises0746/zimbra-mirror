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

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class RetentionPolicy {
    
    @XmlElementWrapper(name=MailConstants.E_KEEP, required=false)
    @XmlElement(name=MailConstants.E_POLICY, required=false)
    private List<Policy> keep = Lists.newArrayList();
    
    @XmlElementWrapper(name=MailConstants.E_PURGE, required=false)
    @XmlElement(name=MailConstants.E_POLICY, required=false)
    private List<Policy> purge = Lists.newArrayList();
    
    public RetentionPolicy() {
    }
    
    public RetentionPolicy(Element e)
    throws ServiceException {
        Element keepEl = e.getOptionalElement(MailConstants.E_KEEP);
        if (keepEl != null) {
            for (Element p : keepEl.listElements(MailConstants.E_POLICY)) {
                keep.add(new Policy(p));
            }
        }
        Element purgeEl = e.getOptionalElement(MailConstants.E_PURGE);
        if (purgeEl != null) {
            for (Element p : purgeEl.listElements(MailConstants.E_POLICY)) {
                purge.add(new Policy(p));
            }
        }
    }
    
    public List<Policy> getKeepPolicy() {
        return Collections.unmodifiableList(keep);
    }
    
    public List<Policy> getPurgePolicy() {
        return Collections.unmodifiableList(purge);
    }
    
    public void setKeepPolicy(Iterable<Policy> keep) {
        this.keep.clear();
        if (keep != null) {
            Iterables.addAll(this.keep, keep);
        }
    }
    
    public void setPurgePolicy(Iterable<Policy> purge) {
        this.purge.clear();
        if (purge != null) {
            Iterables.addAll(this.purge, purge);
        }
    }
}
