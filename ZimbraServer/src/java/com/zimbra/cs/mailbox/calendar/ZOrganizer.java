package com.zimbra.cs.mailbox.calendar;

import java.net.URI;
import java.net.URISyntaxException;

import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.service.ServiceException;

public class ZOrganizer {
    public ZOrganizer(String cn, String address) {
        setCn(cn);
        setAddress(address);
    }
    
//    public ZOrganizer(Organizer org) {
//        ParameterList params = org.getParameters();
//        
//        String addr = org.getCalAddress().getSchemeSpecificPart();
//        
//        // CN
//        Cn cn = (Cn)params.getParameter(Parameter.CN);
//        if (cn != null) {
//            mCn = cn.getValue();
//        }
//        
//        setAddress(addr);
//    }
    

    private static final String FN_ADDRESS         = "a";
    private static final String FN_CN              = "cn";

    public static ZOrganizer parseOrgFromMetadata(Metadata meta) throws ServiceException {
        if (meta == null) {
            return null;
        }
        String addressStr = meta.get(FN_ADDRESS, null);
        String cnStr = meta.get(FN_CN, null);
        
        return new ZOrganizer(cnStr, addressStr);
    }
    
    public Metadata encodeAsMetadata() {
        Metadata meta = new Metadata();
        
        meta.put(FN_ADDRESS, mAddress);
        
        if (mCn != null) {
            meta.put(FN_CN, mCn);
        }
        return meta;
    }
    
    public String getCn() { return mCn; }
    public String getAddress() { return mAddress; }
    public boolean hasCn() { return mCn != null && !mCn.equals(""); }
    
    public void setCn(String cn) { mCn = cn; }
    public void setAddress(String address) { 
        if (address != null) {
            if (address.toLowerCase().startsWith("mailto:")) {
                // MAILTO:  
                address = address.substring(7); 
            }
        }
        mAddress = address; 
    }
    
    public URI getURI() throws ServiceException {
        try {
            return new URI("MAILTO", mAddress, null);
        } catch(URISyntaxException e) {
            throw ServiceException.FAILURE("Could not create URI for address "+mAddress, e);
        }
    }
    
    static ZOrganizer fromProperty(ZProperty prop) {
        String cn = prop.paramVal(ICalTok.CN, null);
        return new ZOrganizer(cn, prop.mValue);
    }
    
    public ZProperty toProperty() {
        ZProperty toRet = new ZProperty(ICalTok.ORGANIZER, "MAILTO:"+getAddress());
        if (hasCn()) {
            toRet.addParameter(new ZParameter(ICalTok.CN, getCn()));
        }
        return toRet;
    }
    
    
    public String toString() {
        if (mCn != null) {
            return "CN=\""+mCn+"\":MAILTO:"+mAddress;
        } else {
            return "MAILTO:"+mAddress;
        }
    }
    
//    public Organizer iCal4jOrganizer() throws ServiceException
//    {
//        ParameterList p = new ParameterList();
//        
//        if (mCn != null && !mCn.equals("")) {
//            Cn cn = new Cn(mCn);
//            p.add(cn);
//        }
//        
//        try {
//            return new Organizer(p, new URI("MAILTO", mAddress, null));
//        } catch (java.net.URISyntaxException e) {
//            throw ServiceException.FAILURE("Building Attendee URI for address "+mAddress, e);
//        }
//    }    

    private String mCn;
    private String mAddress;
}
