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
 * The Original Code is: Zimbra Collaboration Suite Web Client
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
* Creates an empty organizer.
* @constructor
* @class
* This class represents an "organizer", which is something used to classify or contain
* items. So far, that's either a tag or a folder. Tags and folders are represented as
* a tree structure, though tags are flat and have only one level below the root. Folders
* can be nested.
*
* @author Conrad Damon
*
* @param type		[constant]		folder or tag
* @param id			[int]			numeric ID
* @param name		[string]		name
* @param parent		[ZmOrganizer]	parent organizer
* @param tree		[ZmTree]		tree model that contains this organizer
* @param numUnread	[int]*			number of unread items for this organizer
* @param numTotal	[int]*			number of items for this organizer
* @param url		[string]*		URL for this organizer's feed
* @param owner		[string]* 		Owner for this organizer
*/
function ZmOrganizer(type, id, name, parent, tree, numUnread, numTotal, url, owner) {

	if (arguments.length == 0) return;
	
	this.type = type;
	this.id = id;
	this.name = name;
	this.parent = parent;
	this.tree = tree;
	this.numUnread = numUnread || 0;
	this.numTotal = numTotal || 0;
	this.url = url;
	this.owner = owner;

	if (id && tree)
		tree._appCtxt.cacheSet(id, this);

	this.children = new AjxVector();
};

// organizer types
ZmOrganizer.FOLDER				= ZmEvent.S_FOLDER;
ZmOrganizer.TAG					= ZmEvent.S_TAG;
ZmOrganizer.SEARCH				= ZmEvent.S_SEARCH;
ZmOrganizer.CALENDAR			= ZmEvent.S_APPT;
ZmOrganizer.ADDRBOOK 			= ZmEvent.S_CONTACT;
ZmOrganizer.ROSTER_TREE_ITEM	= ZmEvent.S_ROSTER_TREE_ITEM;
ZmOrganizer.ROSTER_TREE_GROUP	= ZmEvent.S_ROSTER_TREE_GROUP;
ZmOrganizer.ZIMLET				= ZmEvent.S_ZIMLET;
ZmOrganizer.NOTEBOOK			= ZmEvent.S_NOTEBOOK;

// defined in com.zimbra.cs.mailbox.Mailbox
ZmOrganizer.ID_ROOT				= 1;
ZmOrganizer.ID_TRASH			= 3;
ZmOrganizer.ID_SPAM				= 4;
ZmOrganizer.ID_ADDRBOOK			= 7;
ZmOrganizer.ID_CALENDAR			= 10;
ZmOrganizer.ID_ZIMLET			= -1000;  // zimlets need a range.  start from -1000 incrementing up.
ZmOrganizer.ID_ROSTER_LIST		= -11;
ZmOrganizer.ID_ROSTER_TREE_ITEM	= -13;

ZmOrganizer.SOAP_CMD = {};
ZmOrganizer.SOAP_CMD[ZmOrganizer.FOLDER]	= "FolderAction";
ZmOrganizer.SOAP_CMD[ZmOrganizer.TAG]		= "TagAction";
ZmOrganizer.SOAP_CMD[ZmOrganizer.SEARCH]	= "FolderAction";
ZmOrganizer.SOAP_CMD[ZmOrganizer.CALENDAR]	= "FolderAction";
ZmOrganizer.SOAP_CMD[ZmOrganizer.ADDRBOOK]	= "FolderAction";
ZmOrganizer.SOAP_CMD[ZmOrganizer.NOTEBOOK]	= "FolderAction";

ZmOrganizer.FIRST_USER_ID = {};
ZmOrganizer.FIRST_USER_ID[ZmOrganizer.FOLDER]	= 256;
ZmOrganizer.FIRST_USER_ID[ZmOrganizer.TAG]		= 64;
ZmOrganizer.FIRST_USER_ID[ZmOrganizer.SEARCH]	= 256;
ZmOrganizer.FIRST_USER_ID[ZmOrganizer.CALENDAR]	= 256;
ZmOrganizer.FIRST_USER_ID[ZmOrganizer.ADDRBOOK] = 256;
ZmOrganizer.FIRST_USER_ID[ZmOrganizer.NOTEBOOK] = 256;

