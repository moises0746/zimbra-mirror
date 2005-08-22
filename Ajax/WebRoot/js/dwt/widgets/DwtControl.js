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

/**
* Creates a control. A control may be created in "deferred" mode, meaning that the UI portion of the control
* will be created "Just In Time". This is useful for widgets which may want to defer construction
* of elements (e.g. DwtTreeItem) until such time as is needed, in the interest of efficiency. Note that if 
* the control is a child of the shell, it won't become visible until its z-index is set.
* @constructor
* @class
* This class represents a control, the highest-level useable widget. A control is a displayable element with
* a set of attributes (size, location, etc) and behaviors (event handlers). A control does not have child
* elements.
*
* @author Ross Dargahi
* @param parent		the parent widget
* @param className	CSS class
* @param posStyle	positioning style (absolute, static, or relative)
* @param deferred	postpone initialization until needed
*/

function DwtControl(parent, className, posStyle, deferred) {

	if (arguments.length == 0) return;
 	this.parent = parent;
	if (parent != null && !(parent instanceof DwtComposite))
		throw new DwtException("Parent must be a subclass of Composite", DwtException.INVALIDPARENT, "DwtWidget");

	this._data = new Object();
	this._eventMgr = new AjxEventMgr();
	this._disposed = false;
    
 	if (parent == null) 
 		return;

	if (!(parent instanceof DwtComposite))
		throw new DwtException("DwtControl parent must be a DwtComposite", DwtException.INVALIDPARENT, "DwtControl");
	
	this._className = className ? className : "DwtControl";
	this._posStyle = posStyle;
	if (!deferred)
		this._initCtrl();
}

DwtControl.prototype.toString = 
function() {
	return "DwtControl";
}

DwtControl.STATIC_STYLE = Dwt.STATIC_STYLE;
DwtControl.ABSOLUTE_STYLE = Dwt.ABSOLUTE_STYLE;
DwtControl.RELATIVE_STYLE = Dwt.RELATIVE_STYLE;

DwtControl.CLIP = Dwt.CLIP;
DwtControl.VISIBLE = Dwt.VISIBLE;
DwtControl.SCROLL = Dwt.SCROLL;
DwtControl.FIXED_SCROLL = Dwt.FIXED_SCROLL;

DwtControl.DEFAULT = Dwt.DEFAULT;

DwtControl._NO_DRAG = 1;
DwtControl._DRAGGING = 2;
DwtControl._DRAG_REJECTED = 3;

DwtControl._DRAG_THRESHOLD = 3;


DwtControl.prototype.addControlListener = 
function(listener) {
	this.addListener(DwtEvent.CONTROL, listener);
}

DwtControl.prototype.removeControlListener = 
function(listener) { 
	this.removeListener(DwtEvent.CONTROL, listener);
}

DwtControl.prototype.addDisposeListener = 
function(listener) {
	this.addListener(DwtEvent.DISPOSE, listener);
}

DwtControl.prototype.removeDisposeListener = 
function(listener) { 
	this.removeListener(DwtEvent.DISPOSE, listener);
}

DwtControl.prototype.addListener =
function(eventType, listener) {
	return this._eventMgr.addListener(eventType, listener); 	
}

DwtControl.prototype.notifyListeners =
function(eventType, event) {
	return this._eventMgr.notifyListeners(eventType, event);
}

DwtControl.prototype.isListenerRegistered =
function(eventType) {
	return this._eventMgr.isListenerRegistered(eventType);
}

DwtControl.prototype.removeListener = 
function(eventType, listener) {
	return this._eventMgr.removeListener(eventType, listener);
}

DwtControl.prototype.removeAllListeners = 
function(eventType) {
	return this._eventMgr.removeAll(eventType);
}

DwtControl.prototype.dispose =
function() {
	if (this._disposed) return;

	if (this.parent != null)
		this.parent._removeChild(this);

	Dwt.disassociateElementFromObject(null, this);

	this._disposed = true;
	var ev = new DwtDisposeEvent();
	ev.dwtObj = this;
	this.notifyListeners(DwtEvent.DISPOSE, ev);
}

