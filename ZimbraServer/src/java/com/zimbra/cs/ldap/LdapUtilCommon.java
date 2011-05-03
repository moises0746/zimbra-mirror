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
package com.zimbra.cs.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.naming.ldap.Rdn;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.ldap.legacy.entry.LdapDomain;
import com.zimbra.cs.ldap.LdapTODO.*;


/**
 * Utils methods shared by both the legacy com.zimbra.cs.account.ldap.LdapUtil
 * and the new com.zimbra.cs.ldap.LdapUtil
 */
public class LdapUtilCommon {
    
    public static boolean contains(String[] values, String val) {
        if (values == null) {
            return false;
        }
        
        for (String s : values) {
            if (s.compareToIgnoreCase(val) == 0) {
                return true;
            }
        }
        return false;
    }
    
    public static Object decodeBase64IfBinary(boolean isBinary, String value) {
        return isBinary ? ByteUtil.decodeLDAPBase64(value) : value;
    }
    
    
    /*
     * convert a real attrName to a binaryTransferAttrName if necessary
     * 
     * e.g. userCertificate => userCertificate;binary
     */
    public static String attrNameToBinaryTransferAttrName(boolean isBinaryTransfer, String attrName) {
        return isBinaryTransfer ? attrName + ";binary" : attrName;
    }
    
    /*
     * convert a binaryTransferAttrName to the real attrName
     * 
     * e.g. userCertificate;binary => userCertificate
     *      zimbraId => zimbraId
     */
    public static String binaryTransferAttrNameToAttrName(String transferAttrName) {
        if (transferAttrName.endsWith(";binary")) {
            String[] parts = transferAttrName.split(";");
            if (parts.length == 2) {
                return parts[0];
            }
        }
        return transferAttrName;
    }

    /**
     * escape *()\ in specified string to make sure user-supplied string doesn't open a security hole.
     * i.e., if the format string is "(sn=*%s*)", and the user types in "a)(zimbraIsAdminAccount=TRUE)(cn=a",
     * we don't want to search for "(sn=*a)(zimbraIsAdminAccount=TRUE)(cn=a*)".
     * 
     * @param s
     * @return
     */
    public static String escapeSearchFilterArg(String s) {
        if (s == null)
            return null;
        else 
            return s.replaceAll("([\\\\\\*\\(\\)])", "\\\\$0");
    }
    
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /*
     * we want to throw the IllegalArgumentException instead of catching it so the cause
     * can be logged with the callers catcher.
     */
    public static boolean isValidUUID(String strRep) throws IllegalArgumentException {
        /*
        if (strRep.length() > 36)
            throw new IllegalArgumentException("uuid must be no longer than 36 characters");
        
        UUID uuid = UUID.fromString(strRep);
        return (uuid != null);   
        */
        
        if (strRep.length() > Provisioning.MAX_ZIMBRA_ID_LEN)
            throw new IllegalArgumentException("uuid must be no longer than " + Provisioning.MAX_ZIMBRA_ID_LEN + " characters");
        
        if (strRep.contains(":"))
            throw new IllegalArgumentException("uuid must not contain ':'");
        
        return true;
    }

    /**
     * Return the later (more recent) of two LDAP timestamps.  Timestamp
     * format is YYYYMMDDhhmmssZ. (e.g. 20060315023000Z)
     * @param timeA
     * @param timeB
     * @return later of the two timestamps; a non-null timestamp is considered
     *         later than a null timestamp; null is returned if both timestamps
     *         are null
     */
    public static String getLaterTimestamp(String timeA, String timeB) {
        if (timeA == null) {
            return timeB;
        } else if (timeB == null) {
            return timeA;
        }
        return timeA.compareTo(timeB) > 0 ? timeA : timeB;
    }

    public static String getEarlierTimestamp(String timeA, String timeB) {
        if (timeA == null) {
            return timeB;
        } else if (timeB == null) {
            return timeA;
        }
        return timeA.compareTo(timeB) < 0 ? timeA : timeB;
    }

