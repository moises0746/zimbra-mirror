/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
ZmBriefcaseController = function(container, app) {
	if (arguments.length == 0) { return; }
	ZmListController.call(this, container, app);

	//this._foldersMap = {};
	this._idMap = {};

	this._listChangeListener = new AjxListener(this,this._fileListChangeListener);
        this._listeners[ZmOperation.OPEN_FILE] = new AjxListener(this, this._openFileListener);
        this._listeners[ZmOperation.SAVE_FILE] = new AjxListener(this, this._saveFileListener);
   	this._listeners[ZmOperation.SEND_FILE] = new AjxListener(this, this._sendFileListener);
   	this._listeners[ZmOperation.SEND_FILE_AS_ATT] = new AjxListener(this, this._sendFileAsAttachmentListener);
  	this._listeners[ZmOperation.NEW_FILE] = new AjxListener(this, this._uploadFileListener);
   	this._listeners[ZmOperation.VIEW_FILE_AS_HTML] = new AjxListener(this, this._viewAsHtmlListener);
	this._dragSrc = new DwtDragSource(Dwt.DND_DROP_MOVE);
	this._dragSrc.addDragListener(new AjxListener(this, this._dragListener));

	this._parentView = {};
}
ZmBriefcaseController.prototype = new ZmListController;
ZmBriefcaseController.prototype.constructor = ZmBriefcaseController;

ZmBriefcaseController.prototype.toString =
function() {
	return "ZmBriefcaseController";
};

// Constants
ZmBriefcaseController._VIEWS = {};
ZmBriefcaseController._VIEWS[ZmId.VIEW_BRIEFCASE] = ZmBriefcaseView;
ZmBriefcaseController._VIEWS[ZmId.VIEW_BRIEFCASE_DETAIL] = ZmDetailListView;
ZmBriefcaseController._VIEWS[ZmId.VIEW_BRIEFCASE_COLUMN] = ZmMultiColView;

// Stuff for the View menu
ZmBriefcaseController.GROUP_BY_ICON = {};
ZmBriefcaseController.GROUP_BY_MSG_KEY = {};
ZmBriefcaseController.GROUP_BY_VIEWS = [];

ZmBriefcaseController.GROUP_BY_MSG_KEY[ZmId.VIEW_BRIEFCASE]			= "explorerView";
ZmBriefcaseController.GROUP_BY_MSG_KEY[ZmId.VIEW_BRIEFCASE_DETAIL]	= "detailView";
ZmBriefcaseController.GROUP_BY_MSG_KEY[ZmId.VIEW_BRIEFCASE_COLUMN]	= "columnBrowserView";

ZmBriefcaseController.GROUP_BY_ICON[ZmId.VIEW_BRIEFCASE]			= "Folder";
ZmBriefcaseController.GROUP_BY_ICON[ZmId.VIEW_BRIEFCASE_DETAIL]		= "ListView";
ZmBriefcaseController.GROUP_BY_ICON[ZmId.VIEW_BRIEFCASE_COLUMN]		= "ListView";

ZmBriefcaseController.GROUP_BY_VIEWS.push(ZmId.VIEW_BRIEFCASE);
ZmBriefcaseController.GROUP_BY_VIEWS.push(ZmId.VIEW_BRIEFCASE_DETAIL);
ZmBriefcaseController.GROUP_BY_VIEWS.push(ZmId.VIEW_BRIEFCASE_COLUMN);

// Overrides ZmListController method, leaving ZmOperation.MOVE off the menu.
ZmBriefcaseController.prototype._standardActionMenuOps =
function() {
	return [ZmOperation.TAG_MENU, ZmOperation.DELETE, ZmOperation.MOVE];
};

ZmBriefcaseController.prototype._getToolBarOps =
function() {
	return [ZmOperation.NEW_MENU,
			ZmOperation.SEP,
			ZmOperation.NEW_FILE,
			ZmOperation.SEP,
			ZmOperation.DELETE, ZmOperation.MOVE,
			ZmOperation.SEP,
			ZmOperation.TAG_MENU,
			ZmOperation.SEP,
			ZmOperation.VIEW_MENU,
			ZmOperation.FILLER,
			ZmOperation.SEND_FILE_MENU];
};