DwtControl.prototype.getDocument =
function() {
	return document;
}

DwtControl.prototype.getData = 
function(key) {
	return this._data[key];
}

DwtControl.prototype.setData = 
function(key, value) {
  this._data[key] = value;
}

DwtControl.prototype.isDisposed =
function() {
	return this._isDisposed;
}

DwtControl.prototype.isInitialized =
function() {
	return this._ctrlInited;
}

DwtControl.prototype.reparent =
function(newParent) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
	var htmlEl = this.getHtmlElement();
	this.parent._removeChild(this);
	DwtComposite._pendingElements[this._htmlElId] = htmlEl;
	newParent._addChild(this);
	this.parent = newParent;
	// TODO do we need a reparent event?
}

DwtControl.prototype.getBounds =
function(incScroll) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getBounds(this.getHtmlElement(), incScroll);
}

DwtControl.prototype.setBounds =
function(x, y, width, height) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	var htmlElement = this.getHtmlElement();
	if (this.isListenerRegistered(DwtEvent.CONTROL)) {
		this._controlEvent.reset();
		var bds = Dwt.getBounds(htmlElement);
		this._controlEvent.oldX = bds.x;
		this._controlEvent.oldY = bds.y;
		this._controlEvent.oldWidth = bds.width;
		this._controlEvent.oldHeight = bds.height;
		Dwt.setBounds(htmlElement, x, y, width, height);
		bds = Dwt.getBounds(htmlElement);
		this._controlEvent.newX = bds.x;
		this._controlEvent.newY = bds.y;
		this._controlEvent.newWidth = bds.width;
		this._controlEvent.newHeight = bds.height;
		this.notifyListeners(DwtEvent.CONTROL, this._controlEvent);
	} else {
		Dwt.setBounds(htmlElement, x, y, width, height);
	}
	
	return this;
}

DwtControl.prototype.getClassName =
function() {
	return this._className;
}

DwtControl.prototype.setClassName =
function(className) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	this._className = className;
	this.getHtmlElement().className = className;
}

DwtControl.prototype.getCursor = 
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getCursor(this.getHtmlElement());
}

DwtControl.prototype.setCursor =
function(cursorName) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	Dwt.setCursor(this.getHtmlElement(), cursorName);
}

DwtControl.prototype.getDragSource =
function() {
	return this._dragSource;
}

DwtControl.prototype.setDragSource =
function(dragSource) {
	this._dragSource = dragSource;
	if (dragSource != null && this._ctrlCaptureObj == null) {
		this._ctrlCaptureObj = new DwtMouseEventCapture(this, DwtControl._mouseOverHdlr,
				DwtControl._mouseDownHdlr, DwtControl._mouseMoveHdlr, 
				DwtControl._mouseUpHdlr, DwtControl._mouseOutHdlr);
	}
}

DwtControl.prototype.getDropTarget =
function() {
	return this._dropTarget;
}

DwtControl.prototype.setDropTarget =
function(dropTarget) {
	this._dropTarget = dropTarget;
}

DwtControl.prototype.getEnabled =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return this._enabled;
}

DwtControl.prototype.setEnabled =
function(enabled, setHtmlElement) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	if (enabled != this._enabled) {
		this._enabled = enabled;
		if (setHtmlElement)
			this.getHtmlElement().disabled = !enabled;
	}
};

DwtControl.prototype.getElementById = 
function (anId) {
    // From my crude testing it seems that getElementById on IE6 is 
    // faster than document.all[id].
    return document.getElementById(anId);
};

DwtControl.prototype.getHtmlElement =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();

	var htmlEl = this.getElementById(this._htmlElId);

	if (htmlEl == null) {
		htmlEl = DwtComposite._pendingElements[this._htmlElId];
	} else if (!htmlEl._rendered) {
		delete DwtComposite._pendingElements[this._htmlElId];
		htmlEl._rendered = true;
	}
	
	return htmlEl;
}

