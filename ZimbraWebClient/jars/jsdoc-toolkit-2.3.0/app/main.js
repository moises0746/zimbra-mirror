/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2010, 2013 Zimbra Software, LLC.
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
/**
 * @version $Id: main.js 795 2009-06-19 07:03:22Z micmath $
 */

function main() {
	IO.include("lib/JSDOC.js");
	IO.includeDir("plugins/");
	
	// process the options
	
	// the -c option: options are defined in a configuration file
	if (JSDOC.opt.c) {
		eval("JSDOC.conf = " + IO.readFile(JSDOC.opt.c));
		
		LOG.inform("Using configuration file at '"+JSDOC.opt.c+"'.");
		
		for (var c in JSDOC.conf) {
			if (c !== "D" && !defined(JSDOC.opt[c])) { // commandline overrules config file
				JSDOC.opt[c] = JSDOC.conf[c];
			}
		}
		
		if (typeof JSDOC.conf["_"] != "undefined") {
			JSDOC.opt["_"] = JSDOC.opt["_"].concat(JSDOC.conf["_"]);
		}
		
		LOG.inform("With configuration: ");
		for (var o in JSDOC.opt) {
			LOG.inform("    "+o+": "+JSDOC.opt[o]);
		}
	}
	
	// be verbose
	if (JSDOC.opt.v) LOG.verbose = true;
	
	// send log messages to a file
	if (JSDOC.opt.o) LOG.out = IO.open(JSDOC.opt.o);
	
	// run the unit tests
	if (JSDOC.opt.T) {
		LOG.inform("JsDoc Toolkit running in test mode at "+new Date()+".");
		IO.include("frame/Testrun.js");
		IO.include("test.js");
	}
	else {
		// a template must be defined and must be a directory path
		if (!JSDOC.opt.t && System.getProperty("jsdoc.template.dir")) {
			JSDOC.opt.t = System.getProperty("jsdoc.template.dir");
		}
		if (JSDOC.opt.t && !JSDOC.opt.t.charAt(JSDOC.opt.t.length-1).match(/[\\\/]/)) {
			JSDOC.opt.t += SYS.slash;
		}
		
		// verbose messages about the options we were given
		LOG.inform("JsDoc Toolkit main() running at "+new Date()+".");
		LOG.inform("With options: ");
		for (var o in JSDOC.opt) {
			LOG.inform("    "+o+": "+JSDOC.opt[o]);
		}
		
		// initialize and build a symbolSet from your code
		JSDOC.JsDoc();
		
		// debugger's option: dump the entire symbolSet produced from your code
		if (JSDOC.opt.Z) {
			LOG.warn("So you want to see the data structure, eh? This might hang if you have circular refs...");
			IO.include("frame/Dumper.js");
			var symbols = JSDOC.JsDoc.symbolSet.toArray();
			for (var i = 0, l = symbols.length; i < l; i++) {
				var symbol = symbols[i];
				print("// symbol: " + symbol.alias);
				print(symbol.serialize());
			}
		}
		else {
			if (typeof JSDOC.opt.t != "undefined") {
				try {
					// a file named "publish.js" must exist in the template directory
					load(JSDOC.opt.t+"publish.js");
					
					// and must define a function named "publish"
					if (!publish) {
						LOG.warn("No publish() function is defined in that template so nothing to do.");
					}
					else {
						// which will be called with the symbolSet produced from your code
						publish(JSDOC.JsDoc.symbolSet);
					}
				}
				catch(e) {
					LOG.warn("Sorry, that doesn't seem to be a valid template: "+JSDOC.opt.t+"publish.js : "+e);
				}
			}
			else {
				LOG.warn("No template given. Might as well read the usage notes.");
				JSDOC.usage();
			}
		}
	}
	
	// notify of any warnings
	if (!JSDOC.opt.q && LOG.warnings.length) {
		print(LOG.warnings.length+" warning"+(LOG.warnings.length != 1? "s":"")+".");
	}
	
	// stop sending log messages to a file
	if (LOG.out) {
		LOG.out.flush();
		LOG.out.close();
	}
}