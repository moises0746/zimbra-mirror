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
 * AjxUtil - static class with some utility methods. This is where to
 * put things when no other class wants them.
 *
 * 12/3/2004 At this point, it only needs AjxEnv to be loaded.
 */
function AjxUtil () {
};

AjxUtil.FLOAT_RE = /^[+\-]?((\d+(\.\d*)?)|((\d*\.)?\d+))([eE][+\-]?\d+)?$/;
AjxUtil.NOTFLOAT_RE = /[^\d\.]/;
AjxUtil.NOTINT_RE = /[^0-9]+/;
AjxUtil.LIFETIME_FIELD = /^([0-9])+([dhms])?$/;
AjxUtil.isSpecified 	= function(aThing) { return ((aThing !== void 0) && (aThing !== null)); };
AjxUtil.isUndefined 	= function(aThing) { return (aThing === void 0); };
AjxUtil.isNull 		= function(aThing) { return (aThing === null); };
AjxUtil.isBoolean 	= function(aThing) { return (typeof(aThing) == 'boolean'); };
AjxUtil.isString 	= function(aThing) { return (typeof(aThing) == 'string'); };
AjxUtil.isNumber 	= function(aThing) { return (typeof(aThing) == 'number'); };
AjxUtil.isObject 	= function(aThing) { return ((typeof(aThing) == 'object') && (aThing !== null)); };
AjxUtil.isArray 		= function(aThing) { return AjxUtil.isInstance(aThing, Array); };
AjxUtil.isFunction 	= function(aThing) { return (typeof(aThing) == 'function'); };
AjxUtil.isDate 		= function(aThing) { return AjxUtil.isInstance(aThing, Date); };

AjxUtil.isNumeric 	= function(aThing) { return (!isNaN(parseInt(aThing)) && AjxUtil.FLOAT_RE.test(aThing) && !AjxUtil.NOTFLOAT_RE.test(aThing)); };
AjxUtil.isInteger	= function(aThing) { return (AjxUtil.isNumeric(aThing) && !AjxUtil.NOTINT_RE.test(aThing)); };
AjxUtil.isNonNegativeInteger = function (aThing) {
	var retVal = (AjxUtil.isNumeric(aThing) && AjxUtil.isInteger(aThing) && (parseInt(aThing) >= 0) ); 
	return retVal;
};
AjxUtil.isLifeTime = function (aThing) { return AjxUtil.LIFETIME_FIELD.test(aThing); };

AjxUtil.SIZE_GIGABYTES = "GB";
AjxUtil.SIZE_MEGABYTES = "MB";
AjxUtil.SIZE_KILOBYTES = "KB";
AjxUtil.SIZE_BYTES = "B";

/**
 * Formats a size (in bytes) to the largest whole unit. For example,
 * AjxUtil.formatSize(302132199) returns "288 MB".
 *
 * @param size      The size (in bytes) to be formatted.
 * @param round     True to round to nearest integer. Default is true.
 * @param fractions Number of fractional digits to display, if not rounding.
 *                  Trailing zeros after the decimal point are trimmed.
 */
AjxUtil.formatSize = 
function(size, round, fractions) {
	if (round == null) round = true;
	if (fractions == null) fractions = 20; // max allowed for toFixed is 20

	var units = AjxUtil.SIZE_BYTES;
	if (size >= 1073741824) {
		size /= 1073741824;
		units = AjxUtil.SIZE_GIGABYTES;
	}
	else if (size >= 1048576) {
		size /= 1048576;
		units = AjxUtil.SIZE_MEGABYTES;
	}
	else if (size > 1023) {
		size /= 1024;
		units = AjxUtil.SIZE_KILOBYTES;
	}

	var formattedSize = round ? Math.round(size) : size.toFixed(fractions).replace(/\.?0+$/,"");
	var formattedUnits = ' '+units;
	
	return formattedSize + formattedUnits;
}

/**
 * Formats a size (in bytes) to a specific unit. Since the unit size is
 * known, the unit is not shown in the returned string. For example,
 * AjxUtil.formatSizeForUnit(302132199, AjxUtil.SIZE_MEGABYTES, false, 2) 
 * returns "288.13".
 *
 * @param size      The size (in bytes) to be formatted.
 * @param units     The unit of measure.
 * @param round     True to round to nearest integer. Default is true.
 * @param fractions Number of fractional digits to display, if not rounding.
 *                  Trailing zeros after the decimal point are trimmed.
 */