DwtControl.prototype.setHtmlElementId =
function(id) {
	if (this._disposed) return;
	
	if (this._ctrlInited) {
		var htmlEl = this.getHtmlElement();
		if (!htmlEl._rendered) {
			delete DwtComposite._pendingElements[this._htmlElId];
			DwtComposite._pendingElements[id] = htmlEl;
		}
		htmlEl.id = id;
	}
	this._htmlElId = id;
}

DwtControl.prototype.getX =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getLocation(this.getHtmlElement()).x;
}

DwtControl.prototype.getXW =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
    var bounds = this.getBounds();
	return bounds.x+bounds.width;
}

DwtControl.prototype.getY =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getLocation(this.getHtmlElement()).y;
}

DwtControl.prototype.getYH =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
    var bounds = this.getBounds();
	return bounds.y+bounds.height;
}

DwtControl.prototype.getLocation =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getLocation(this.getHtmlElement());
}

DwtControl.prototype.setLocation =
function(x, y) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	if (this.isListenerRegistered(DwtEvent.CONTROL)) {
		var htmlElement = this.getHtmlElement();
		this._controlEvent.reset();
		var loc = Dwt.getLocation(htmlElement);
		this._controlEvent.oldX = loc.x;
		this._controlEvent.oldY = loc.y;
		Dwt.setLocation(htmlElement, x, y);
		loc = Dwt.getLocation(htmlElement);
		this._controlEvent.newX = loc.x;
		this._controlEvent.newY = loc.y;
		this.notifyListeners(DwtEvent.CONTROL, this._controlEvent);
	} else {
		Dwt.setLocation(this.getHtmlElement(), x, y);
	}
	return this;
}

DwtControl.prototype.getScrollStyle =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getScrollStyle(this.getHtmlElement());
}

DwtControl.prototype.setScrollStyle =
function(scrollStyle) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	Dwt.setScrollStyle(this.getHtmlElement(), scrollStyle);
}

DwtControl.prototype.getW = 
function(incScroll) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getSize(this.getHtmlElement(), incScroll).x;
}

DwtControl.prototype.getH = 
function(incScroll) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getSize(this.getHtmlElement(), incScroll).y;
}

DwtControl.prototype.getSize = 
function(incScroll) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getSize(this.getHtmlElement(), incScroll);
}

DwtControl.prototype.setSize = 
function(width, height) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	if (this.isListenerRegistered(DwtEvent.CONTROL)) {
		var htmlElement = this.getHtmlElement();
		this._controlEvent.reset();
		var sz = Dwt.getSize(htmlElement);
		this._controlEvent.oldWidth = sz.x;
		this._controlEvent.oldHeight = sz.y;
		Dwt.setSize(htmlElement, width, height);
		sz = Dwt.getSize(htmlElement);
		this._controlEvent.newWidth = sz.x;
		this._controlEvent.newHeight = sz.y;
		this.notifyListeners(DwtEvent.CONTROL, this._controlEvent);
	} else {
		Dwt.setSize(this.getHtmlElement(), width, height);
	}
	return this;
}

DwtControl.prototype.getToolTipContent =
function() {
	if (this._disposed) return;

	return this._toolTipContent;
}

DwtControl.prototype.setToolTipContent =
function(text) {
	if (this._disposed) return;

	this._toolTipContent = text;
}

DwtControl.prototype.getVisible =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getVisible(this.getHtmlElement());
}

DwtControl.prototype.setVisible =
function(visible) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	Dwt.setVisible(this.getHtmlElement(), visible);
}

DwtControl.prototype.getZIndex =
function() {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	return Dwt.getZIndex(this.getHtmlElement());
}

/**
* Sets the z-index for this object's HTML element. Since z-index is only relevant among peer
* elements, we make sure that all elements that are being displayed via z-index hang off the
* main shell.
*
* @param idx	the new z-index for this element
*/
DwtControl.prototype.setZIndex =
function(idx) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
//	if (!(this.parent instanceof DwtShell))
//		throw new DwtException("Element must have DwtShell as parent", DwtException.INVALIDPARENT, "DwtControl.setZIndex");
	Dwt.setZIndex(this.getHtmlElement(), idx);
}

