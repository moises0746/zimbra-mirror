/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
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
package com.zimbra.qa.selenium.projects.ajax.tests.calendar.meetings.attendee.singleday.actions.conversation;

import java.util.*;

import org.testng.annotations.Test;
import com.zimbra.qa.selenium.framework.items.AppointmentItem;
import com.zimbra.qa.selenium.framework.items.FolderItem;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.*;
import com.zimbra.qa.selenium.projects.ajax.ui.calendar.FormApptNew;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.DisplayMail;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew;

public class DeclineProposeNewTime extends CalendarWorkWeekTest {

	public DeclineProposeNewTime() {
		logger.info("New "+ DeclineProposeNewTime.class.getCanonicalName());
		super.startingPage =  app.zPageCalendar;
		super.startingAccountPreferences = null;
	}
	
	@Test(description = "Rt-click to appointment -> Propose New Time and decline the new time using conversation view", 
			groups = { "functional" })
	public void DeclineProposeNewTime_01() throws HarnessException {

		// ------------------------ Test data ------------------------------------

		String organizerEmailAddress, apptAttendee1EmailAddress, apptAttendee2EmailAddress;
		ZimbraAccount organizer, apptAttendee1, apptAttendee2; 
		String apptSubject = ZimbraSeleniumProperties.getUniqueString();
		String apptBody = ZimbraSeleniumProperties.getUniqueString();
		String modifiedBody = ZimbraSeleniumProperties.getUniqueString();
		
		apptAttendee1 = app.zGetActiveAccount();
		apptAttendee1EmailAddress = app.zGetActiveAccount().EmailAddress;
		organizer = ZimbraAccount.AccountA();
		organizerEmailAddress = ZimbraAccount.AccountA().EmailAddress;
		apptAttendee2 = ZimbraAccount.AccountB();
		apptAttendee2EmailAddress = ZimbraAccount.AccountB().EmailAddress;

		Calendar now = this.calendarWeekDayUTC;
		AppointmentItem appt = new AppointmentItem();
		ZDate startUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 17, 0, 0);
		ZDate endUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 18, 0, 0);
		ZDate modifiedStartUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 18, 0, 0);
		ZDate modifiedEndUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 19, 0, 0);
		
		// --------------- Creating invitation (apptAttendee1) ----------------------------
		organizer.soapSend(
				"<CreateAppointmentRequest xmlns='urn:zimbraMail'>"
				+		"<m>"
				+			"<inv method='REQUEST' type='event' status='CONF' draft='0' class='PUB' fb='B' transp='O' allDay='0' name='"+ apptSubject +"'>"
				+				"<s d='"+ startUTC.toTimeZone(ZTimeZone.TimeZoneEST.getID()).toYYYYMMDDTHHMMSS() +"' tz='"+ ZTimeZone.TimeZoneEST.getID() +"'/>"
				+				"<e d='"+ endUTC.toTimeZone(ZTimeZone.TimeZoneEST.getID()).toYYYYMMDDTHHMMSS() +"' tz='"+ ZTimeZone.TimeZoneEST.getID() +"'/>"
				+				"<or a='" + organizerEmailAddress + "'/>"
				+				"<at role='REQ' ptst='NE' rsvp='1' a='" + apptAttendee1EmailAddress + "'/>"
				+				"<at role='REQ' ptst='NE' rsvp='1' a='" + apptAttendee2EmailAddress + "'/>"
				+			"</inv>"
				+			"<e a='"+ apptAttendee1EmailAddress +"' t='t'/>"
				+			"<e a='"+ apptAttendee2EmailAddress +"' t='t'/>"
				+			"<su>"+ apptSubject +"</su>"
				+			"<mp content-type='text/plain'>"
				+				"<content>" + apptBody + "</content>"
				+			"</mp>"
				+		"</m>"
				+	"</CreateAppointmentRequest>");        
		app.zPageMail.zToolbarPressButton(Button.B_REFRESH);
		
		// --------------- Login to attendee & propose new time ----------------------------------------------------

		// Modify body content and propose new time
		FormApptNew apptForm = (FormApptNew)app.zPageCalendar.zListItem(Action.A_RIGHTCLICK, Button.O_PROPOSE_NEW_TIME_MENU, apptSubject);
		appt.setStartTime(modifiedStartUTC);
		appt.setEndTime(modifiedEndUTC);
		appt.setContent(modifiedBody);
		apptForm.zFill(appt);
		apptForm.zSubmit();
		
		// ------ Organizer ------
		
		// Original invite body shouldn't be changed for organizer
		organizer.soapSend(
				"<SearchRequest xmlns='urn:zimbraMail' types='appointment' calExpandInstStart='"+ startUTC.addDays(-10).toMillis() +"' calExpandInstEnd='"+ endUTC.addDays(10).toMillis() +"'>"
			+		"<query>" + "subject:(" + apptSubject + ")" + " " + "content:(" + apptBody +")" + "</query>"
			+	"</SearchRequest>");
	
		String organizerInvId = organizer.soapSelectValue("//mail:appt", "invId");
		ZAssert.assertNotNull(organizerInvId, "Original invite body shouldn't be changed for apptAttendee1");
		
		// Verify organizer gets email notification using modified date & content
		String inboxId = FolderItem.importFromSOAP(organizer, FolderItem.SystemFolder.Inbox).getId();
		organizer.soapSend(
					"<SearchRequest xmlns='urn:zimbraMail' types='message'>"
				+       "<query>inid:"+ inboxId +" subject:(" + apptSubject + ")" + " " + "content:(" + modifiedBody +")" + "</query>"
				+	"</SearchRequest>");
		String messageId = organizer.soapSelectValue("//mail:m", "id");
		ZAssert.assertNotNull(messageId, "Verify organizer gets email notification using modified date & content");
		
		// ------ Attendee1 ------
		
		// Verify that the attendee status still shows as 'NEEDS ACTION'
		apptAttendee1.soapSend(
					"<SearchRequest xmlns='urn:zimbraMail' types='appointment' calExpandInstStart='"+ startUTC.addDays(-10).toMillis() +"' calExpandInstEnd='"+ endUTC.addDays(10).toMillis() +"'>"
				+		"<query>" + "subject:(" + apptSubject + ")" + " " + "content:(" + apptBody +")" + "</query>"
				+	"</SearchRequest>");
		
		String  apptAttendee1InvId= apptAttendee1.soapSelectValue("//mail:appt", "invId");
		ZAssert.assertNotNull(apptAttendee1InvId, "Original invite body shouldn't be changed for attendee");
		
		apptAttendee1.soapSend(
				"<GetAppointmentRequest  xmlns='urn:zimbraMail' id='"+ apptAttendee1InvId +"'/>");
		String attendee1Status = apptAttendee1.soapSelectValue("//mail:at[@a='"+ app.zGetActiveAccount().EmailAddress +"']", "ptst");
		ZAssert.assertEquals(attendee1Status, "NE", "Verify that the attendee status still shows as 'NEEDS ACTION'");
	
		
		// ------ Attendee2 ------
		
		// Attendee2 shouldn't receive any mail if Attendee1 proposes new time to Organizer
		apptAttendee2.soapSend(
				"<SearchRequest xmlns='urn:zimbraMail' types='message'>"
			+		"<query>" + "subject:(" + apptSubject + ")" + "</query>"
			+	"</SearchRequest>");
	
		String proposeNewTimeMsg = apptAttendee2.soapSelectValue("//mail:appt", "invId");
		ZAssert.assertNull(proposeNewTimeMsg, "Attendee2 shouldn't receive any mail if Attendee1 proposes new time to Organizer");
		

		// Logout to attendee and login as organizer to decline proposed new time
		app.zPageMain.zLogout();
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyPrefsRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ organizer.ZimbraId +"</id>"
			+		"<a n='zimbraPrefGroupMailBy'>conversation</a>"
			+	"</ModifyPrefsRequest>");
		app.zPageLogin.zLogin(organizer);
		
		DisplayMail display = (DisplayMail)app.zPageMail.zListItem(Action.A_LEFTCLICK, apptSubject);
		display.zPressButton(Button.B_DECLINE_PROPOSE_NEW_TIME);
		FormMailNew mailComposeForm = new FormMailNew(app);
		mailComposeForm.zSubmit();
		SleepUtil.sleepLong();
		
		// ------ Organizer ------
		
		// Verify that the attendee1 status showing as 'NEEDS ACTION' for organizer
		organizer.soapSend(
					"<SearchRequest xmlns='urn:zimbraMail' types='appointment' calExpandInstStart='"+ startUTC.addDays(-10).toMillis() +"' calExpandInstEnd='"+ endUTC.addDays(10).toMillis() +"'>"
				+		"<query>" + "subject:(" + apptSubject + ")" + " " + "content:(" + apptBody +")" + "</query>"
				+	"</SearchRequest>");
		organizerInvId = organizer.soapSelectValue("//mail:appt", "invId");
		organizer.soapSend("<GetAppointmentRequest  xmlns='urn:zimbraMail' id='"+ organizerInvId +"'/>");
		String attendeeStatus = organizer.soapSelectValue("//mail:at[@a='"+ apptAttendee1EmailAddress +"']", "ptst");
		ZAssert.assertEquals(attendeeStatus, "NE", "Verify that the attendee shows as 'NEEDS ACTION' for organizer");
		
		
		// ------ Attendee1 ------
		
		// Verify that the attendee1 status showing as 'NEEDS ACTION' for attendee and reveive declined propose new time message
		apptAttendee1.soapSend(
					"<SearchRequest xmlns='urn:zimbraMail' types='appointment' calExpandInstStart='"+ startUTC.addDays(-10).toMillis() +"' calExpandInstEnd='"+ endUTC.addDays(10).toMillis() +"'>"
				+		"<query>" + "subject:(" + apptSubject + ")" + " " + "content:(" + apptBody +")" + "</query>"
				+	"</SearchRequest>");
		String attendeeInvId = apptAttendee1.soapSelectValue("//mail:appt", "invId");
		apptAttendee1.soapSend(
					"<GetAppointmentRequest  xmlns='urn:zimbraMail' id='"+ attendeeInvId +"'/>");
		String myStatus = apptAttendee1.soapSelectValue("//mail:at[@a='"+ apptAttendee1EmailAddress +"']", "ptst");
		ZAssert.assertEquals(myStatus, "NE", "Verify that the attendee shows as 'NEEDS ACTION' for organizer");
		
		apptAttendee1.soapSend(
				"<SearchRequest xmlns='urn:zimbraMail' types='message'>"
				+		"<query>" + "subject:(" + "Decline New Time Proposed.*" + ")" + " " + "content:(" + modifiedBody +")" + "</query>"
			+	"</SearchRequest>");
		messageId = apptAttendee1.soapSelectValue("//mail:m", "id");

		apptAttendee1.soapSend(
				"<GetMsgRequest  xmlns='urn:zimbraMail'>"
			+		"<m id='"+ messageId +"'/>"
			+	"</GetMsgRequest>");
		ZAssert.assertNotNull(messageId, "Attendee should get message from the organizer for declining proposed new time");
		
		
		// ------ Attendee2 ------
		
		// Verify that the attendee2 status still showing as 'NEEDS ACTION' and doesn't reveive propose new time message
		apptAttendee2.soapSend(
					"<SearchRequest xmlns='urn:zimbraMail' types='appointment' calExpandInstStart='"+ startUTC.addDays(-10).toMillis() +"' calExpandInstEnd='"+ endUTC.addDays(10).toMillis() +"'>"
				+		"<query>" + "subject:(" + apptSubject + ")" + " " + "content:(" + apptBody +")" + "</query>"
				+	"</SearchRequest>");
		attendeeInvId = apptAttendee2.soapSelectValue("//mail:appt", "invId");
		apptAttendee2.soapSend(
					"<GetAppointmentRequest  xmlns='urn:zimbraMail' id='"+ attendeeInvId +"'/>");
		myStatus = apptAttendee2.soapSelectValue("//mail:at[@a='"+ apptAttendee2EmailAddress +"']", "ptst");
		ZAssert.assertEquals(myStatus, "NE", "Verify that the attendee2 status still showing as 'NEEDS ACTION'");
		
		apptAttendee2.soapSend(
				"<SearchRequest xmlns='urn:zimbraMail' types='message'>"
			+		"<query>" + "subject:(" + "New Time Proposed.*" + ")" + " " + "content:(" + apptBody +")" + "</query>"
			+	"</SearchRequest>");
		messageId = apptAttendee2.soapSelectValue("//mail:m", "id");

		apptAttendee2.soapSend(
				"<GetMsgRequest  xmlns='urn:zimbraMail'>"
			+		"<m id='"+ messageId +"'/>"
			+	"</GetMsgRequest>");
		ZAssert.assertNull(messageId, "Attendee2 shouldn't get message from the organizer for declining proposed new time");

	}
	
}
