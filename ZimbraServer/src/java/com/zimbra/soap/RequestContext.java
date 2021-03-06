/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra, Inc.
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

package com.zimbra.soap;



/**
 * @author zimbra
 *
 */
public class RequestContext {


    /**
     * @return the reqHost
     */
    public String getVirtualHost() {
        return virtualHost;
    }


    /**
     * @param reqHost the reqHost to set
     */
    public void setVirtualHost(String reqHost) {
        this.virtualHost = reqHost;
    }

    private String virtualHost;

}
