/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

function ZmCalendar(id, name, parent, tree, color, link) {
	ZmOrganizer.call(this, ZmOrganizer.CALENDAR, id, name, parent, tree);
	this.color = color || ZmOrganizer.DEFAULT_COLOR;
	this.link = link;
}

ZmCalendar.prototype = new ZmOrganizer;
ZmCalendar.prototype.constructor = ZmCalendar;

ZmCalendar.prototype.toString = 
function() {
	return "ZmCalendar";
}

// Constants

ZmCalendar.ID_CALENDAR = ZmOrganizer.ID_CALENDAR;

// Static methods

/** Caller is responsible to catch exception. */
ZmCalendar.create =
function(appCtxt, name, parentFolderId) {
	parentFolderId = parentFolderId || ZmOrganizer.ID_ROOT;

	var soapDoc = AjxSoapDoc.create("CreateFolderRequest", "urn:zimbraMail");
	var folderNode = soapDoc.set("folder");
	folderNode.setAttribute("name", name);
	folderNode.setAttribute("l", parentFolderId);
	folderNode.setAttribute("view", ZmOrganizer.VIEWS[ZmOrganizer.CALENDAR]);

	var appController = appCtxt.getAppController();
	return appController.sendRequest(soapDoc, false);
}

ZmCalendar.createFromJs =
function(parent, obj, tree, link) {
	if (!(obj && obj.id)) return;

	// create calendar, populate, and return
	var calendar = new ZmCalendar(obj.id, obj.name, parent, tree, obj.color, link);
	if (obj.folder && obj.folder.length) {
		for (var i = 0; i < obj.folder.length; i++) {
			var folder = obj.folder[i];
			if (folder.view == ZmOrganizer.VIEWS[ZmOrganizer.CALENDAR]) {
				var childCalendar = ZmCalendar.createFromJs(calendar, folder, tree, false);
				calendar.children.add(childCalendar);
			}
		}
	}
	if (obj.link && obj.link.length) {
		for (var i = 0; i < obj.link.length; i++) {
			var link = obj.link[i];
			if (link.view == ZmOrganizer.VIEWS[ZmOrganizer.CALENDAR]) {
				var childCalendar = ZmCalendar.createFromJs(calendar, link, tree, true);
				calendar.children.add(childCalendar);
			}
		}
	}
	
	// set shares
	calendar._setSharesFromJs(obj);
	
	return calendar;
}

ZmCalendar.sortCompare = 
function(calA, calB) {
	// links appear after personal calendars
	if (calA.link != calB.link) {
		return calA.link ? 1 : -1;
	}
	
	// sort by calendar name
	var calAName = calA.name.toLowerCase();
	var calBName = calB.name.toLowerCase();
	if (calAName < calBName) return -1;
	if (calAName > calBName) return 1;
	return 0;
}

ZmCalendar.checkName =
function(name) {
	return ZmOrganizer.checkName(name);
}

// Public methods

ZmCalendar.prototype.notifyCreate =
function(obj, link) {
	var calendar = ZmCalendar.createFromJs(this, obj, this.tree, link);
	var index = ZmOrganizer.getSortIndex(calendar, ZmCalendar.sortCompare);
	this.children.add(calendar, index);
	this._eventNotify(ZmEvent.E_CREATE, calendar);
}

ZmCalendar.prototype.getName = 
function() {
	if (this.id == ZmOrganizer.ID_ROOT) {
		return ZmMsg.calendars;
	} 
	return this.name;
}

ZmCalendar.prototype.getIcon = 
function() {
	if (this.id == ZmOrganizer.ID_ROOT) {
		return null;
	}
	return this.link ? "GroupSchedule" : "CalendarFolder";
}
