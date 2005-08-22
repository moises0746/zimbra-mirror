/*
***** BEGIN LICENSE BLOCK *****
Version: ZAPL 1.1

The contents of this file are subject to the Zimbra AJAX Public License Version 1.1 ("License");
You may not use this file except in compliance with the License. You may obtain a copy of the
License at http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
ANY KIND, either express or implied. See the License for the specific language governing rights
and limitations under the License.

The Original Code is: Zimbra AJAX Toolkit.

The Initial Developer of the Original Code is Zimbra, Inc.
Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
All Rights Reserved.
Contributor(s): ______________________________________.

***** END LICENSE BLOCK *****
*/

function DwtComposite(parent, className, posStyle, deferred) {

	if (arguments.length == 0) return;
	className = className || "DwtComposite";
	DwtControl.call(this, parent, className, posStyle, deferred);

	this._children = new AjxVector();
	this._updating = false;
}

DwtComposite.prototype = new DwtControl;
DwtComposite.prototype.constructor = DwtComposite;

// Pending elements hash (i.e. elements that have not yet been realized)
DwtComposite._pendingElements = new Object();

DwtComposite.prototype.toString = 
function() {
	return "DwtComposite";
}

DwtComposite.prototype.dispose =
function() {
	if (this._disposed) return;
DBG.println(AjxDebug.DBG3, "DwtComposite.prototype.dispose: " + this.toString() + " - " + this._htmlElId);
	var sz = this._children.size();
	if (sz > 0) {
		// Dup the array since disposing the children will result in _removeChild
		// being called which will modify the array
		var a = this._children.getArray().slice(0);
		for (var i = 0; i < sz; i++) {
			if (a[i].dispose)
				a[i].dispose();
		}
	}		
	DwtControl.prototype.dispose.call(this);
}

DwtComposite.prototype.getChildren =
function() {
	return this._children.getArray().slice(0);
}

DwtComposite.prototype.getNumChildren =
function() {
	return this._children.size();
}

DwtComposite.prototype.removeChildren =
function() {
	var a = this._children.getArray();
	while (this._children.size() > 0)
		a[0].dispose();
}

DwtComposite.prototype.clear =
function() {
	this.removeChildren();
	this.getHtmlElement().innerHTML = "";
}

DwtComposite.prototype._addChild =
function(child) {
	this._children.add(child);
	this.getHtmlElement().appendChild(child.getHtmlElement());
}

DwtComposite.prototype._removeChild =
function(child) {
	DBG.println(AjxDebug.DBG3, "DwtComposite.prototype._removeChild: " + child._htmlElId + " - " + child.toString());
	// Make sure that the child is initialized. Certain children (such as DwtTreeItems)
	// can be created in a deferred manner (i.e. they will only be initialized if they
	// are required to be visible
	if (child.isInitialized()) {
		this._children.remove(child);
		// Sometimes children are nested in arbitrary HTML so we elect to remove them
		// in this fashion rather than use this.getHtmlElement().removeChild(child.getHtmlElement()
		var childHtmlEl = child.getHtmlElement();
		if (childHtmlEl && childHtmlEl.parentNode)
			childHtmlEl.parentNode.removeChild(childHtmlEl);
	}
}

DwtComposite.prototype._update = function() {}