// fields that can be part of a displayed organizer
var i = 1;
ZmOrganizer.F_NAME		= i++;
ZmOrganizer.F_UNREAD	= i++;
ZmOrganizer.F_TOTAL		= i++;
ZmOrganizer.F_PARENT	= i++;
ZmOrganizer.F_COLOR		= i++;
ZmOrganizer.F_QUERY		= i++;
ZmOrganizer.F_SHARES	= i++;

// Following chars invalid in organizer names: " : / [anything less than " "]
ZmOrganizer.VALID_NAME_CHARS = "[^\\x00-\\x1F\\x7F:\\/\\\"]";
ZmOrganizer.VALID_PATH_CHARS = "[^\\x00-\\x1F\\x7F:\\\"]"; // forward slash is OK in path
ZmOrganizer.VALID_NAME_RE = new RegExp('^' + ZmOrganizer.VALID_NAME_CHARS + '+$');

ZmOrganizer.MAX_NAME_LENGTH			= 128;	// max allowed by server
ZmOrganizer.MAX_DISPLAY_NAME_LENGTH	= 30;	// max we will show

// colors - these are the server values
ZmOrganizer.C_ORANGE	= 0;
ZmOrganizer.C_BLUE		= 1;
ZmOrganizer.C_CYAN		= 2;
ZmOrganizer.C_GREEN		= 3;
ZmOrganizer.C_PURPLE	= 4;
ZmOrganizer.C_RED		= 5;
ZmOrganizer.C_YELLOW	= 6;
ZmOrganizer.C_PINK		= 7;
ZmOrganizer.C_GRAY		= 8;
ZmOrganizer.MAX_COLOR	= ZmOrganizer.C_GRAY;
ZmOrganizer.DEFAULT_COLOR = ZmOrganizer.C_ORANGE;

// color names
ZmOrganizer.COLOR_TEXT = new Object();
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_ORANGE]	= ZmMsg.orange;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_BLUE]		= ZmMsg.blue;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_CYAN]		= ZmMsg.cyan;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_GREEN]		= ZmMsg.green;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_PURPLE]	= ZmMsg.purple;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_RED]		= ZmMsg.red;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_YELLOW]	= ZmMsg.yellow;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_PINK]		= ZmMsg.pink;
ZmOrganizer.COLOR_TEXT[ZmOrganizer.C_GRAY]		= ZmMsg.gray;

ZmOrganizer.COLORS = [];
ZmOrganizer.COLOR_CHOICES = [];
for (var i = 0; i <= ZmOrganizer.MAX_COLOR; i++) {
	var color = ZmOrganizer.COLOR_TEXT[i];
	ZmOrganizer.COLORS.push(color);
	ZmOrganizer.COLOR_CHOICES.push( { value: i, label: color } );
}

// views
ZmOrganizer.VIEWS = new Object;
ZmOrganizer.VIEWS[ZmOrganizer.FOLDER] = "conversation";
ZmOrganizer.VIEWS[ZmOrganizer.CALENDAR] = "appointment";
ZmOrganizer.VIEWS[ZmOrganizer.ADDRBOOK] = "contact";
ZmOrganizer.VIEWS[ZmOrganizer.NOTEBOOK] = "wiki";

// Abstract methods

ZmOrganizer.sortCompare = function(organizerA, organizerB) {};
ZmOrganizer.prototype.create = function() {};

// Static methods

ZmOrganizer.getViewName =
function(organizerType) {
	return ZmOrganizer.VIEWS[organizerType];
};