/**
* Convenience function to toggle visibility using z-index. It uses the two lowest level
* z-indexes. Any further stacking will have to use setZIndex() directly.
*
* @param show	true if we want to show the element, false if we want to hide it
*/
DwtControl.prototype.zShow =
function(show) {
	this.setZIndex(show ? Dwt.Z_VIEW : Dwt.Z_HIDDEN);
}

DwtControl.prototype.preventSelection = 
function(targetEl) {
	return !this._isInputEl(targetEl);
}

DwtControl.prototype.preventContextMenu = 
function(targetEl) {
	return !this._isInputEl(targetEl);
}

DwtControl.prototype._isInputEl = 
function(targetEl) {
	var bIsInput = false;
	var tagName = targetEl.tagName.toLowerCase();
	var type = tagName == "input" ? targetEl.type.toLowerCase() : null;
	
	if (tagName == "textarea" || (type && (type == "text" || type == "password")))
		bIsInput = true;
	
	return bIsInput;
}

/* The next two methods are called by subclasses to enable event handling by DwtControl */
DwtControl.prototype._setMouseEventHdlrs =
function(clear) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	var htmlElement = this.getHtmlElement();
	if (clear !== true) {
		htmlElement.ondblclick = DwtControl._dblClick; 
		htmlElement.onmouseover = DwtControl._mouseOverHdlr;
		htmlElement.onmousemove = DwtControl._mouseMoveHdlr;
		htmlElement.onmousedown = DwtControl._mouseDownHdlr;
		htmlElement.onmouseup = DwtControl._mouseUpHdlr;
		htmlElement.onmouseout = DwtControl._mouseOutHdlr;
		htmlElement.onselectstart = DwtControl._onselectStartHdlr;
		htmlElement.oncontextmenu = DwtControl._oncontextMenuHdlr;
	} else {
		htmlElement.ondblclick = htmlElement.onmouseover = htmlElement.onmousemove =
		htmlElement.onmouseenter = htmlElement.onmouseleave = 
		htmlElement.onmousedown = htmlElement.onmouseup = htmlElement.onmouseout = null; 
	}
}

/**
 * This sets mouseenter and mouseleave to handle rollover events.
 * NOTE: It has the side effect of unsetting mouseover and mouseout.
 */
DwtControl.prototype._setIERolloverEventHdlrs =
function(clear) {
	if (AjxEnv.isIE) {
		if (this._disposed) return;
		
		if (!this._ctrlInited) 
			this._initCtrl();
		
		var htmlElement = this.getHtmlElement();
		
		
		if (clear !== true) {
			htmlElement.onmouseenter = DwtControl._mouseEnterHdlr;
			htmlElement.onmouseleave = DwtControl._mouseLeaveHdlr;
			htmlElement.onmouseover = null;
			htmlElement.onmouseout = null;
		} else {
			htmlElement.onmouseenter = null;
			htmlElement.onmouseleave = null;
		}
	}
};

DwtControl.prototype._setKeyEventHdlrs =
function(clear) {
	if (this._disposed) return;

	if (!this._ctrlInited) 
		this._initCtrl();
		
	var htmlElement = this.getHtmlElement();
	if (clear !== true) {
		htmlElement.onkeydown = DwtControl._keyDownHdlr;
		htmlElement.onkeyup = DwtControl._keyUpHdlr;
		htmlElement.onkeypress = DwtControl._keyPressHdlr;
	} else {
		htmlElement.onkeydown = htmlElement.onkeyup = htmlElement.onkeypress = null;
	}
}

/* Subclasses may override this method to return an HTML element that will represent
 * the dragging icon. The icon must be created on the DwtShell widget. If this method returns
 * null, it indicates that the drag failed*/
DwtControl.prototype._getDnDIcon =
function(dragOp) {
	DBG.println("DwtControl.prototype._getDnDIcon");
	return null;
}

/* Subclasses may override this method to set the DnD icon properties based on whether drops are
 * allowed */
DwtControl.prototype._setDnDIconState =
function(dropAllowed) {
	this._dndIcon.className = (dropAllowed) ? "DropAllowed" : "DropNotAllowed";
}


