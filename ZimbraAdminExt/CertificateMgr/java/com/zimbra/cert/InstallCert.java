/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cert;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.cs.rmgmt.RemoteResultParser;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;


public class InstallCert extends AdminDocumentHandler {
    private final static String TYPE = "type" ;
    final static String CERT_TYPE_SELF= "self" ;
    final static String CERT_TYPE_COMM = "comm" ;
    private final static String AID = "aid" ;
    private final static String ALLSERVER = "allserver" ;
    private final static String ALLSERVER_FLAG = "-allserver" ;
    private final static String VALIDATION_DAYS = "validation_days" ;
     
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        //get a server
        List<Server> serverList =  prov.getAllServers();
        Server server = ZimbraCertMgrExt.getCertServer(serverList);
        
        if (server == null) {
            throw ServiceException.INVALID_REQUEST("No valid server was found", null);
        }
        RemoteManager rmgr = RemoteManager.getRemoteManager(server);
        String cmd = ZimbraCertMgrExt.INSTALL_CERT_CMD ;
        String certType = request.getAttribute(TYPE) ;
        if (certType == null || certType.length() == 0 ) {
            throw ServiceException.INVALID_REQUEST("No valid certificate type is set", null);
        }else if (certType.equals(CERT_TYPE_SELF) || certType.equals(CERT_TYPE_COMM)) {
            cmd += " " + certType  ;   
        }else {
            throw ServiceException.INVALID_REQUEST("Invalid certificate type: " + certType + ". Must be (self|comm).", null);
        }
        
        if (certType.equals("comm")) {
            String attachId = request.getAttribute(AID);
            Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachId, lc.getRawAuthToken());
            if (up == null)
                throw ServiceException.FAILURE("Uploaded file with " + attachId + " was not found.", null);
           
            checkUploadedCommCert(attachId, up, rmgr) ;
        }
        
        //always set the -new flag for the cmd since the ac requests for a new cert always
        cmd += " -new " ;
        
        Element valDayEl = request.getElement(VALIDATION_DAYS) ;
        String validation_days = null ;
                
        if ((valDayEl != null) && (!certType.equals("comm"))) {
            validation_days = valDayEl.getText() ;
            if (validation_days != null && validation_days.length() > 0) {
                cmd += " " + validation_days ;
            }
        }
        
        try {
            Element allserverEl = request.getElement(ALLSERVER) ;
            String allserver = allserverEl.getText() ;
            if (allserver != null && allserver.equals("1")) {
                cmd += " " + ALLSERVER_FLAG;
            }
        }catch (ServiceException e) {
            //allserver parameter is not present. Ignore
        }
                
        ZimbraLog.security.debug("***** Executing the cmd = " + cmd) ;
        RemoteResult rr = rmgr.execute(cmd);
        //ZimbraLog.security.info("***** Exit Status Code = " + rr.getMExitStatus()) ;
        try {
            OutputParser.parseOuput(rr.getMStdout()) ;
        }catch (IOException ioe) {
            throw ServiceException.FAILURE("exception occurred handling command", ioe);
        }
        
        /** getMExitStatus code is not properly implemented
        if (rr.getMExitStatus() != 0) {
            String error = new String (rr.getMStderr()) ;
            throw ServiceException.FAILURE("exception occurred handling command", new Exception (error));
        }**/
        
        Element response = lc.createElement(ZimbraCertMgrService.INSTALL_CERT_RESPONSE);
        
        return response;    
    }
    
    private boolean checkUploadedCommCert (String aid, Upload up, RemoteManager rmgr) throws ServiceException {
        InputStream is = null ;
        try {
            is = up.getInputStream() ;
            byte [] content = ByteUtil.getContent(is, 1024) ;
            ZimbraLog.security.debug ("Put the uploaded commercial crt  to " + ZimbraCertMgrExt.UPLOADED_CRT_FILE) ;
            ByteUtil.putContent(ZimbraCertMgrExt.UPLOADED_CRT_FILE, content) ;
            
            //run zmcertmgr verifycrt to validate the cert and key
            String cmd = ZimbraCertMgrExt.VERIFY_CRT_CMD + " comm "
                        + " " + ZimbraCertMgrExt.COMM_CRT_KEY_FILE
                        + " " + ZimbraCertMgrExt.UPLOADED_CRT_FILE ;
            ZimbraLog.security.debug("*****  Executing the cmd: " + cmd);
            RemoteResult rr = rmgr.execute(cmd) ;
            try {
                OutputParser.parseOuput(rr.getMStdout()) ;
            }catch (IOException ioe) {
                throw ServiceException.FAILURE("exception occurred handling command", ioe);
            }
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error reading uploaded certificate", ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    ZimbraLog.security.warn("exception closing uploaded certificate:", ioe);
                }
            }
        }

        return true ;
    }
}