/**
* Checks an organizer (folder or tag) name for validity. Returns an error message if the
* name is invalid and null if the name is valid. Note that a name, rather than a path, is
* checked.
*
* @param name		an organizer name
*/
ZmOrganizer.checkName =
function(name) {
	if (name.length == 0)
		return ZmMsg.nameEmpty;

	if (name.length > ZmOrganizer.MAX_NAME_LENGTH)
		return AjxMessageFormat.format(ZmMsg.nameTooLong, ZmOrganizer.MAX_NAME_LENGTH);

	if (!ZmOrganizer.VALID_NAME_RE.test(name))
		return AjxMessageFormat.format(ZmMsg.errorInvalidName, name);

	return null;
};

ZmOrganizer.checkSortArgs =
function(orgA, orgB) {
	if (!orgA && !orgB) return 0;
	if (orgA && !orgB) return 1;
	if (!orgA && orgB) return -1;
	return null;
};

ZmOrganizer.checkColor =
function(color) {
	return ((color != null) && (color >= 0 && color <= ZmOrganizer.MAX_COLOR)) ? color : ZmOrganizer.DEFAULT_COLOR;
};

// Public methods

ZmOrganizer.prototype.toString = 
function() {
	return "ZmOrganizer";
};

/**
* Returns the name of this organizer.
*
* @param showUnread		whether to display the number of unread items (in parens)
* @param maxLength		length in chars to truncate the name to
* @param noMarkup		if true, don't return any HTML
*/
ZmOrganizer.prototype.getName = 
function(showUnread, maxLength, noMarkup) {
	var name = (maxLength && this.name.length > maxLength) ? this.name.substring(0, maxLength - 3) + "..." : this.name;
	if (!noMarkup)
		name = AjxStringUtil.htmlEncode(name, true);
	if (showUnread && this.numUnread > 0) {
		name = [name, " (", this.numUnread, ")"].join("");
		if (!noMarkup)
			name = ["<b>", name, "</b>"].join("");
	}
	return name;
};

ZmOrganizer.prototype.getShares =
function() {
	return this.shares;
};

ZmOrganizer.prototype.setShares = 
function(shares) {
	this.shares = shares;
};

ZmOrganizer.prototype.getShareByGranteeId = 
function(granteeId) {
	if (this.shares) {
		for (var i = 0; i < this.shares.length; i++) {
			var share = this.shares[i];
			if (share.grantee.id == granteeId) {
				return share;
			}
		}
	}
	return null;
};

ZmOrganizer.prototype.addShare = 
function(share) {
	if (!this.shares) {
		this.shares = [];
	}
	this.shares.push(share);
};

ZmOrganizer.prototype.getIcon = function() {};

// Actions

/**
* Assigns the organizer a new name.
*/
ZmOrganizer.prototype.rename =
function(name) {
	if (name == this.name) return;
	this._organizerAction({action: "rename", attrs: {name: name}});
};

ZmOrganizer.prototype.setColor =
function(color) {
	var color = ZmOrganizer.checkColor(color);
	if (this.color == color) return;
	this._organizerAction({action: "color", attrs: {color: color}});
};

/**
* Assigns the organizer a new parent, moving it within its tree.
*
* @param newParent		the new parent of this organizer
*/
ZmOrganizer.prototype.move =
function(newParent) {
	var newId = (newParent.id > 0) ? newParent.id : ZmOrganizer.ID_ROOT;
	if ((newId == this.id || newId == this.parent.id) ||
		(this.type == ZmOrganizer.FOLDER && newId == ZmFolder.ID_SPAM) ||
		(newParent.isChildOf(this))) {
		return;
	}

	this._organizerAction({action: "move", attrs: {l: newId}});
};

/**
* Deletes an organizer. If it's a folder, the server deletes any contents and/or
* subfolders. If it's Trash or Spam, the server deletes and re-creates the folder.
* In that case, we don't bother to remove it from the UI (and we ignore creates on
* system folders).
*/
ZmOrganizer.prototype._delete =
function() {
	DBG.println(AjxDebug.DBG1, "deleting: " + this.name + ", ID: " + this.id);
	var isEmptyOp = (this.type == ZmOrganizer.FOLDER && (this.id == ZmFolder.ID_SPAM || this.id == ZmFolder.ID_TRASH));
	// make sure we're not deleting a system object (unless we're emptying SPAM or TRASH)
	if (this.isSystem() && !isEmptyOp) return;
	
	var action = isEmptyOp ? "empty" : "delete";
	this._organizerAction({action: action});
};