/* Subclasses may override this method to destroy the Dnd icon HTML element. */
DwtControl._junkIconId = 0;
DwtControl.prototype._destroyDnDIcon =
function(icon) {
	if (icon != null) {
		// not sure why there is no parent node, but if there isn't one,
		// let's try and do our best to get rid of the icon
		if (icon.parentNode) {
			icon.parentNode.removeChild(icon);
		} else {
			// at least hide the icon, and change the id so we can't get it
			// back later
			icon.style.zIndex = -100;
			icon.id = "DwtJunkIcon" + DwtControl._junkIconId++;
			icon = void 0;
		}
	}
}

/* Subclasses may override this method to provide feedback as to whether a possibly
 * valid capture is taking place. For example, there are instances such as when a mouse
 * down happens on a scroll bar in a DwtListView that are reported in the context of
 * the DwtListView, but which are not really a valid mouse down i.e. on a list item. In
 * such cases this function would return false */
 DwtControl.prototype._isValidDragObject =
 function(ev) {
 	return true;
 }

/* subclasses may override the following  functions to provide UI behaviour for DnD operations.
 * _dragEnter is called when a drag operation enters a control. _dragOver is called multiple times
 * as an item crossed over the control. _dragLeave is called when the drag operation exits the control. 
 * _drop is called when the item is dropped on the target.
 */
DwtControl.prototype._dragEnter =
function() {
}

DwtControl.prototype._dragOver =
function() {
}

DwtControl.prototype._dragLeave =
function() {
}

DwtControl.prototype._drop =
function() {
}

DwtControl.prototype._initCtrl = 
function() {
	this.shell = this.parent.shell || this.parent;
	var htmlElement = this.parent.getDocument().createElement("div");
	this._htmlElId = htmlElement.id = (this._htmlElId == null) ? Dwt.getNextId() : this._htmlElId;
	DwtComposite._pendingElements[this._htmlElId] = htmlElement;
	Dwt.associateElementWithObject(htmlElement, this);
	if (this._posStyle == null || this._posStyle == DwtControl.STATIC_STYLE) {
        htmlElement.style.position = DwtControl.STATIC_STYLE;
	} else {
        htmlElement.style.position = this._posStyle;
	}
	htmlElement.className = this._className;
	htmlElement.style.overflow = "visible";
	this._toolTipContent = null;
	this._enabled = true;
	this._controlEvent = new DwtControlEvent();
	this._dragging = DwtControl._NO_DRAG;
	this._ctrlInited = true;
	// Make sure this is the last thing we do
	this.parent._addChild(this);
}

DwtControl._keyDownHdlr =
function(ev) {
}

DwtControl._keyUpHdlr =
function(ev) {
}

DwtControl._keyPressHdlr =
function(ev) {
}

DwtControl._dblClick = 
function(ev) {
	return DwtControl._mouseEvent(ev, DwtEvent.ONDBLCLICK);
}

DwtControl._mouseEnterHdlr =
function(ev) {
	return DwtControl._mouseOverGeneralHdlr(ev, DwtEvent.ONMOUSEENTER);
};

DwtControl._mouseOverHdlr =
function(ev) {
	return DwtControl._mouseOverGeneralHdlr(ev, DwtEvent.ONMOUSEOVER);
}

DwtControl._mouseOverGeneralHdlr = function (ev, eventName){
	// Check to see if a drag is occurring. If so, don't process the mouse
	// over events.
	var captureObj = DwtMouseEventCapture.getCaptureObj();
	if (captureObj != null) {
		ev = DwtUiEvent.getEvent(ev);
		ev._stopPropagation = true;
		return false;
	}
	var obj = DwtUiEvent.getDwtObjFromEvent(ev);
	if (obj == null)
		return false;
	var mouseEv = DwtShell.mouseEvent;
	if (obj._dragging == DwtControl._NO_DRAG) {
		mouseEv.setFromDhtmlEvent(ev);
		if (obj.isListenerRegistered(eventName))
			obj.notifyListeners(eventName, mouseEv);
		// Call the tooltip after the listeners to give them a 
		// chance to change the tooltip text
		if (obj._toolTipContent != null) {
			var shell = DwtShell.getShell(window);
			var tooltip = shell.getToolTip();
			tooltip.setContent(obj._toolTipContent);
			tooltip.mouseOver(mouseEv.docX, mouseEv.docY);
		}
	}
	mouseEv._stopPropagation = true;
	mouseEv._returnValue = false;
	mouseEv.setToDhtmlEvent(ev);
	return false;

};