ZmBriefcaseController.prototype._initializeToolBar =
function(view) {
	if (!this._toolbar[view]) {
		ZmListController.prototype._initializeToolBar.call(this, view);
		this._setupViewMenu(view, true);
		this._setNewButtonProps(view, ZmMsg.uploadNewFile, "NewPage", "NewPageDis", ZmOperation.NEW_FILE);

		var toolbar = this._toolbar[this._currentView];
		var button = toolbar.getButton(ZmOperation.REFRESH);
		if (button) {
			button.setImage("Refresh");
		}

		button = toolbar.getButton(ZmOperation.DELETE);
		button.setToolTipContent(ZmMsg.deletePermanentTooltip);

        this._initSendMenu(view);        
	}

	this._setupViewMenu(view, false);
    
};

ZmBriefcaseController.prototype._resetOperations =
function(parent, num) {
	if (!parent) return;

	ZmListController.prototype._resetOperations.call(this, parent, num);
	var isFolderSelected =false;
	var items = this._listView[this._currentView].getSelection();
	var noOfFolders = 0;
	if(items){
		for(var i=0;i<items.length;i++){
			var item = items[i];
			if(item.isFolder){
				isFolderSelected = true;
				noOfFolders++;
			}
		}
	}
	var multiFolderSelect = (noOfFolders>1);


	var isShared = this.isShared(this._currentFolder);
	var isReadOnly = this.isReadOnly(this._currentFolder);
	var isItemSelected = (num>0);

	if (appCtxt.get(ZmSetting.VIEW_ATTACHMENT_AS_HTML)) {
		var isViewHtmlEnabled = true;

		var items = this._listView[this._currentView].getSelection();
		if (items) {
			for (var i=0; i<items.length; i++) {
				var item = items[i];
				if (!this.isConvertable(item)) {
					isViewHtmlEnabled = false;
					break;
				}
			}
		}
		parent.enable([ZmOperation.VIEW_FILE_AS_HTML], isItemSelected && isViewHtmlEnabled );
	}
	parent.enable([ ZmOperation.OPEN_FILE, ZmOperation.SEND_FILE, ZmOperation.SEND_FILE_MENU], isItemSelected && !multiFolderSelect );
	parent.enable([ ZmOperation.DELETE ], !isReadOnly && isItemSelected );
	parent.enable([ ZmOperation.TAG_MENU ], !isShared && isItemSelected && !isFolderSelected);
	parent.enable([ ZmOperation.NEW_FILE, ZmOperation.VIEW_MENU ], true);
	parent.enable([ ZmOperation.SEND_FILE_MENU, ZmOperation.SEND_FILE, ZmOperation.SEND_FILE_AS_ATT ], isItemSelected && !isFolderSelected );
};

ZmBriefcaseController.prototype._getTagMenuMsg =
function() {
	return ZmMsg.tagFile;
};

ZmBriefcaseController.prototype._doDelete =
function(items,delcallback) {

	if (!items) {
		items = this._listView[this._currentView].getSelection();
	}

	var dialog = appCtxt.getConfirmationDialog();
	var message = items instanceof Array && items.length > 1 ? ZmMsg.confirmDeleteItemList : null;
	if (!message) {
		if (!this._confirmDeleteFormatter) {
			this._confirmDeleteFormatter = new AjxMessageFormat(ZmMsg.confirmDeleteItem);
		}

		var item = items instanceof Array ? items[0] : items;
		if(!item) return;
		message = this._confirmDeleteFormatter.format(item.name);
	}
	var callback = new AjxCallback(this, this._doDelete2, [items,delcallback]);
	dialog.popup(message, callback);
};

ZmBriefcaseController.prototype._doDelete2 =
function(items, delcallback) {
	var ids = ZmBriefcaseController.__itemize(items);
	if (!ids) return;

	var soapDoc = AjxSoapDoc.create("ItemActionRequest", "urn:zimbraMail");
	var actionNode = soapDoc.set("action");
	actionNode.setAttribute("id", ids);
	actionNode.setAttribute("op", "delete");

	var responseHandler = new AjxCallback(this,this.deleteCallback,[ids]);

	/*
	if(delcallback){
		responseHandler = delcallback;
	}*/

	var params = {
		soapDoc: soapDoc,
		asyncMode: true,
		callback: responseHandler,
		errorCallback: null,
		noBusyOverlay: false
	};

	var appController = appCtxt.getAppController();
	var response = appController.sendRequest(params);
	return response;
};