ZmOrganizer.prototype.markAllRead =
function() {
	this._organizerAction({action: "read", attrs: {l: this.id}});
};

ZmOrganizer.prototype.sync =
function() {
	this._organizerAction({action: "sync"});
};

// Notification handling

ZmOrganizer.prototype.notifyDelete =
function() {
	this.deleteLocal();
	this._notify(ZmEvent.E_DELETE);
};

ZmOrganizer.prototype.notifyCreate = function() {};

/*
* Handle modifications to fields that organizers have in general. Note that
* the notification object may contain multiple notifications.
*
* @param obj	[Object]	a "modified" notification
*/
ZmOrganizer.prototype.notifyModify =
function(obj) {
	var doNotify = false;
	var details = {};
	var fields = {};
	if (obj.name != null && this.name != obj.name) {
		details.oldName = this.name;
		this.name = obj.name;
		fields[ZmOrganizer.F_NAME] = true;
		this.parent.children.sort(ZmTreeView.COMPARE_FUNC[this.type]);
		doNotify = true;
	}
	if (obj.u != null && this.numUnread != obj.u) {
		this.numUnread = obj.u;
		fields[ZmOrganizer.F_UNREAD] = true;
		doNotify = true;
	}
	if (obj.n != null && this.numTotal != obj.n) {
		this.numTotal = obj.n;
		fields[ZmOrganizer.F_TOTAL] = true;
		doNotify = true;
	}
	if (obj.color != null) {
		var color = ZmOrganizer.checkColor(obj.color);
		if (this.color != color) {
			this.color = color;
			fields[ZmOrganizer.F_COLOR] = true;
		}
		doNotify = true;
	}
	if (obj.acl && obj.acl.grant) {
		for (var i = 0; i < obj.acl.grant.length; i++) {
			var grant = obj.acl.grant[i];
			var share = this.getShareByGranteeId(grant.zid);
			if (share) {
				share.link.perm = grant.perm;
			}
			else {
				share = ZmOrganizerShare.createFromJs(this, grant);
				this.addShare(share, true);
			}
		}
		fields[ZmOrganizer.F_SHARES] = true;
		doNotify = true;
	}
	// Send out composite MODIFY change event
	if (doNotify) {
		details.fields = fields;
		this._notify(ZmEvent.E_MODIFY, details);
	}

	if (obj.l != null && obj.l != this.parent.id) {
		var newParent = this._getNewParent(obj.l);
		this.reparent(newParent);
		this._notify(ZmEvent.E_MOVE);
		// could be moving search between Folders and Searches - make sure
		// it has the correct tree
		this.tree = newParent.tree; 
	}
};

// Local change handling

ZmOrganizer.prototype.deleteLocal =
function() {
	this.children.removeAll();
	this.parent.children.remove(this);
};

/**
* Returns true if this organizer has a child with the given name.
*
* @param name		the name of the organizer to look for
*/
ZmOrganizer.prototype.hasChild =
function(name) {
	name = name.toLowerCase();
	var a = this.children.getArray();
	var sz = this.children.size();
	for (var i = 0; i < sz; i++)
		if (a[i].name && (a[i].name.toLowerCase() == name))
			return true;

	return false;
};

ZmOrganizer.prototype.reparent =
function(newParent) {
	this.parent.children.remove(this);
	newParent.children.add(this);
	this.parent = newParent;
};

/**
* Returns the organizer with the given ID, wherever it is.
*
* @param id		the ID to search for
*/
ZmOrganizer.prototype.getById =
function(id) {
	if (this.id == id)
		return this;
	
	var organizer;
	var a = this.children.getArray();
	var sz = this.children.size();
	for (var i = 0; i < sz; i++) {
		if (organizer = a[i].getById(id))
			return organizer;
	}
	return null;	
};