DwtControl._mouseDownHdlr =
function(ev) {
  		
	var obj = DwtUiEvent.getDwtObjFromEvent(ev);
	if (!obj)
		return false;
		
	if (obj._toolTipContent != null) {
		var shell = DwtShell.getShell(window);
		var tooltip = shell.getToolTip();
		tooltip.mouseDown();
	}
	
	// If we have a dragSource, then we need to start capturing mouse events
	var mouseEv = DwtShell.mouseEvent;
	mouseEv.setFromDhtmlEvent(ev);
	if (obj._dragSource != null && mouseEv.button == DwtMouseEvent.LEFT
			&& obj._isValidDragObject(mouseEv)) 
	{
		try {
			obj._ctrlCaptureObj.capture();
		} catch (ex) {
			DBG.dumpObj(ex);
		}
		obj._dragOp = (mouseEv.ctrlKey) ? Dwt.DND_DROP_COPY : Dwt.DND_DROP_MOVE;
		obj._dragStartX = mouseEv.docX;
		obj._dragStartY = mouseEv.docY;
	}
	
	return DwtControl._mouseEvent(ev, DwtEvent.ONMOUSEDOWN, mouseEv);
}

DwtControl._mouseMoveHdlr =
function(ev) {
	// If captureObj == null, then we are not a Draggable control or a 
	// mousedown event has not occurred , so do the default behaviour, 
	// else do the draggable behaviour 
	var captureObj = DwtMouseEventCapture.getCaptureObj();
	var obj = (captureObj == null) ? DwtUiEvent.getDwtObjFromEvent(ev) : 
	            captureObj.targetObj;
 	if (obj == null)
		return false;  
	var mouseEv = DwtShell.mouseEvent;
	mouseEv.setFromDhtmlEvent(ev);

	// This following can happen during a DnD operation if the mouse moves
	// out the window. This seems to happen on IE only
	if (mouseEv.docX < 0 || mouseEv.docY < 0) {
		mouseEv._stopPropagation = true;
		mouseEv._returnValue = false;
		mouseEv.setToDhtmlEvent(ev);
		return false;
	}
	
	// If we are not draggable or if have not started dragging and are 
	// within the Drag threshold then simple handle it as a move
	if (obj._dragSource == null || captureObj == null
		|| (obj != null && obj._dragging == DwtControl._NO_DRAG 
			&& Math.abs(obj._dragStartX - mouseEv.docX) < 
			   DwtControl._DRAG_THRESHOLD 
			&& Math.abs(obj._dragStartY - mouseEv.docY) < 
			   DwtControl._DRAG_THRESHOLD)) {
		if (obj._toolTipContent != null) {
			var shell = DwtShell.getShell(window);
			var tooltip = shell.getToolTip();
			tooltip.mouseMove();
		}
		return DwtControl._mouseEvent(ev, DwtEvent.ONMOUSEMOVE);
	} else {
		// Deal with mouse moving out of the window etc...
		
		// If we are not dragging, then see if we can drag. 
		// If we cannot drag this control, then
		// we will set dragging status to DwtControl._DRAG_REJECTED 
		if (obj._dragging == DwtControl._NO_DRAG) {
			obj._dragOp = obj._dragSource._beginDrag(obj._dragOp, obj);
			if (obj._dragOp != Dwt.DND_DROP_NONE) {
				obj._dragging = DwtControl._DRAGGING;
				obj._dndIcon = obj._getDnDIcon(obj._dragOp);
				if (obj._dndIcon == null)
					obj._dragging = DwtControl._DRAG_REJECTED;
			} else {
				obj._dragging = DwtControl._DRAG_REJECTED;
			}
		}
		
		// If we are draggable, then see if the control under the mouse 
		// (if one exists) will allow us to be dropped on it. 
		// This is done by (a) making sure that the drag source data type
		// can be dropped onto the target, and (b) that the application 
		// will allow it (i.e. via the listeners on the DropTarget
		if (obj._dragging != DwtControl._DRAG_REJECTED) {
			var destDwtObj = mouseEv.dwtObj;
			if (destDwtObj && destDwtObj._dropTarget && destDwtObj != obj) {
				if (destDwtObj != obj._lastDestDwtObj || 
					destDwtObj._dropTarget.hasMultipleTargets()) {
					if (destDwtObj._dropTarget._dragEnter(
										obj._dragOp, 
										destDwtObj, 
										obj._dragSource._getData(), ev)) {

						obj._setDnDIconState(true);
						obj._dropAllowed = true;
						destDwtObj._dragEnter();
					} else {
						obj._setDnDIconState(false);;
						obj._dropAllowed = false;
					}
				}
			} else {
				obj._setDnDIconState(false);
			}
			
			if (obj._lastDestDwtObj && obj._lastDestDwtObj != destDwtObj 
				&& obj._lastDestDwtObj._dropTarget 
				&& obj._lastDestDwtObj != obj) {

				obj._lastDestDwtObj._dragLeave();
				obj._lastDestDwtObj._dropTarget._dragLeave();
			}
			
			obj._lastDestDwtObj = destDwtObj;
					
			Dwt.setLocation(obj._dndIcon, mouseEv.docX + 2, mouseEv.docY + 2);
			// TODO set up timed event to fire off another mouseover event. 
			// Also need to cancel
			// any pending event, though we should do the cancel earlier 
			// in the code
		} else {
			// XXX: confirm w/ ROSS!
			DwtControl._mouseEvent(ev, DwtEvent.ONMOUSEMOVE);
		}
		mouseEv._stopPropagation = true;
		mouseEv._returnValue = false;
		mouseEv.setToDhtmlEvent(ev);
		return false;
	}
}

