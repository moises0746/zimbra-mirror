/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZAPL 1.1
 * 
 * The contents of this file are subject to the Zimbra AJAX Public
 * License Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra AJAX Toolkit.
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
* Creates a callback which consists of at least a function reference, and possibly also
* an object to call it from.
* @constructor
* @class
* This class represents a callback function which can be called standalone, or from a
* given object. What the callback takes as arguments and what it returns are left to the
* client.
*
* @author Conrad Damon
* @param obj	the object to call the function from
* @param func	the callback function
* @param args   default arguments
*/
function AjxCallback(obj, func, args) {
	if (arguments.length == 0) return;

	this.obj = obj;
	this.func = func;
	this._args = args;
}

AjxCallback.prototype.toString = 
function() {
	return "AjxCallback";
}

/**
* Runs the callback function, from within the object if there is one. Passes a single 
* argument on to the callback function. If you want to pass more than one argument, 
* collect them in an array and have the called function break it apart. Whatever the
* called function returns is passed along.
*
* @param args	arguments to pass to the called function
* @returns		whatever the called function returns
*/
AjxCallback.prototype.run =
function(args) {
	var args1;
	if (this._args != undefined && args != undefined) {
		if (this._args instanceof Array) {
			args1 = this._args;
		} else {
			args1 = new Array();
			args1.push(this._args);
		}
		args1 = args1.concat(args);
	} else {
		args1 = args || this._args;
	}
	if (this.obj)
		return this.func.call(this.obj, args1);
	else
		return this.func(args1);
}