/**
* Returns the first organizer found with the given name, starting from the root.
*
* @param name		the name to search for
*/
ZmOrganizer.prototype.getByName =
function(name) {
	return this._getByName(name.toLowerCase());
};

/**
* Returns the number of children of this organizer.
*/
ZmOrganizer.prototype.size =
function() {
	return this.children.size();
};

/**
* Returns true if the given organizer is a descendant of this one.
*
* @param organizer		a possible descendant of ours
*/
ZmOrganizer.prototype.isChildOf =
function (organizer) {
	var parent = this.parent;
	while (parent) {
		if (parent == organizer)
			return true;
		parent = parent.parent;
	}
	return false;
};

/*
* Returns the organizer with the given ID. Looks in this organizer's tree.
*
* @param parentId	[int]		ID of the organizer to find
*/
ZmOrganizer.prototype._getNewParent =
function(parentId) {
	return this.tree.getById(parentId);
};

/**
* Returns true is this is a system tag or folder.
*/
ZmOrganizer.prototype.isSystem =
function () {
	return (this.id < ZmOrganizer.FIRST_USER_ID[this.type]);
};

/**
* Returns true is this organizer gets its contents from an external feed.
*/
ZmOrganizer.prototype.isFeed =
function () {
	return (this.url != null);
};

ZmOrganizer.prototype.getOwner =
function() {
	return this.owner;
};

ZmOrganizer.getSortIndex =
function(child, sortFunction) {
	if (!sortFunction) return null;
	var children = child.parent.children.getArray();
	for (var i = 0; i < children.length; i++) {
		var test = sortFunction(child, children[i]);
		if (test == -1)
			return i;
	}
	return i;
};

/*
* Sends a request to the server. Note that it's done asynchronously, but
* there is no callback given. Hence, an organizer action is the last thing
* done before returning to the event loop. The result of the action is
* handled via notifications.
*
* @param action		[string]	operation to perform
* @param attrs		[Object]	hash of additional attributes to set in the request
*/
ZmOrganizer.prototype._organizerAction =
function(params) {
	var cmd = ZmOrganizer.SOAP_CMD[this.type];
	var soapDoc = AjxSoapDoc.create(cmd + "Request", "urn:zimbraMail");
	var actionNode = soapDoc.set("action");
	actionNode.setAttribute("op", params.action);
	actionNode.setAttribute("id", this.id);
	for (var attr in params.attrs)
		actionNode.setAttribute(attr, params.attrs[attr]);
	var execFrame = new AjxCallback(this, this._itemAction, params);
	this.tree._appCtxt.getAppController().sendRequest({soapDoc: soapDoc, asyncMode: true, execFrame: execFrame});
};

// Test the name of this organizer and then descendants against the given name, case insensitively
ZmOrganizer.prototype._getByName =
function(name) {
	if (this.name && name == this.name.toLowerCase())
		return this;
		
	var organizer;
	var a = this.children.getArray();
	var sz = this.children.size();
	for (var i = 0; i < sz; i++) {
		if (organizer = a[i]._getByName(name))
			return organizer;
	}
	return null;	
};

ZmOrganizer.prototype.addChangeListener =
function(listener) {
	this.tree.addChangeListener(listener);
};

ZmOrganizer.prototype.removeChangeListener =
function(listener) {
	this.tree.removeChangeListener(listener);
};

ZmOrganizer.prototype._setSharesFromJs =
function(obj) {
	if (obj.acl && obj.acl.grant && obj.acl.grant.length > 0) {
		var shares = new Array(obj.acl.grant.length);
		for (var i = 0; i < obj.acl.grant.length; i++) {
			var grant = obj.acl.grant[i];
			shares[i] = ZmOrganizerShare.createFromJs(this, grant);
		}
		this.setShares(shares);
	}
};

// Handle notifications through the tree
ZmOrganizer.prototype._notify =
function(event, details) {
	if (details)
		details.organizers = [this];
	else
		details = {organizers: [this]};
	this.tree._notify(event, details);
};