DwtControl._mouseUpHdlr =
function(ev) {
	// See if are doing a drag n drop operation
	var captureObj = DwtMouseEventCapture.getCaptureObj();
	var obj = (captureObj == null) ? DwtUiEvent.getDwtObjFromEvent(ev) : captureObj.targetObj;
	if (!obj._dragSource || !captureObj) {
		return DwtControl._mouseEvent(ev, DwtEvent.ONMOUSEUP);
	} else {
		captureObj.release();
		var mouseEv = DwtShell.mouseEvent;
		mouseEv.setFromDhtmlEvent(ev);
		if (obj._dragging != DwtControl._DRAGGING) {
			obj._dragging = DwtControl._NO_DRAG;
			return DwtControl._mouseEvent(ev, DwtEvent.ONMOUSEUP, mouseEv);
		} else {
			obj._lastDestDwtObj = null;
			var destDwtObj = mouseEv.dwtObj;
			if (destDwtObj != null && destDwtObj._dropTarget != null && 
				obj._dropAllowed && destDwtObj != obj) {
				destDwtObj._drop();
				destDwtObj._dropTarget._drop(obj._dragSource._getData(), ev);
				obj._dragSource._endDrag();
				obj._destroyDnDIcon(obj._dndIcon);
				obj._dragging = DwtControl._NO_DRAG;
			} else {
				// The following code sets up the drop effect for when an 
				// item is dropped onto an invalid target. Basically the 
				// drag icon will spring back to its starting location.
				obj._dragEndX = mouseEv.docX;
				obj._dragEndY = mouseEv.docY;
				if (obj._badDropAction == null) {
					obj._badDropAction = new AjxTimedAction();
					obj._badDropAction.method = 
						DwtControl.prototype._badDropEffect;
					obj._badDropAction.obj = obj;	
				}
				obj._badDropAction.params.removeAll();
				
				// Line equation is y = mx + c. Solve for c, and set up d (direction)
				var m = (obj._dragEndY - obj._dragStartY) / (obj._dragEndX - obj._dragStartX);
				obj._badDropAction.params.add(m);
				obj._badDropAction.params.add(obj._dragStartY - (m * obj._dragStartX));
				obj._badDropAction.params.add((obj._dragStartX - obj._dragEndX < 0) ? -1 : 1);
				AjxTimedAction.scheduleAction(obj._badDropAction, 0);
			}
			mouseEv._stopPropagation = true;
			mouseEv._returnValue = false;
			mouseEv.setToDhtmlEvent(ev);
			return false;
		}
	}
}


