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

/**
* Create a new, empty appt list.
* @constructor
* @class
* This class represents a list of appts.
*
*/
function ZmRosterItemList(appCtxt) {
	ZmList.call(this, ZmItem.ROSTER_ITEM, appCtxt);
	this._addrHash = {};
}

ZmRosterItemList.prototype = new ZmList;
ZmRosterItemList.prototype.constructor = ZmRosterItemList;

ZmRosterItemList.prototype.toString = 
function() {
	return "ZmRosterItemList";
}

ZmRosterItemList.prototype.addItem =
function(item, skipNotify) {
    this.add(item);
    this._addrHash[item.addr] = item;
    if (!skipNotify) {
        this._eventNotify(ZmEvent.E_CREATE, [item]);
    }
};

ZmRosterItemList.prototype.removeItem = 
function(item, skipNotify) {
    this.remove(item);
    delete this._addrHash[item.addr];
    if (!skipNotify) {
        this._eventNotify(ZmEvent.E_REMOVE, [item]);
    }    
};

ZmRosterItemList.prototype.getByAddr =
function(addr) {
    return this._addrHash[addr];
};

ZmRosterItemList.prototype.getById =
function(id) {
    return this.getById(id);
};

ZmRosterItemList.prototype.removeAllItems =
function() {
	var listArray = this.getArray();
	for (var i=0; i < listArray.length; i++) {
	    this.removeItem(listArray[i]);
	}
};

ZmRosterItemList.prototype.loadFromJs =
function(obj) {
	if (obj && obj.item && obj.item.length) {
		for (var i = 0; i < obj.item.length; i++) {
		    var item = obj.item[i];
            //if (item.group == null) item.group = ZmMsg.buddies;
            var item = new ZmRosterItem(item.id, this, this._appCtxt, item.addr, item.name, item.show, item.status, item.group);
            this.addItem(item);
        	}
	}
};

ZmRosterItemList.prototype.reload =
function() {
    this.removeAllItems();
	this.loadFromJs({ 
	    item: [
            {id: "r0", addr: "dkarp@zimbra.com", name: "Dan", show: ZmRosterItem.SHOW_ONLINE, group: "Friends"},
            {id: "r1", addr: "ross@zimbra.com", name: "Ross", show: ZmRosterItem.SHOW_DND, group: "Work"},
            {id: "r2", addr: "satish@zimbra.com", name: "Satish", show: ZmRosterItem.SHOW_AWAY, status:"out to lunch", group: "Work"},
            {id: "r3", addr: "tim@zimbra.com", name: "Tim", show: ZmRosterItem.SHOW_OFFLINE, group: "Work"},
            {id: "r4", addr: "anand@zimbra.com", name: "Anand", show: ZmRosterItem.SHOW_EXT_AWAY, group: "Friends,Work"},
            {id: "r5", addr: "andy@zibra.com", name: "Andy", show: ZmRosterItem.SHOW_CHAT, group: "Work"},
            {id: "r6", addr: "matt@gmail.com", show:ZmRosterItem.SHOW_ONLINE, group: "Family"}
	    ]
    });
};
