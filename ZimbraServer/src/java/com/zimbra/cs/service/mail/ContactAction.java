/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class ContactAction extends ItemAction {

	public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_READ) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on contact: " + operation, null);
        String successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_CONTACT);

        Element response = lc.createElement(MailService.CONTACT_ACTION_RESPONSE);
        Element actionOut = response.addUniqueElement(MailService.E_ACTION);
        actionOut.addAttribute(MailService.A_ID, successes);
        actionOut.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }
}