// view management

ZmBriefcaseController.prototype._getViewType =
function() {
	return this._currentView;
};

ZmBriefcaseController.prototype._defaultView =
function() {
	return ZmId.VIEW_BRIEFCASE_COLUMN;
};

ZmBriefcaseController.prototype._createNewView =
function(view) {
	if (!this._listView[view]) {
		var viewCtor = ZmBriefcaseController._VIEWS[view];
		if(view == ZmId.VIEW_BRIEFCASE_COLUMN) {
			this._parentView[view] = new viewCtor(this._container, null, null, this, this._dropTgt);
			var listView = this._listView[view] = this._parentView[view].getListView();
			listView.setDragSource(this._dragSrc);
			return listView;
		}else{
			this._listView[view] = new viewCtor(this._container, this, this._dropTgt);
		}
	}
	this._listView[view].setDragSource(this._dragSrc);
	return this._listView[view];
};

ZmBriefcaseController.prototype._setViewContents =
function(view) {
	if(view == ZmId.VIEW_BRIEFCASE_COLUMN){
		this._parentView[view].set(this._list);           //Set the list
	}else{
		this._listView[view].set(this._list);
	}
	// Select the appropriate notebook in the tree view.
	if (this._object) {
		var overviewController = appCtxt.getOverviewController();
		var treeController = overviewController.getTreeController(ZmOrganizer.BRIEFCASE);
		var treeView = treeController.getTreeView(this._app.getOverviewId());
		if (treeView) {
			var folderId = this._object;
			var skipNotify = true;
			treeView.setSelected(folderId, skipNotify);
		}
	}
	this._refreshInProgress = false;
};

ZmBriefcaseController.prototype.isRefreshing =
function() {
	return this._refreshInProgress;
};

ZmBriefcaseController.prototype._refreshListener =
function(event) {
	//TODO:refresh listener
};

ZmBriefcaseController.prototype._getDefaultFocusItem =
function() {
	return this._toolbar[this._currentView];
};

ZmBriefcaseController.__itemize =
function(objects) {
	if (objects instanceof Array) {
		var ids = [];
		for (var i = 0; i < objects.length; i++) {
			var object = objects[i];
			if (object.id) {
				ids.push(object.id);
			}
		}
		return ids.join();
	}
	return objects.id;
};

// view management

ZmBriefcaseController.prototype.show =
function(folderId, force, fromSearch) {
	if (folderId == null) {
		folderId = ZmOrganizer.ID_BRIEFCASE;
	}

	DBG.println(AjxDebug.DBG2,"ZmBriefcaseController.show folder id:"+folderId);

	// save state
	this._fromSearch = fromSearch;

	var shownFolder = this._object;
	var currentFolder = folderId;
	this._object = currentFolder;
	this._currentFolder = currentFolder;
	this._forceSwitch = force;
	var callback = new AjxCallback(this,this.showFolderContents);
	this.getItemsInFolder(currentFolder,callback);
};

ZmBriefcaseController.prototype.showFolderContents =
function(items) {

    //populate list
    if(items){

          //filter Notebook documents
            var temp_list = items;
            this._list = new ZmList(ZmItem.MIXED, this._currentSearch);
            if(temp_list){
                var temp_arr = temp_list.getArray();
                for (var i=0; i < temp_arr.length ; i++) {
                    var r = temp_arr[i];
                    var org = appCtxt.getById(r.folderId);
                    if(org && org instanceof ZmBriefcase){
                       this._list.add(r);
                    }
                }
            }
    }else{
            this._list = new ZmList(ZmItem.BRIEFCASE);

            if (this._object) {
                var item = new ZmBriefcaseItem();
                item.id = this._object;
                this._list.add(item);
            }
    }

    // switch view
	var view = this._currentView;
	if (!view) {
		view = this._defaultView();
		this._forceSwitch = true;
	}

    this.switchView(view, this._forceSwitch);

	if(!this._forceSwitch){
	    this._setViewContents(this._currentView);
	}

};