DwtControl.prototype._badDropEffect =
function(m, c, d) {
	var usingX = (Math.abs(m) <= 1);
	// Use the bigger delta to control the snap effect
	var delta = usingX ? this._dragStartX - this._dragEndX : this._dragStartY - this._dragEndY;
	if (delta * d > 0) {
		if (usingX) {
			this._dragEndX += (30 * d);
			this._dndIcon.style.top = m * this._dragEndX + c;
			this._dndIcon.style.left = this._dragEndX;
		} else {
			this._dragEndY += (30 * d);
			this._dndIcon.style.top = this._dragEndY;
			this._dndIcon.style.left = (this._dragEndY - c) / m;
		}	
		AjxTimedAction.scheduleAction(this._badDropAction, 0);
 	} else {
  		this._destroyDnDIcon(this._dndIcon);
		this._dragging = DwtControl._NO_DRAG;
  	}
}

DwtControl._mouseOutHdlr =
function(ev) {
	return DwtControl._mouseOutGeneralHdlr(ev, DwtEvent.ONMOUSEOUT);
};

DwtControl._mouseLeaveHdlr =
function(ev) {
	return DwtControl._mouseOutGeneralHdlr(ev, DwtEvent.ONMOUSELEAVE);
};

DwtControl._mouseOutGeneralHdlr = function (ev, eventName){
	var obj = DwtUiEvent.getDwtObjFromEvent(ev);
	if (obj == null)
		return false;
	if (obj._toolTipContent != null) {
		var shell = DwtShell.getShell(window);
		var tooltip = shell.getToolTip();
		tooltip.mouseOut();
	}
	return DwtControl._mouseEvent(ev, eventName);
};

DwtControl._onselectStartHdlr = 
function(ev) {
	var obj = DwtUiEvent.getDwtObjFromEvent(ev);
	if (!obj)
		return false;

	return DwtControl._mouseEvent(ev, DwtEvent.ONSELECTSTART);
}

DwtControl._oncontextMenuHdlr = 
function(ev) {
	var obj = DwtUiEvent.getDwtObjFromEvent(ev);
	if (!obj)
		return false;

	return DwtControl._mouseEvent(ev, DwtEvent.ONCONTEXTMENU);
}

DwtControl._mouseEvent = 
function(ev, eventType, mouseEvIn) {
	var obj = DwtUiEvent.getDwtObjFromEvent(ev);
	
	if (obj == null)
		return false;
		
	var mouseEv = (mouseEvIn == null ) ? DwtShell.mouseEvent : mouseEvIn;
        // notify the world that we have a mouse event.
        // By notifying before any widget registered listener receives the
        // messages, we're saying that all widget related listeners will take
        // precedence over outside listeners.
	DwtEventManager.notifyListeners( eventType, mouseEv);

	if (obj.isListenerRegistered(eventType)) {
		if (mouseEvIn == null)
			mouseEv.setFromDhtmlEvent(ev);	
		obj.notifyListeners(eventType, mouseEv);
	}
	
	if (!mouseEv._populated) {
		mouseEv._stopPropagation = true;
		mouseEv._returnValue = false;
	}
	mouseEv.setToDhtmlEvent(ev);
	return mouseEv._returnValue;
}

DwtControl.prototype.setContent =
function(content) {
	if (content)
		this.getHtmlElement().innerHTML = content;
}

DwtControl.prototype.clearContent =
function() {
	this.getHtmlElement().innerHTML = "";
}
