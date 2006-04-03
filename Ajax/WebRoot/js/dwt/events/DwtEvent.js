/*
 * Copyright (C) 2006, The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


function DwtEvent(init) {
	if (arguments.length == 0) return;
	this.dwtObj = null;
}

DwtEvent.prototype.toString = 
function() {
	return "DwtEvent";
}

// native browser events - value is the associated DOM property
DwtEvent.ONCHANGE		= "onchange";
DwtEvent.ONCLICK		= "onclick";
DwtEvent.ONCONTEXTMENU	= "oncontextmenu";
DwtEvent.ONDBLCLICK		= "ondblclick";
DwtEvent.ONFOCUS		= "onfocus";
DwtEvent.ONBLUR 		= "onblur";
DwtEvent.ONKEYDOWN		= "onkeydown";
DwtEvent.ONKEYPRESS		= "onkeypress";
DwtEvent.ONKEYUP		= "onkeyup";
DwtEvent.ONMOUSEDOWN	= "onmousedown";
DwtEvent.ONMOUSEENTER	= "onmouseenter"; // IE only
DwtEvent.ONMOUSELEAVE	= "onmouseleave"; // IE only
DwtEvent.ONMOUSEMOVE	= "onmousemove";
DwtEvent.ONMOUSEOUT		= "onmouseout";
DwtEvent.ONMOUSEOVER	= "onmouseover";
DwtEvent.ONMOUSEUP		= "onmouseup";
DwtEvent.ONMOUSEWHEEL	= "onmousewheel";
DwtEvent.ONSELECTSTART	= "onselectstart";

// semantic events
DwtEvent.ACTION			= "ACTION";			// right-click
DwtEvent.BUTTON_PRESSED = "BUTTON_PRESSED";
DwtEvent.CONTROL		= "CONTROL";		// resize
DwtEvent.DATE_RANGE		= "DATE_RANGE";
DwtEvent.DISPOSE		= "DISPOSE";		// removal
DwtEvent.ENTER			= "ENTER";			// enter/return key
DwtEvent.HOVEROVER		= "HOVEROVER";		// mouseover for X ms
DwtEvent.HOVEROUT		= "HOVEROUT";
DwtEvent.POPDOWN		= "POPDOWN";
DwtEvent.POPUP			= "POPUP";
DwtEvent.SELECTION		= "SELECTION";		// left-click
DwtEvent.TREE			= "TREE";
DwtEvent.STATE_CHANGE	= "STATE_CHANGE";
DwtEvent.TAB			= "TAB";

// XForms
DwtEvent.XFORMS_READY				= "xforms-ready";
DwtEvent.XFORMS_DISPLAY_UPDATED		= "xforms-display-updated";
DwtEvent.XFORMS_VALUE_CHANGED		= "xforms-value-changed";
DwtEvent.XFORMS_FORM_DIRTY_CHANGE	= "xforms-form-dirty-change";
DwtEvent.XFORMS_CHOICES_CHANGED		= "xforms-choices-changed";
DwtEvent.XFORMS_VALUE_ERROR			= "xforms-value-error";

// Convenience lists
DwtEvent.KEY_EVENTS = [DwtEvent.ONKEYDOWN, DwtEvent.ONKEYPRESS, DwtEvent.ONKEYUP];

DwtEvent.MOUSE_EVENTS = [DwtEvent.ONCONTEXTMENU, DwtEvent.ONDBLCLICK, DwtEvent.ONMOUSEDOWN,
						 DwtEvent.ONMOUSEMOVE, DwtEvent.ONMOUSEUP, DwtEvent.ONSELECTSTART,
						 DwtEvent.ONMOUSEOVER, DwtEvent.ONMOUSEOUT];