ZmBriefcaseController.prototype.switchView =
function(view, force) {
	var viewChanged = force || view != this._currentView;

	if (viewChanged) {
		this._currentView = view;
		this._setup(view);
	}
	this._resetOperations(this._toolbar[view], 0);

	if (viewChanged) {
		var elements = {};
		elements[ZmAppViewMgr.C_TOOLBAR_TOP] = this._toolbar[this._currentView];
		if(this._currentView == ZmId.VIEW_BRIEFCASE_COLUMN){
			elements[ZmAppViewMgr.C_APP_CONTENT] = this._parentView[this._currentView];
		}else{
			elements[ZmAppViewMgr.C_APP_CONTENT] = this._listView[this._currentView];
		}
		this._setView({view:view, elements:elements, isAppView:true});
	}
	Dwt.setTitle(this.getCurrentView().getTitle());
};

ZmBriefcaseController.prototype.searchCallback =
function(callback,folderId,results) {
	var response = results.getResponse();
	var items = [];
	if (response) {
		this._list = response.getResults(ZmItem.BRIEFCASE);
		items = this._list.getArray();
		for (var i=0; i<items.length; i++) {
			if (items[i].folderId!=folderId) {
				items[i].remoteFolderId = items[i].folderId;
				items[i].folderId = folderId;
			}
			//this.putItem(items[i]);
		}
	}

	if (callback) {
		callback.run(items);
	}
};

ZmBriefcaseController.prototype.searchFolder =
function(folderId,callback) {
	var search = 'inid:"' + folderId + '"';

	var soapDoc = AjxSoapDoc.create("SearchRequest", "urn:zimbraMail");
	soapDoc.setMethodAttribute("types", ZmSearch.TYPE[ZmItem.BRIEFCASE]);
	soapDoc.setMethodAttribute("limit", "250");
	var queryNode = soapDoc.set("query", search);

	var errorCallback = null;
	//TODO: error handling to show empty folder
	var handleResponse =  null;
	var params = {
		soapDoc: soapDoc,
		asyncMode: Boolean(handleResponse),
		callback: handleResponse,
		errorCallback: errorCallback,
		noBusyOverlay: false
	};
	// NOTE: Need to keep track of request params for response handler

	var appController = appCtxt.getAppController();
	var response = appController.sendRequest(params);

	this.handleSearchResponse(folderId,response,callback);

};

ZmBriefcaseController.prototype.handleSearchResponse =
function (folderId,response,callback) {
	var items = null;    //it's zmlist now not an array
	if (response && (response.SearchResponse || response._data.SearchResponse)) {
		var searchResponse = response.SearchResponse || response._data.SearchResponse;
		var docs = searchResponse.doc || [];
		items = this.processDocsResponse(docs,folderId);
	}
	if (callback) {
		callback.run(items);
	}
};

ZmBriefcaseController.prototype.processDocsResponse =
function(docs,folderId) {
	var items = new ZmList(ZmItem.MIXED,this._currentSearch);
	for (var i = 0; i < docs.length; i++) {
		var doc = docs[i];
		var item = this.getItemById(doc.id);
		if (!item) {
			item = new ZmBriefcaseItem();
			item.set(doc);
			item.folderId = folderId;
			//item.remoteFolderId = remoteFolderId; // REVISIT
			items.add(item);
		}
		else {
			item.set(doc);
			items.add(item);
		}
    }

	//recursive search not done yet : workaround
	var folder = appCtxt.getById(folderId);
	if(folder){
		var childrens = folder.children;
		for(var i=0;i<childrens.size();i++){
			var briefcase = childrens.get(i);
			DBG.println("briefcase folder:"+briefcase.name);//cdel
			var item = this.getItemById(briefcase.id);
			if(!item){
				item = new ZmBriefcaseItem();
			}
			item.id = briefcase.id;
			item.name = briefcase.name;
//			item.folderId = this._currentFolder;
			item.folderId = folderId;
			item.isFolder = true;
			//this.putItem(item);
			items.add(item);
            //items.push(item);
		}
	}

	return items;
};


ZmBriefcaseController.prototype.getItemById =
function(itemId) {
	return (this._idMap[itemId] ? this._idMap[itemId].item : null);
};

ZmBriefcaseController.prototype.getItemsInFolder =
function(folderId,callback) {
	folderId = folderId || ZmOrganizer.ID_BRIEFCASE;
	this.searchFolder(folderId,callback);
};

ZmBriefcaseController.prototype._dragListener =
function(ev) {
	ZmListController.prototype._dragListener.call(this, ev);
};