AjxUtil.formatSizeForUnits = function(size, units, round, fractions) {
	if (units == null) units = AjxUtil.SIZE_BYTES;
	if (round == null) round = true;
	if (fractions == null) fractions = 20; // max allowed for toFixed is 20

	switch (units) {
		case AjxUtil.SIZE_GIGABYTES: { size /= 1073741824; break; }
		case AjxUtil.SIZE_MEGABYTES: { size /= 1048576; break; }
		case AjxUtil.SIZE_KILOBYTES: { size /= 1024; break; }
	}
	
	var formattedSize = round ? Math.round(size) : size.toFixed(fractions).replace(/\.?0+$/,"");
	return formattedSize;
}

/**
 * Performs the opposite of AjxUtil.formatSize in that this function takes a 
 * formatted size.
 *
 * @param units Unit constant: "GB", "MB", "KB", "B". Must be specified 
 *              unless the formatted size ends with the size marker, in
 *				which case the size marker in the formattedSize param
 *				overrides this parameter.
 */
AjxUtil.parseSize = function(formattedSize, units) {
	// NOTE: Take advantage of fact that parseFloat ignores bad chars
	//       after numbers
	var size = parseFloat(formattedSize.replace(/^\s*/,""));

	var marker = /[GMK]?B$/i;
	var result = marker.exec(formattedSize);
	if (result) {
		//alert("units: "+units+", result[0]: '"+result[0]+"'");
		units = result[0].toUpperCase();
	}
	
	switch (units) {
		case AjxUtil.SIZE_GIGABYTES: size *= 1073741824; break;
		case AjxUtil.SIZE_MEGABYTES: size *= 1048576; break; 
		case AjxUtil.SIZE_KILOBYTES: size *= 1024; break;
	}
	
	//alert("AjxUtil#parseSize: formattedSize="+formattedSize+", size="+size);
	return size;
}

AjxUtil.isInstance = 
function(aThing, aClass) { 
	return !!(aThing && aThing.constructor && (aThing.constructor === aClass)); 
};

AjxUtil.assert = function(aCondition, aMessage) {
	if (!aCondition && AjxUtil.onassert) AjxUtil.onassert(aMessage);
};

AjxUtil.onassert = 
function(aMessage) {
	// Create an exception object and set the message
	var myException = new Object();
	myException.message = aMessage;
	
	// Compile a stack trace
	var myStack = new Array();
	if (AjxEnv.isIE5_5up) {
		// On IE, the caller chain is on the arguments stack
		var myTrace = arguments.caller;
		while (myTrace) {
		    myStack[myStack.length] = myTrace.callee;
	    	myTrace = myTrace.caller;
		}
	} else {
		try {
			var myTrace = arguments.callee.caller;
			while (myTrace) {
				myStack[myStack.length] = myTrace;
				if (myStack.length > 2) break;
				myTrace = myTrace.caller;
		    }
		} catch (e) {
		}
	}
	myException.stack = myStack;
	
	// Alert with the message and a description of the stack
	var stackString = '';
	var MAX_LEN = 170;
	for (var i = 1; i < myStack.length; i++) {
		if (i > 1) stackString += '\n';
		if (i < 11) {
			var fs = myStack[i].toString();
			if (fs.length > MAX_LEN) {
				fs = fs.substr(0,MAX_LEN) + '...';
				fs = fs.replace(/\n/g, '');
			}
			stackString += i + ': ' + fs;
		} else {
			stackString += '(' + (myStack.length - 11) + ' frames follow)';
			break;
		}
	}
	alert('assertion:\n\n' + aMessage + '\n\n---- Call Stack ---\n' + stackString);
	
	// Now throw the exception
	throw myException;
};

AjxUtil.NODE_REPEATS = new Object();
AjxUtil.NODE_REPEATS["folder"]	= true;
AjxUtil.NODE_REPEATS["search"]	= true;
AjxUtil.NODE_REPEATS["tag"]		= true;
AjxUtil.NODE_REPEATS["pref"]		= true;
AjxUtil.NODE_REPEATS["attr"]		= true;
AjxUtil.NODE_REPEATS["c"]		= true;
AjxUtil.NODE_REPEATS["m"]		= true;
AjxUtil.NODE_REPEATS["cn"]		= true;
AjxUtil.NODE_REPEATS["e"]		= true;
AjxUtil.NODE_REPEATS["a"]		= true;
AjxUtil.NODE_REPEATS["mbx"]		= true;
//AjxUtil.NODE_REPEATS["mp"]		= true; // only when parent is "mp"
// these really shouldn't repeat
AjxUtil.NODE_REPEATS["prefs"]	= true;
AjxUtil.NODE_REPEATS["attrs"]	= true;
AjxUtil.NODE_REPEATS["tags"]	= true;

