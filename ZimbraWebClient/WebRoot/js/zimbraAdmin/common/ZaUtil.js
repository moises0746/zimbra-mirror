/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2006, 2007, 2008, 2009 Zimbra, Inc.
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

/**
* Utility Class for the Admin Console. 
* @class ZaUtil
* 
**/

ZaUtil = function() {};

ZaUtil.HELP_URL = "help/admin/html/";

/*
 * @param v: all the valid life time value is end with smhd
 */
ZaUtil.getLifeTimeInSeconds =
function (v){
	if (AjxUtil.isLifeTime(v)) {
		var len = v.length ;
		var d = v.substr (0, len -1);
		var p = v.substr (len - 1, len);
		
		if (p == "s"){
			return d;
		}else if ( p == "m") {
			return d*60 ;
		}else if (p == "h"){
			return d*3600 ;
		}else if (p == "d") {
			return d*216000;
		}
	}else{
		throw (new AjxException(AjxMessageFormat.format(ZaMsg.UTIL_INVALID_LIFETIME,[v])));
	}
}

ZaUtil.findValueInObjArrByPropertyName =
function (arr, value, property){
	if (!property) property = "name" ; //for ZaAccountMemberOfListView 
	   
	for(var i=0; i<arr.length; i++) {
		if (arr[i][property] == value){
			return i ;
		}
	}	
	return -1;
}

ZaUtil.getListItemLabel = function (arr, value)  {
   for(var i=0; i<arr.length; i++) {
		if (arr[i]["value"] == value){
			return arr[i]["label"] ;
		}
	}

    return null ;
}

ZaUtil.findValueInArray =
function (arr, value){
	for(var i=0; i<arr.length; i++) {
		if (arr[i] == value){
			return i ;
		}
	}	
	return -1;
}

/**
 * remove the duplicate elements from an array
 */
ZaUtil.getUniqueArrayElements =
function (arr) {
	var uniqueArr = [] ;
	for (var i=0; i < arr.length; i++) {
		if (ZaUtil.findValueInArray(uniqueArr, arr[i]) < 0) {
			uniqueArr.push(arr[i]);
		}
	}
	
	return uniqueArr ;
}

/**
 * return a server date time string in yyyyMMddHHmmss'Z' format 
 * @param date: a Date object
 * 
 */
ZaUtil.getAdminServerDateTime =
function (date, useUTC) {
	var s = AjxDateUtil.getServerDateTime(date, useUTC) ;
	//remove the T of the server string generated by AjaxTK
	return s.substring(0,8) + s.substring(9) ;
}

ZaUtil.compareObjects = function(obj1, obj2) {
	if(obj1.id==obj2.id)
		return 0;
	if (obj1.name > obj2.name)
		return 1;
	if (obj1.name < obj2.name)
		return -1;	
}

ZaUtil.deepCloneObject = function (obj, ignoredProperties) {
    var newObj = {};
    if (obj) {
        for (var key in obj) {
            if (ignoredProperties && ignoredProperties.length > 0) {
                if (ZaUtil.findValueInArray(ignoredProperties, key) >=0) {
                    continue ;
                }
            }
            var v = obj [key] ;
            if (v!= null && (v instanceof Array || typeof (v) == "object")){
                newObj [key] = ZaUtil.deepCloneObject (v) ;
            }  else {
                newObj [key] = v ;
            }
        }

    }else {
        return null ;
    }

    return newObj ;
}

/**
 * combine the object array property values
 *
 * an example:
 * var objArr =
 * [
 *      {name: "abc"},
 *      {name:"efg}
 * ]
 *
 * ZaUtil.join(objArr, "name", ":") => "abc:efg" 
 */
ZaUtil.join = function (objArray, key, delimiter) {
    if (objArray == null) return "" ;
    var strArr = [] ;
    for (var i=0; i < objArray.length; i ++) {
        strArr.push (objArray[i][key]) ;
    }
    return strArr.join(delimiter) ;
}