ZmBriefcaseController.prototype.__popupUploadDialog =
function(callback, title) {
	if (!this._currentFolder) {
		this._currentFolder = ZmOrganizer.ID_BRIEFCASE;
	}

	var isShared = this.isShared(this._currentFolder);
	var isReadOnly = this.isReadOnly(this._currentFolder);

	if (isShared && isReadOnly) {
		var dialog = appCtxt.getMsgDialog();
		dialog.setMessage(ZmMsg.errorPermission, DwtMessageDialog.WARNING_STYLE);
		dialog.popup();
	} else {
        var cFolder = appCtxt.getById(this._currentFolder);
        var dialog = appCtxt.getUploadDialog();
		dialog.popup(cFolder,callback, title);
	}
};

ZmBriefcaseController.prototype.refreshFolder =
function() {
	this._refreshInProgress = true;
	this.show(this._object);
};

ZmBriefcaseController.prototype.handleRefreshFolder =
function(folderIds) {
    for(var i in folderIds) {
        if(this._currentFolder == folderIds[i]) {
            this.refreshFolder();
        }
    }
};

ZmBriefcaseController.prototype.reloadFolder =
function(mode) {
	DBG.println(AjxDebug.DBG2,"refresh folder:"+this._object);
	if(mode == "delete" && this.isMultiColView()) {
/*		var mView  = this._parentView[this._currentView];
		var col = mView.getColumn(this._currentFolder);
		mView.removeChildColumns(col.getColumnIndex());
		mView.setCurrentListView(col);*/
	}

	this.refreshFolder();
};

ZmBriefcaseController.prototype.isReadOnly =
function(folderId) {
	//if one of the ancestor is readonly then no chances of childs being writable
	var isReadOnly = false;
	var folder = appCtxt.getById(folderId);
	var rootId = ZmOrganizer.getSystemId(ZmOrganizer.ID_ROOT);
	while (folder && folder.parent && (folder.parent.id != rootId) && !folder.isReadOnly()) {
		folder = folder.parent;
	}
	if (folder && folder.isReadOnly()) {
		isReadOnly = true;
	}

	return isReadOnly;
};

ZmBriefcaseController.prototype.getBriefcaseFolder =
function(folderId) {
	var briefcase;
	var folder = appCtxt.getById(folderId);
	var rootId = ZmOrganizer.getSystemId(ZmOrganizer.ID_ROOT);
	while (folder && folder.parent && (folder.parent.id != rootId)) {
		folder = folder.parent;
	}
	briefcase = folder;
	return briefcase;
};

ZmBriefcaseController.prototype.isShared =
function(folderId) {
	var briefcase = this.getBriefcaseFolder(folderId);
	return briefcase && briefcase.link;
};

ZmBriefcaseController.prototype._listSelectionListener =
function(ev) {
	ZmListController.prototype._listSelectionListener.call(this, ev);
	if (ev.detail == DwtListView.ITEM_DBL_CLICKED) {
		var item = ev.item;
        var restUrl = item.getRestUrl();
        if(item && item.isFolder){
            if(!this.isMultiColView()){
                this.show(item.id);
            }
        }else if(restUrl != null) {
            window.open(restUrl);
        }
	}
};

ZmBriefcaseController.prototype._listActionListener =
function(ev) {
	ZmListController.prototype._listActionListener.call(this, ev);

	var actionMenu = this.getActionMenu();
	actionMenu.popup(0, ev.docX, ev.docY);
	if (ev.ersatz) {
		// menu popped up via keyboard nav
		actionMenu.setSelectedItem(0);
	}

        var item = ev.item;
        actionMenu.getOp(ZmOperation.SAVE_FILE).setEnabled(item && !item.isFolder && item.restUrl);
};

ZmBriefcaseController.prototype._getActionMenuOps =
function() {
	var list = [ ZmOperation.OPEN_FILE, ZmOperation.SAVE_FILE, ZmOperation.SEND_FILE, ZmOperation.SEND_FILE_AS_ATT ];
	if (appCtxt.get(ZmSetting.VIEW_ATTACHMENT_AS_HTML)) {
		list.push(ZmOperation.VIEW_FILE_AS_HTML);
	}
	list.push(ZmOperation.SEP);
	list = list.concat(this._standardActionMenuOps());
	return list;
};