AjxUtil.NODE_IS_ATTR = new Object();
AjxUtil.NODE_IS_ATTR["authToken"]	= true;
AjxUtil.NODE_IS_ATTR["lifetime"]		= true;
AjxUtil.NODE_IS_ATTR["sessionId"]	= true;
AjxUtil.NODE_IS_ATTR["name"]			= true;
AjxUtil.NODE_IS_ATTR["quotaUsed"]	= true;
AjxUtil.NODE_IS_ATTR["su"]			= true;
AjxUtil.NODE_IS_ATTR["fr"]			= true;
AjxUtil.NODE_IS_ATTR["mid"]			= true;
//AjxUtil.NODE_IS_ATTR["content"]	= true; // only when parent is "note"

AjxUtil.NODE_CONTENT = new Object();
AjxUtil.NODE_CONTENT["pref"]	= true;
AjxUtil.NODE_CONTENT["attr"]	= true;
AjxUtil.NODE_CONTENT["a"]	= true;

// IE doesn't define Node type constants
AjxUtil.ELEMENT_NODE	= 1;
AjxUtil.TEXT_NODE	= 3;

AjxUtil.xmlToJs =
function(node, omitName) {

	if (node.nodeType == AjxUtil.TEXT_NODE)
		return ['"', node.data, '"'].join("");

	var name = node.name ? node.name : node.localName;
	if (node.nodeType == AjxUtil.ELEMENT_NODE) {
		var text = omitName ? "{" : [name, ":{"].join("");
		var needComma = false;	
		if (node.attributes) {
			for (var i = 0; i < node.attributes.length; i++) {
				var attr = node.attributes[i];
				if (attr.name == "xmlns") continue;
				if (needComma) text += ",";
				var value = AjxUtil.isNumeric(attr.value) ? attr.value : AjxUtil.jsEncode(attr.value);
				text = [text, attr.name, ':', value].join("");
				needComma = true;
			}
		}
		if (node.hasChildNodes()) {
			var cnodes = new Object();
			var hasChild = false;
			for (var i = 0; i < node.childNodes.length; i++) {
				var child = node.childNodes[i];
				var cname = child.name ? child.name : child.localName;
				var isAttr = AjxUtil.NODE_IS_ATTR[cname] || 
							 (name == "content" && parent.name == "note");
				if (isAttr) {
					if (needComma) text += ",";
					text = [text, cname, ':', AjxUtil.jsEncode(child.textContent)].join("");
					needComma = true;
				} else {
					if (!cnodes[cname])
						cnodes[cname] = new Array();
					cnodes[cname].push(child);
					hasChild = true;
				}
			}
			if (hasChild && needComma) {text += ","; needComma = false;}
			for (var cname in cnodes) {
				if (needComma) {
					text += ",";
					needComma = false;
				}
				var repeats = AjxUtil.NODE_REPEATS[cname] ||
							  (cname == "mp" && name == "mp");
				if (repeats) text += cname + ":[";
				var clist = cnodes[cname];
				for (var i = 0; i < clist.length; i++) {
					if (needComma) text += ",";
					text += AjxUtil.xmlToJs(clist[i], repeats);
					needComma = true;
				}
				if (repeats) text += "]";
			}
		}
		text += "}";
	}

	return text;
}

AjxUtil.JS_CHAR_ENCODINGS = [
	"\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005", "\\u0006", "\\u0007",
	"\\b",     "\\t",     "\\n",     "\\u000B", "\\f",     "\\r",     "\\u000E", "\\u000F",
	"\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017",
	"\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E", "\\u001F"
];

AjxUtil.jsEncode =
function(string) {

	if (!string) return "\"\"";

	var text = '"';
	for (var i = 0; i < string.length; i++) {
		var c = string.charAt(i);
		switch (c) {
			case '\\': case '"': case '/':
				text += '\\' + c;
				break;
			default:
				var code = string.charCodeAt(i);
				text += (code < 32) ? AjxUtil.JS_CHAR_ENCODINGS[code] : c;
		}
	}
	text += '"';
	return text;
}