    /*
      * expansions for bind dn string:
      * 
      * %n = username with @ (or without, if no @ was specified)
      * %u = username with @ removed
      * %d = domain as foo.com
      * %D = domain as dc=foo,dc=com
      * 
      * exchange example, where the exchange domian is different than the zimbra one
      * 
      * zimbraAuthMech      ldap
      * zimbraAuthLdapURL   ldap://exch1/
      * zimbraAuthLdapDn    %n@example.zimbra.com
      * 
      * our own LDAP example:
      * 
      * zimbraAuthMech       ldap
      * zimbraAuthLdapURL    ldap://server.example.zimbra.com/
      * zimbraAuthLdapUserDn uid=%u,ou=people,%D
      */
      public static String computeAuthDn(String name, String bindDnRule) {
         if (bindDnRule == null || bindDnRule.equals("") || bindDnRule.equals("%n"))
             return name;
    
         int at = name.indexOf("@");
    
         Map<String, String> vars = new HashMap<String, String>();
         vars.put("n", name);         
    
         if (at  == -1) {
             vars.put("u", name);
         } else {
             vars.put("u", name.substring(0, at));
             String d = name.substring(at+1);
             vars.put("d", d);
             vars.put("D", LdapUtilCommon.domainToDN(d));
         }
         
         return LdapUtilCommon.expandStr(bindDnRule, vars);
      }

    public static String getBooleanString(boolean b) {
        if (b) {
            return LdapConstants.LDAP_TRUE;
        }
        return LdapConstants.LDAP_FALSE;
    }

    public static String getZimbraSearchBase(Domain domain, GalOp galOp) {
        String sb;
        if (galOp == GalOp.sync) {
            sb = domain.getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase);
            if (sb == null)
                sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
        } else {
            sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
        }
        LdapDomain ld = (LdapDomain) domain;
        if (sb.equalsIgnoreCase("DOMAIN"))
            return ld.getDN();
            //mSearchBase = mDIT.domainDNToAccountSearchDN(ld.getDN());
        else if (sb.equalsIgnoreCase("SUBDOMAINS"))
            return ld.getDN();
        else if (sb.equalsIgnoreCase("ROOT"))
            return "";
        return "";
    }

    //
    // Escape rdn value defined in:
    // http://www.ietf.org/rfc/rfc2253.txt?number=2253
    //
    @TODO  // replace with unboundid's impl, or make this SDK neutral
    public static String escapeRDNValue(String rdn) {
        return (String)Rdn.escapeValue(rdn);
    }

    @TODO  // replace with unboundid's impl, or make this SDK neutral
    public static String unescapeRDNValue(String rdn) {
        return (String)Rdn.unescapeValue(rdn);
    }

    public static String domainToDN(String parts[], int offset) {
        StringBuffer sb = new StringBuffer(128);
        for (int i=offset; i < parts.length; i++) {
            if (i-offset > 0) sb.append(",");
            sb.append("dc=").append(escapeRDNValue(parts[i]));
        }
        return sb.toString();
    }

    /**
     * Given a domain like foo.com, return the dn: dc=foo,dc=com
     * @param domain
     * @return the dn
     */
    public static String domainToDN(String domain) {
        return domainToDN(domain.split("\\."), 0);
    }

    /**
     * given a dn like "uid=foo,ou=people,dc=widgets,dc=com", return the String[]
     * [0] = uid=foo
     * [1] = ou=people,dc=widgets,dc=com
     * 
     * if the dn cannot be split into rdn and dn:
     * [0] = the input dn
     * [1] = the input dn
     * 
     * @param dn
     * @return
     */
    public static String[] dnToRdnAndBaseDn(String dn) {
        String[] values = new String[2];
        int baseDnIdx = dn.indexOf(",");
        
        if (baseDnIdx!=-1 && dn.length()>baseDnIdx+1) {
            values[0] = dn.substring(0, baseDnIdx);
            values[1] = dn.substring(baseDnIdx+1);
        } else {
            values[0] = dn;
            values[1] = dn;
        }
        
        return values;
    }

    /**
       * Takes the specified format string, and replaces any % followed by a single character
       * with the value in the specified vars hash. If the value isn't found in the hash, uses
       * a default value of "".
       * @param fmt the format string
       * @param vars should have a key which is a String, and a value which is also a String.
       * @return the formatted string
       */
    public static String expandStr(String fmt, Map<String, String> vars) {
        if (fmt == null || fmt.equals(""))
            return fmt;
    
        if (fmt.indexOf('%') == -1)
            return fmt;
    
        StringBuffer sb = new StringBuffer(fmt.length()+32);
        for (int i=0; i < fmt.length(); i++) {
            char ch = fmt.charAt(i);
            if (ch == '%') {
                i++;
                if (i > fmt.length())
                    return sb.toString();
                ch = fmt.charAt(i);
                if (ch != '%') {
                    String val = vars.get(Character.toString(ch));
                    if (val != null)
                        sb.append(val);
                    else
                        sb.append(ch);
                } else {
                    sb.append(ch);
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

}