ZmBriefcaseController.prototype._openFileListener =
function() {
	var view = this._listView[this._currentView];
	var items = view.getSelection();
	if (!items) { return; }

	items = items instanceof Array ? items : [ items ];
	for (var i = 0; i<items.length; i++) {
        var item = items[i];
        var restUrl = item.getRestUrl();
        if(item && item.isFolder){
            this.show(item.id);
        }else if(restUrl != null) {
            window.open(restUrl);
        }
	}
};

ZmBriefcaseController.prototype._saveFileListener =
function() {
	var view = this._listView[this._currentView];
	var items = view.getSelection();
	if (!items) { return; }

	items = items instanceof Array ? items : [ items ];
	for (var i = 0; i<items.length; i++) {
		var item = items[i];
		if (item && item.restUrl) {
                        window.location = item.restUrl + "?disp=a";
		}
	}
};

ZmBriefcaseController.prototype._viewAsHtmlListener =
function() {
	var view = this._listView[this._currentView];
	var items = view.getSelection();
	if (!items) { return; }

	items = items instanceof Array ? items : [ items ];
	for (var i = 0; i<items.length; i++) {
		var item = items[i];
		if (item && item.restUrl) {
			this.viewAsHtml(item.restUrl);
		}
	}
};

ZmBriefcaseController.prototype.viewAsHtml =
function(restUrl) {
	if (restUrl.match(/\?/)) {
		restUrl+= "&view=html";
	} else {
		restUrl+= "?view=html";
	}
	window.open(restUrl);
};

ZmBriefcaseController.prototype._uploadFileListener =
function() {
	if(this.isMultiColView()){
		var view = this._listView[this._currentView];
		var items = view.getSelection();
		if(items && items.length==1 && items[0].isFolder) {
			this._parentView[this._currentView].setCurrentListView(view.getNextColumn());
			this.updateCurrentFolder(items[0].id);
		}
	}
	this._app._handleNewItem();
};

ZmBriefcaseController.prototype._sendFileListener =
function(event) {
	var view = this._listView[this._currentView];
	var items = view.getSelection();
	items = items instanceof Array ? items : [ items ];

	var names = [];
	var urls = [];
	var inNewWindow = this._app._inNewWindow(event);

	var briefcase, shares;
	var noprompt = false;

	for (var i = 0; i < items.length; i++) {
		var item = items[i];
		var url = item.getRestUrl();
		if (appCtxt.isOffline) {
			var remoteUri = appCtxt.get(ZmSetting.OFFLINE_REMOTE_SERVER_URI);
			url = remoteUri + url.substring((url.indexOf("/",7)));
		}
		urls.push(url);
		names.push(item.name);

		if (noprompt) continue;

		briefcase = appCtxt.getById(item.folderId);
		shares = briefcase && briefcase.shares;
		if (shares) {
			for (var j = 0; j < shares.length; j++) {
				noprompt = noprompt || shares[j].grantee.type == ZmShare.TYPE_PUBLIC;
			}
		}
	}

	if (!shares || !noprompt) {
		var args = [names, urls, inNewWindow];
		var callback = new AjxCallback(this, this._sendFileListener2, args);

		var dialog = appCtxt.getConfirmationDialog();
		dialog.popup(ZmMsg.errorPermissionRequired, callback);
	} else {
		this._sendFileListener2(names, urls);
	}
};

ZmBriefcaseController.prototype._sendFileListener2 =
function(names, urls, inNewWindow) {
	var action = ZmOperation.NEW_MESSAGE;
	var msg = new ZmMailMsg();
	var toOverride = null;
	var subjOverride = new AjxListFormat().format(names);
	var extraBodyText = urls.join("\n");
	AjxDispatcher.run("Compose", {action: action, inNewWindow: inNewWindow, msg: msg,
								  toOverride: toOverride, subjOverride: subjOverride,
								  extraBodyText: extraBodyText});
    var cc = AjxDispatcher.run("GetComposeController");

};

ZmBriefcaseController.prototype._sendFileAsAttachmentListener =
function(event) {
	var view = this._listView[this._currentView];
	var items = view.getSelection();
	items = items instanceof Array ? items : [ items ];

    var docInfo = [];
    
	for (var i = 0; i < items.length; i++) {
		var item = items[i];
		var url = item.getRestUrl();

		var briefcase = appCtxt.getById(item.folderId);
        if(briefcase.isRemote() || briefcase.isReadOnly()) {
            continue;
        }

        docInfo.push({id: item.id, ct: item.contentType, s: item.size});
	}

    if(docInfo.length == 0) {
        return;
    }

    var action = ZmOperation.NEW_MESSAGE;
    var msg = new ZmMailMsg();
    var toOverride = null;

    //var cc = appCtxt.getApp(ZmApp.MAIL).getComposeController();
    var cc = AjxDispatcher.run("GetComposeController");
    cc._setView({action:action, msg: msg, toOverride: toOverride, inNewWindow: false});
    var callback = new AjxCallback(this, cc._handleResponseSaveDraftListener);
    cc.sendDocs(docInfo,true,callback);
};

ZmBriefcaseController.prototype._moveCallback =
function(folder) {
	this._doMove(this._pendingActionData, folder, null, true);
	this._clearDialog(appCtxt.getChooseFolderDialog());
	this._pendingActionData = null;
	this.reloadFolder();
};

ZmBriefcaseController.prototype._resetOpForCurrentView =
function(num) {
	this._resetOperations(this._toolbar[this._currentView], num || 0);
};


ZmBriefcaseController.prototype._initSendMenu =
function(view) {
    var sendBtn = this._toolbar[view].getButton(ZmOperation.SEND_FILE_MENU);
    var menu = new ZmPopupMenu(sendBtn);
    sendBtn.setMenu(menu);

    var sendOps = [ZmOperation.SEND_FILE, ZmOperation.SEND_FILE_AS_ATT];
    for (var i = 0; i < sendOps.length; i++) {
        var id = sendOps[i];
        var params = {
            image:ZmOperation.getProp(id, "image"),
            text:ZmMsg[ZmOperation.getProp(id, "textKey")]
        };
        var mi = menu.createMenuItem(id, params);
        mi.setData(ZmOperation.MENUITEM_ID, id);
        mi.addSelectionListener(this._listeners[id]);
    }
    return menu;
};

ZmBriefcaseController.prototype._setupViewMenu =
function(view, firstTime) {
	var btn;

	if (firstTime) {
		btn = this._toolbar[view].getButton(ZmOperation.VIEW_MENU);
		var menu = btn.getMenu();
		if (!menu) {
			menu = new ZmPopupMenu(btn);
			btn.setMenu(menu);
			for (var i = 0; i < ZmBriefcaseController.GROUP_BY_VIEWS.length; i++) {
				var id = ZmBriefcaseController.GROUP_BY_VIEWS[i];
				var mi = menu.createMenuItem(id, {image:ZmBriefcaseController.GROUP_BY_ICON[id],
												  text:ZmMsg[ZmBriefcaseController.GROUP_BY_MSG_KEY[id]],
												  style:DwtMenuItem.RADIO_STYLE});
				mi.setData(ZmOperation.MENUITEM_ID, id);
				mi.addSelectionListener(this._listeners[ZmOperation.VIEW]);
				if (id == this._defaultView())
					mi.setChecked(true, true);
			}
		}
	} else {
		// always set the switched view to be the checked menu item
		btn = this._toolbar[view].getButton(ZmOperation.VIEW_MENU);
		var menu = btn ? btn.getMenu() : null;
		var mi = menu ? menu.getItemById(ZmOperation.MENUITEM_ID, view) : null;
		if (mi) { mi.setChecked(true, true); }
	}

	// always reset the view menu button icon to reflect the current view
	btn.setImage(ZmBriefcaseController.GROUP_BY_ICON[view]);
};

ZmBriefcaseController.CONVERTABLE = {
	doc:/\.doc$/i,
	xls:/\.xls$/i,
	pdf:/\.pdf$/i,
	ppt:/\.ppt$/i,
	zip:/\.zip$/i
};

ZmBriefcaseController.prototype.isConvertable =
function(item) {
	var name = item.name;
	for(var type in ZmBriefcaseController.CONVERTABLE){
		var regex = ZmBriefcaseController.CONVERTABLE[type];
		if(name.match(regex)){
			return true;
		}
	}
	return false;
};

ZmBriefcaseController.prototype._viewAsHtmlListener =
function() {

	var view = this._listView[this._currentView];
	var items = view.getSelection();
	if(!items)
	return;

	items = items instanceof Array ? items : [ items ];
	for(var i = 0;i<items.length;i++){
		var item = items[i];
		if(item && item.restUrl){
			this.viewAsHtml(item.restUrl);
		}
	}
};

ZmBriefcaseController.prototype.viewAsHtml =
function(restUrl) {
	if (restUrl.match(/\?/)) {
		restUrl += "&view=html";
	} else {
		restUrl += "?view=html";
	}
	window.open(restUrl);
};

ZmBriefcaseController.prototype.addChangeListeners =
function() {
	var items = this._list.getArray();
	if(items){
			var list = ((items instanceof Array) && items.length>0) ? items[0].list : items.list;
			if(list) {
				list.addChangeListener(this._listChangeListener);
			}
	}
};

ZmBriefcaseController.prototype._fileListChangeListener =
function(ev) {
	if(ev.handled) return;
	var details = ev._details;
	if(!details) return;
	var items = details.items
	this._list._notify(ev.event,{items:items});
};

ZmBriefcaseController.prototype.getParentView =
function() {
	return this._parentView[this._currentView];
};


ZmBriefcaseController.prototype._initializeListView =
function(view) {

	if(view != ZmId.VIEW_BRIEFCASE_COLUMN){
		ZmListController.prototype._initializeListView.call(this,view);
		return;
	}

	if (this._listView[view]) { return; }

	this._listView[view] = this._createNewView(view);

};

ZmBriefcaseController.prototype._addListListeners =
function(colView) {
	colView.addSelectionListener(new AjxListener(this, this._listSelectionListener));
	colView.addActionListener(new AjxListener(this, this._listActionListener));
};

//cfolder
ZmBriefcaseController.prototype.handleUpdate =
function(organizers) {

	for (var i = 0; i < organizers.length; i++) {
		var organizer = organizers[i];
		var id = organizer.id;
		var parentId  = organizer.parent ? organizer.parent.id : null;
		if (id == this._currentFolder || this._currentFolder == parentId) {
			this.reloadFolder();
		}
	}
};


ZmBriefcaseController.prototype.updateCurrentFolder =
function(folderId) {
	this._currentFolder = folderId;
	this._object = folderId;
};

ZmBriefcaseController.prototype.isMultiColView =
function() {
	return (this._currentView == ZmId.VIEW_BRIEFCASE_COLUMN);
};

ZmBriefcaseController.prototype.mapSupported =
function(map) {
	return (map == "list" && (this._currentView != ZmId.VIEW_BRIEFCASE));
};

ZmBriefcaseController.prototype.deleteCallback =
function(ids){
	if(this.isMultiColView()){
		if(!ids) return;
		var itemIds = ids.split(",");
		var mView = this._parentView[this._currentView];
		for(var i in itemIds){
			var briefcase = appCtxt.getById(itemIds[i]);
			if(briefcase){
				briefcase.notifyDelete();
			}
			appCtxt.cacheRemove(itemIds[i]);
			var col = mView.getColumn(itemIds[i]);
			if(col){
				var prevCol = col.getPreviousColumn();
				if(prevCol) {
					mView.removeChildColumns(prevCol.getColumnIndex());
				}
			}
		}

		if(mView){
			var listView = mView.getCurrentListView();
			if(listView) {
				this.updateCurrentFolder(listView._folderId);
			}
		}
	}

	this.reloadFolder();
};

ZmBriefcaseController.prototype.getItemTooltip =
function(item, listView) {
    var dateStr = this._getDateInLocaleFormat(item.modifyDate);
    var prop = [
		{name:ZmMsg.briefcasePropName, value:item.name},
		{name:ZmMsg.briefcasePropSize, value:AjxUtil.formatSize(item.size)},
		{name:ZmMsg.briefcasePropModified, value:(item.modifyDate ? dateStr+"" : "")}
	];

	var subs = {
		fileProperties: prop,
		tagTooltip: listView._getTagToolTip(item)
	};
    return AjxTemplate.expand("briefcase.Briefcase#Tooltip", subs);
};

ZmBriefcaseController.prototype._getDateInLocaleFormat =
function(date) {
    var dateFormatter = AjxDateFormat.getDateTimeInstance(AjxDateFormat.FULL, AjxDateFormat.MEDIUM);
    return dateFormatter.format(date);
}


//offline related modules
ZmBriefcaseController.prototype.handleMailboxChange =
function() {
    //this._foldersMap = {};
    this._idMap = {};
    
    this.show(null, true);
};

ZmBriefcaseController.prototype.getCurrentFolderId =
function() {
	return this._currentFolder;
};