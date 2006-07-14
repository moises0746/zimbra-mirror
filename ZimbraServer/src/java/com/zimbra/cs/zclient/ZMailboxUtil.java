/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zclient.ZConversation.ZMessageSummary;
import com.zimbra.cs.zclient.ZMailbox.OwnerBy;
import com.zimbra.cs.zclient.ZMailbox.SharedItemBy;
import com.zimbra.cs.zclient.ZMailbox.SearchSortBy;
import com.zimbra.cs.zclient.ZSearchParams.Cursor;
import com.zimbra.cs.zclient.ZTag.Color;
import com.zimbra.cs.zclient.soap.ZSoapMailbox;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapTransport.DebugListener;

/**
 * @author schemers
 */
public class ZMailboxUtil implements DebugListener {
 
    private boolean mInteractive = false;
    private boolean mGlobalVerbose = false;
    private boolean mDebug = false;
    private String mAccount = null;
    private String mPassword = null;
    private String mUrl = "http://localhost";
    
    /** current command */
    private Command mCommand;
    
    /** current command line */
    private CommandLine mCommandLine;
    
    /** parser for internal commands */
    private CommandLineParser mParser = new GnuParser();;
    
    public void setDebug(boolean debug) { mDebug = debug; }
    
    public void setVerbose(boolean verbose) { mGlobalVerbose = verbose; }
    
    public void setAccount(String account) { mAccount = account; }

    public void setPassword(String password) { mPassword = password; }
    
    public void setUrl(String url) throws SoapFaultException { 
        try {
            URI uri = new URI(url);
            if (uri.getPath() == null || uri.getPath().length() <= 1) {
                if (url.charAt(url.length()-1) == '/') 
                    url = url.substring(0, url.length()-1) + ZimbraServlet.USER_SERVICE_URI;
                else 
                    url = url + ZimbraServlet.USER_SERVICE_URI;                
            }
            mUrl = url;
        } catch (URISyntaxException e) {
            throw SoapFaultException.CLIENT_ERROR("invlaid URL: "+url, e);
        }
    }

    private void usage() {
        
        if (mCommand != null) {
            System.out.printf("usage:%n%n%s%n", mCommand.getFullUsage());
        }

        if (mInteractive)
            return;
        
        System.out.println("");
        System.out.println("zmmailbox [args] [cmd] [cmd-args ...]");
        System.out.println("");
        System.out.println("  -h/--help                                display usage");
        System.out.println("  -f/--file                                use file as input stream");
        System.out.println("  -u/--url      http[s]://{host}[:{port}]  server hostname and optional port");
        System.out.println("  -a/--account  {name}                     account name to auth as");
        System.out.println("  -p/--password {pass}                     password for account");
        System.out.println("  -v/--verbose                             verbose mode (dumps full exception stack trace)");
        System.out.println("  -d/--debug                               debug mode (dumps SOAP messages)");
        System.out.println("");
        doHelp(null);
        System.exit(1);
    }

    public static enum Category {

        COMMANDS("help on all commands"),
        CONTACT("help on contact-related commands"),
        CONVERSATION("help on conversation-related commands"),
        FOLDER("help on folder-related commands"),
        ITEM("help on item-related commands"),
        MESSAGE("help on message-related commands"),
        MISC("help on misc commands"), 
        SEARCH("help on search-related commands"),         
        TAG("help on tag-related commands");        

        String mDesc;

        public String getDescription() { return mDesc; }
        
        Category(String desc) {
            mDesc = desc;
        }
    }

    public static Option getOption(String shortName, String longName, boolean hasArgs, String help) {
        return new Option(shortName, longName, hasArgs, help);
    }
    
    private static Option O_COLOR = new Option("c", "color", true, "color");
    private static Option O_FLAGS = new Option("f", "flags", true, "flags");
    private static Option O_FOLDER = new Option("F", "folder", true, "folder-path-or-id");
    private static Option O_LIMIT = new Option("l", "limit", true, "max number of results to return");    
    private static Option O_SORT = new Option("s", "sort", true, "sort order TODO");
    private static Option O_REPLACE = new Option("r", "replace", false, "replace contact (default is to merge)");    
    private static Option O_TAGS = new Option("t", "tags", true, "list of tag ids/names");
    private static Option O_TYPES = new Option("T", "types", true, "list of types to search for (message,conversation,contact,wiki)");
    private static Option O_VERBOSE = new Option("v", "verbose", false, "verbose output");
    private static Option O_VIEW = new Option("V", "view", true, "default type for folder (conversation,message,contact,appointment,wiki)");    
    
    enum Command {
        
        CREATE_CONTACT("createContact", "cct", "[attr1 value1 [attr2 value2...]]", "create contact", Category.CONTACT, 2, Integer.MAX_VALUE, O_FOLDER),        
        CREATE_FOLDER("createFolder", "cf", "{folder-name}", "create folder", Category.FOLDER, 1, 1, O_VIEW, O_COLOR, O_FLAGS),
        CREATE_MOUNTPOINT("createMountpoint", "cm", "{folder-name} {owner-id-or-name} {remote-item-id-or-path}", "create mountpoint", Category.FOLDER, 3, 3, O_VIEW, O_COLOR, O_FLAGS),
        CREATE_SEARCH_FOLDER("createSearchFolder", "csf", "{folder-name} {query}", "create search folder", Category.FOLDER, 2, 2, O_SORT, O_TYPES, O_COLOR),        
        CREATE_TAG("createTag", "ct", "{tag-name}", "create tag", Category.TAG, 1, 1, O_COLOR),
        DELETE_CONTACT("deleteContact", "dct", "{contact-ids}", "hard delete contact(s)", Category.CONTACT, 1, 1),        
        DELETE_CONVERSATION("deleteConversation", "dc", "{conv-ids}", "hard delete conversastion(s)", Category.CONVERSATION, 1, 1),
        DELETE_ITEM("deleteItem", "di", "{item-ids}", "hard delete item(s)", Category.ITEM, 1, 1),        
        DELETE_FOLDER("deleteFolder", "df", "{folder-path}", "hard delete a folder (and subfolders)", Category.FOLDER, 1, 1),
        DELETE_MESSAGE("deleteMessage", "dm", "{msg-ids}", "hard delete message(s)", Category.MESSAGE, 1, 1),
        DELETE_TAG("deleteTag", "dt", "{tag-name}", "delete a tag", Category.TAG, 1, 1),
        EMPTY_FOLDER("emptyFolder", "ef", "{folder-path}", "empty all the items in a folder (including subfolders)", Category.FOLDER, 1, 1),        
        EXIT("exit", "quit", "", "exit program", Category.MISC, 0, 0),
        FLAG_CONTACT("flagContact", "fct", "{contact-ids} [0|1*]", "flag/unflag contact(s)", Category.CONTACT, 1, 2),
        FLAG_CONVERSATION("flagConversation", "fc", "{conv-ids} [0|1*]", "flag/unflag conversation(s)", Category.CONVERSATION, 1, 2),
        FLAG_ITEM("flagItem", "fi", "{item-ids} [0|1*]", "flag/unflag item(s)", Category.ITEM, 1, 2),
        FLAG_MESSAGE("flagMessage", "fm", "{msg-ids} [0|1*]", "flag/unflag message(s)", Category.MESSAGE, 1, 2),
        GET_ALL_CONTACTS("getAllContacts", "gact", "[attr1 [attr2...]]", "get all contacts", Category.CONTACT, 0, Integer.MAX_VALUE, O_VERBOSE, O_FOLDER),
        GET_ALL_FOLDERS("getAllFolders", "gaf", "", "get all folders", Category.FOLDER, 0, 0, O_VERBOSE),
        GET_ALL_MOUNTPOINTS("getAllMountpoints", "gam", "", "get all mountpoints", Category.FOLDER, 0, 0, O_VERBOSE),        
        GET_ALL_TAGS("getAllTags", "gat", "", "get all tags", Category.TAG, 0, 0, O_VERBOSE),
        GET_CONTACTS("getContacts", "gct", "{contact-ids} [attr1 [attr2...]]", "get contact(s)", Category.CONTACT, 1, Integer.MAX_VALUE, O_VERBOSE),                
        GET_CONVERSATION("getConversation", "gc", "{conv-id}", "get a converation", Category.CONVERSATION, 1, 1, O_VERBOSE),
        GET_MESSAGE("getMessage", "gm", "{msg-id}", "get a message", Category.MESSAGE, 1, 1, O_VERBOSE),
        HELP("help", "?", "commands", "return help on a group of commands, or all commands. Use -v for detailed help.", Category.MISC, 0, 1, O_VERBOSE),
        IMPORT_URL_INTO_FOLDER("importURLIntoFolder", "iuif", "{folder-path} {url}", "add the contents to the remote feed at {target-url} to the folder", Category.FOLDER, 2, 2),
        MARK_CONVERSATION_READ("markConversationRead", "mcr", "{conv-ids} [0|1*]", "mark conversation(s) as read/unread", Category.CONVERSATION, 1, 2),
        MARK_CONVERSATION_SPAM("markConversationSpam", "mcs", "{conv} [0|1*] [{dest-folder-path}]", "mark conversation as spam/not-spam, and optionally move", Category.CONVERSATION, 1, 3),
        MARK_ITEM_READ("markItemRead", "mir", "{item-ids} [0|1*]", "mark item(s) as read/unread", Category.ITEM, 1, 2),
        MARK_FOLDER_READ("markFolderRead", "mfr", "{folder-path}", "mark all items in a folder as read", Category.FOLDER, 1, 1),
        MARK_MESSAGE_READ("markMessageRead", "mmr", "{msg-ids} [0|1*]", "mark message(s) as read/unread", Category.MESSAGE, 1, 2),
        MARK_MESSAGE_SPAM("markMessageSpam", "mms", "{msg} [0|1*] [{dest-folder-path}]", "mark a message as spam/not-spam, and optionally move", Category.MESSAGE, 1, 3),
        MARK_TAG_READ("markTagRead", "mtr", "{tag-name}", "mark all items with this tag as read", Category.TAG, 1, 1),
        MODIFY_CONTACT("modifyContactAttrs", "mcta", "{contact-id} [attr1 value1 [attr2 value2...]]", "modify a contact", Category.CONTACT, 3, Integer.MAX_VALUE, O_REPLACE),
        MODIFY_FOLDER_CHECKED("modifyFolderChecked", "mfch", "{folder-path} [0|1*]", "modify whether a folder is checked in the UI", Category.FOLDER, 1, 2),
        MODIFY_FOLDER_COLOR("modifyFolderColor", "mfc", "{folder-path} {new-color}", "modify a folder's color", Category.FOLDER, 2, 2),
        MODIFY_FOLDER_EXCLUDE_FREE_BUSY("modifyFolderExcludeFreeBusy", "mfefb", "{folder-path} [0|1*]", "change whether folder is excluded from free-busy", Category.FOLDER, 1, 2),        
        MODIFY_FOLDER_URL("modifyFolderURL", "mfu", "{folder-path} {url}", "modify a folder's URL", Category.FOLDER, 2, 2),
        MODIFY_TAG_COLOR("modifyTagColor", "mtc", "{tag-name} {tag-color}", "modify a tag's color", Category.TAG, 2, 2),
        MOVE_CONTACT("moveContact", "mct", "{contact-ids} {dest-folder-path}", "move contact(s) to a new folder", Category.CONTACT, 2, 2),
        MOVE_CONVERSATION("moveConversation", "mc", "{conv-ids} {dest-folder-path}", "move conversation(s) to a new folder", Category.CONVERSATION, 2, 2),
        MOVE_ITEM("moveItem", "mi", "{item-ids} {dest-folder-path}", "move item(s) to a new folder", Category.ITEM, 2, 2),
        MOVE_MESSAGE("moveMessage", "mm", "{msg-ids} {dest-folder-path}", "move message(s) to a new folder", Category.MESSAGE, 2, 2),
        NOOP("noOp", "no", "", "do a NoOp SOAP call to the server", Category.MISC, 0, 0),
        RENAME_FOLDER("renameFolder", "rf", "{folder-path} {new-folder-path}", "rename folder", Category.FOLDER, 2, 2),
        RENAME_TAG("renameTag", "rt", "{tag-name} {new-tag-name}", "rename tag", Category.TAG, 2, 2),
        SEARCH("search", "s", "{query}", "perform search", Category.SEARCH, 1, 1, O_LIMIT, O_SORT, O_TYPES),
        SEARCH_CURRENT("searchCurrent", "sc", "", "redisplay last search", Category.SEARCH, 0, 1),
        SEARCH_NEXT("searchNext", "sn", "", "fetch next page of search results", Category.SEARCH, 0, 1),
        SEARCH_PREVIOUS("searchPrevious", "sp", "", "fetch previous page of search results", Category.SEARCH, 0, 1),
        SYNC_FOLDER("syncFolder", "sf", "{folder-path}", "synchronize folder's contents to the remote feed specified by folder's {url}", Category.FOLDER, 1, 1),
        TAG_CONTACT("tagContact", "tct", "{contact-ids} {tag-name} [0|1*]", "tag/untag contact(s)", Category.CONTACT, 2, 3),
        TAG_CONVERSATION("tagConversation", "tc", "{conv-ids} {tag-name} [0|1*]", "tag/untag conversation(s)", Category.CONVERSATION, 2, 3),
        TAG_ITEM("tagItem", "ti", "{item-ids} {tag-name} [0|1*]", "tag/untag item(s)", Category.ITEM, 2, 3),
        TAG_MESSAGE("tagMessage", "tm", "{msg-ids} {tag-name} [0|1*]", "tag/untag message(s)", Category.MESSAGE, 2, 3);

        private String mName;
        private String mAlias;
        private String mSyntax;
        private String mHelp;
        private Options mOpts;
        private Category mCat;
        private int mMinArgLength = 0;
        private int mMaxArgLength = Integer.MAX_VALUE;

        public String getName() { return mName; }
        public String getAlias() { return mAlias; }
        public String getSyntax() { return mSyntax; }
        public String getHelp() { return mHelp; }
        public Category getCategory() { return mCat; }
        public boolean hasHelp() { return mSyntax != null; }
        public boolean checkArgsLength(String args[]) {
            int len = args == null ? 0 : args.length;
            return len >= mMinArgLength && len <= mMaxArgLength;
        }
        public Options getOptions() {
            return mOpts;
        }

        public String getCommandHelp() {
            String commandName = String.format("%s(%s)", getName(), getAlias());
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %-38s %s%n", commandName, getHelp()));
            return sb.toString();
        }
        
        public String getFullUsage() {
            String commandName = String.format("%s(%s)", getName(), getAlias());
            Collection opts = getOptions().getOptions();

            StringBuilder sb = new StringBuilder();
            
            sb.append(String.format("  %-28s %s%n", commandName, (opts.size() > 0 ? "[opts] ":"") + getSyntax()));
            //if (opts.size() > 0)
            //    System.out.println();
            
            for (Object o: opts) {
                Option opt = (Option) o;
                String arg = opt.hasArg() ? " <arg>" : "";
                String optStr = String.format("  -%s/--%s%s", opt.getOpt(), opt.getLongOpt(), arg);
                sb.append(String.format("  %-30s %s%n", optStr, opt.getDescription()));
            }
            //sb.append(String.format("%n    %s%n%n", getHelp()));
            return sb.toString();
        }

        private Command(String name, String alias, String syntax, String help, Category cat, int minArgLength, int maxArgLength, Option ... opts)  {
            mName = name;
            mAlias = alias;
            mSyntax = syntax;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;
            mOpts = new Options();
            for (Option o: opts) {
                mOpts.addOption(o);
            }
        }
        
    }
    
    private Map<String,Command> mCommandIndex;
    private ZMailbox mMbox;
    private String mPrompt = "mbox> ";
    ZSearchParams mSearchParams;
    ZSearchResult mSearchResult;
    
    private boolean isId(String value) {
        return (value.length() == 36 &&
                value.charAt(8) == '-' &&
                value.charAt(13) == '-' &&
                value.charAt(18) == '-' &&
                value.charAt(23) == '-');
    }
    
    private void addCommand(Command command) {
        String name = command.getName().toLowerCase();
        if (mCommandIndex.get(name) != null)
            throw new RuntimeException("duplicate command: "+name);
        
        String alias = command.getAlias().toLowerCase();
        if (mCommandIndex.get(alias) != null)
            throw new RuntimeException("duplicate command: "+alias);
        
        mCommandIndex.put(name, command);
        mCommandIndex.put(alias, command);
    }
    
    private void initCommands() {
        mCommandIndex = new HashMap<String, Command>();

        for (Command c : Command.values())
            addCommand(c);
    }
    
    private Command lookupCommand(String command) {
        return mCommandIndex.get(command.toLowerCase());
    }

    private ZMailboxUtil() {
        initCommands();
    }
    
    public void initMailbox() throws ServiceException, IOException {
        mMbox = ZSoapMailbox.getMailbox(mAccount, mPassword, mUrl, mDebug ? this : null);
        mPrompt = String.format("mbox %s> ", mAccount);
    }
    
    private ZTag lookupTag(String idOrName) throws SoapFaultException {
        ZTag tag = mMbox.getTagByName(idOrName);
        if (tag == null) tag = mMbox.getTagById(idOrName);
        if (tag == null) throw SoapFaultException.CLIENT_ERROR("unknown tag: "+idOrName, null);
        return tag;
    }
    
    /**
     * takes a list of ids or names, and trys to resolve them all to valid tag ids
     * 
     * @param idsOrNames
     * @return
     * @throws SoapFaultException
     */
    private String lookupTagIds(String idsOrNames) throws SoapFaultException {
        StringBuilder ids = new StringBuilder();
        for (String t : idsOrNames.split(",")) {
            ZTag tag = lookupTag(t);
            if (ids.length() > 0) ids.append(",");
            ids.append(tag.getId());
        }
        return ids.toString();
    }
    
    private String lookupFolderId(String pathOrId) throws ServiceException {
        return lookupFolderId(pathOrId, false);
    }

    Pattern sTargetConstraint = Pattern.compile("\\{(.*)\\}$");

    private String getTargetContstraint(String indexOrId) {
        Matcher m = sTargetConstraint.matcher(indexOrId);
        return m.find() ? m.group(1) : null;
    }
    
    private String translateId(String indexOrId) throws ServiceException {
        Matcher m = sTargetConstraint.matcher(indexOrId);
        if (m.find()) indexOrId = m.replaceAll("");

        StringBuilder ids = new StringBuilder();
        for (String t : indexOrId.split(",")) {
            
            if (t.length() > 1 && t.charAt(0) == '#') {
                t = t.substring(1);
                //System.out.println(t);                
                int i = t.indexOf('-');
                if (i != -1) {
                    int start = Integer.parseInt(t.substring(0, i));
                    String es = t.substring(i+1, t.length());
//                    System.out.println(es);
                    int end = Integer.parseInt(t.substring(i+1, t.length()));
                    for (int j = start; j <= end; j++) {
                        String id = mIndexToId.get(j);
                        if (id == null) throw SoapFaultException.CLIENT_ERROR("unknown index: "+t, null);
                        if (ids.length() > 0) ids.append(",");                        
                        ids.append(id);
                    }
                } else {
                    String id = mIndexToId.get(Integer.parseInt(t));
                    if (id == null) throw SoapFaultException.CLIENT_ERROR("unknown index: "+t, null);
                    if (ids.length() > 0) ids.append(",");                    
                    ids.append(id);
                }
            } else {
                if (ids.length() > 0) ids.append(",");                
                ids.append(t);
            }
        }
        return ids.toString();
    }

    private String lookupFolderId(String pathOrId, boolean parent) throws ServiceException {
        if (parent && pathOrId != null) pathOrId = ZMailbox.getParentPath(pathOrId);
        if (pathOrId == null || pathOrId.length() == 0) return null;
        ZFolder folder = mMbox.getFolderById(pathOrId);
        if (folder == null) {
            ZMountpoint mp = mMbox.getMountpointById(pathOrId);
            if (mp != null) return mp.getId();
        }
        if (folder == null) folder = mMbox.getFolderByPath(pathOrId);
        if (folder == null) throw SoapFaultException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        return folder.getId();
    }
    
    private String param(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }
    
    private boolean paramb(String[] args, int index, boolean defaultValue) {
        return args.length > index ? args[index].equals("1") : defaultValue;
    }
    
    private String param(String[] args, int index) {
        return param(args, index, null);
    }
    
    private ZTag.Color tagColorOpt() throws ServiceException {
        String color = mCommandLine.getOptionValue(O_COLOR.getOpt());
        return color == null ? null : ZTag.Color.fromString(color);
    }
    
    private ZFolder.Color folderColorOpt() throws ServiceException {
        String color = mCommandLine.getOptionValue(O_COLOR.getOpt());
        return color == null ? null : ZFolder.Color.fromString(color);
    }

    private ZFolder.View folderViewOpt() throws ServiceException {
        String view = mCommandLine.getOptionValue(O_VIEW.getOpt());        
        return view == null ? null : ZFolder.View.fromString(view);
    }

    private String flagsOpt() {
        return mCommandLine.getOptionValue(O_FLAGS.getOpt());        
    }

    private String typesOpt() {
        return mCommandLine.getOptionValue(O_TYPES.getOpt());        
    }

    private String folderOpt() {
        return mCommandLine.getOptionValue(O_FOLDER.getOpt());
    }

    private boolean replaceOpt() {
        return mCommandLine.hasOption(O_REPLACE.getOpt());
    }
    
    private boolean verboseOpt() {
        return mCommandLine.hasOption(O_VERBOSE.getOpt());
    }
    
    private SearchSortBy searchSortByOpt() throws ServiceException {
        String sort = mCommandLine.getOptionValue(O_SORT.getOpt());                
        return (sort == null ? null : SearchSortBy.fromString(sort));
    }
    
    private boolean execute(String args[]) throws ServiceException, ArgException, IOException {
        
        mCommand = lookupCommand(args[0]);
        
        // shift them over for parser
        String newArgs[] = new String[args.length-1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        args = newArgs;

        if (mCommand == null)
            return false;
        
        try {
            mCommandLine = mParser.parse(mCommand.getOptions(), args, true);
            args = mCommandLine.getArgs();
        } catch (ParseException e) {
            usage();
            return true;
        }
        
        if (!mCommand.checkArgsLength(args)) {
            usage();
            return true;
        }
        
        switch(mCommand) {
        case CREATE_CONTACT:
            ZContact cc = mMbox.createContact(lookupFolderId(folderOpt()), null, getMap(args, 0));
            System.out.println(cc.getId());
            break;
        case CREATE_FOLDER:
            doCreateFolder(args);
            break;
        case CREATE_MOUNTPOINT:
            doCreateMountpoint(args);
            break;
        case CREATE_SEARCH_FOLDER:
            doCreateSearchFolder(args);
            break;
        case CREATE_TAG:
            ZTag ct = mMbox.createTag(args[0], tagColorOpt());
            System.out.println(ct.getId());
            break;
        case DELETE_CONTACT:
            mMbox.deleteContact(args[0]);
            break;            
        case DELETE_CONVERSATION:
            mMbox.deleteConversation(translateId(args[0]), param(args, 1));
            break;
        case DELETE_FOLDER:
            mMbox.deleteFolder(lookupFolderId(args[0]));
            break; 
        case DELETE_ITEM:
            mMbox.deleteItem(args[0], param(args, 1));
            break;            
        case DELETE_MESSAGE:
            mMbox.deleteMessage(args[0]);
            break;
        case DELETE_TAG:
            mMbox.deleteTag(lookupTag(args[0]).getId());
            break;
        case EMPTY_FOLDER:
            mMbox.emptyFolder(lookupFolderId(args[0]));
            break;                        
        case EXIT:
            System.exit(0);
            break;
        case FLAG_CONTACT:
            mMbox.flagContact(args[0], paramb(args, 1, true));
            break;            
        case FLAG_CONVERSATION:
            mMbox.flagConversation(translateId(args[0]), paramb(args, 1, true), param(args, 2));
            break;            
        case FLAG_ITEM:
            mMbox.flagItem(args[0], paramb(args, 1, true), param(args, 2));
            break;                        
        case FLAG_MESSAGE:
            mMbox.flagMessage(args[0], paramb(args, 1, true));
            break;            
        case GET_ALL_CONTACTS:
            doGetAllContacts(args); 
            break;
        case GET_CONTACTS:
            doGetContacts(args); 
            break;            
        case GET_ALL_FOLDERS:
            doGetAllFolders(args); 
            break;            
        case GET_ALL_MOUNTPOINTS:
            doGetAllMountpoints(args); 
            break;
        case GET_ALL_TAGS:
            doGetAllTags(args); 
            break;            
        case GET_CONVERSATION:
            doGetConversation(args);
            break;
        case GET_MESSAGE:
            doGetMessage(args);
            break;            
        case HELP:
            doHelp(args); 
            break;
        case IMPORT_URL_INTO_FOLDER:
            mMbox.importURLIntoFolder(lookupFolderId(args[0]), args[1]);
            break;
        case MARK_CONVERSATION_READ:
            mMbox.markConversationRead(translateId(args[0]), paramb(args, 1, true), param(args, 2));
            break;
        case MARK_ITEM_READ:
            mMbox.markItemRead(args[0], paramb(args, 1, true), param(args, 2));
            break;
        case MARK_FOLDER_READ:
            mMbox.markFolderRead(lookupFolderId(args[0]));
            break;                                    
        case MARK_MESSAGE_READ:
            mMbox.markMessageRead(args[0], paramb(args, 1, true));
            break;
        case MARK_CONVERSATION_SPAM:            
            mMbox.markConversationSpam(translateId(args[0]), paramb(args, 1, true), lookupFolderId(param(args, 2)), param(args, 3));
            break;            
        case MARK_MESSAGE_SPAM:            
            mMbox.markMessageSpam(args[0], paramb(args, 1, true), lookupFolderId(param(args, 2)));
            break;            
        case MARK_TAG_READ:
            mMbox.markTagRead(lookupTag(args[0]).getId());
            break;
        case MODIFY_CONTACT:
            doModifyContact(args);
            break;
        case MODIFY_FOLDER_CHECKED:
            mMbox.modifyFolderChecked(lookupFolderId(args[0]), paramb(args, 1, true));
            break;                        
        case MODIFY_FOLDER_COLOR:
            mMbox.modifyFolderColor(lookupFolderId(args[0]), ZFolder.Color.fromString(args[1]));
            break;                        
        case MODIFY_FOLDER_EXCLUDE_FREE_BUSY:
            mMbox.modifyFolderExcludeFreeBusy(lookupFolderId(args[0]), paramb(args, 1, true));
            break;
        case MODIFY_FOLDER_URL:
            mMbox.modifyFolderURL(lookupFolderId(args[0]), args[1]);
            break;
        case MODIFY_TAG_COLOR:
            mMbox.modifyTagColor(lookupTag(args[0]).getId(), Color.fromString(args[1]));            
            break;
        case MOVE_CONVERSATION:
            mMbox.moveConversation(translateId(args[0]), lookupFolderId(param(args, 1)), param(args, 2));
            break;                        
        case MOVE_ITEM:
            mMbox.moveItem(args[0], lookupFolderId(param(args, 1)), param(args, 2));
            break;                                    
        case MOVE_MESSAGE:
            mMbox.moveMessage(args[0], lookupFolderId(param(args, 1)));
            break;
        case MOVE_CONTACT:
            mMbox.moveContact(args[0], lookupFolderId(param(args, 1)));
            break;
        case NOOP:
            mMbox.noOp();
            break;
        case RENAME_FOLDER:
            mMbox.renameFolder(lookupFolderId(args[0]), args[1]);
            break;        
        case RENAME_TAG:
            mMbox.renameTag(lookupTag(args[0]).getId(), args[1]);
            break;
        case SEARCH:
            doSearch(args);
            break;
        case SEARCH_NEXT:
            doSearchNext(args);
            break;
        case SEARCH_CURRENT:
            doSearchCurrent(args);
            break;            
        case SEARCH_PREVIOUS:
            doSearchPrevious(args);
            break;
        case SYNC_FOLDER:
            mMbox.syncFolder(lookupFolderId(args[0]));
            break;
        case TAG_CONTACT:
            mMbox.tagContact(args[0], lookupTag(args[1]).getId(), paramb(args, 2, true));
            break;
        case TAG_CONVERSATION:
            mMbox.tagConversation(translateId(args[0]), lookupTag(args[1]).getId(), paramb(args, 2, true), param(args, 3));
            break;
        case TAG_ITEM:
            mMbox.tagItem(args[0], lookupTag(args[1]).getId(), paramb(args, 2, true), param(args, 3));
            break;
        case TAG_MESSAGE:
            mMbox.tagMessage(args[0], lookupTag(args[1]).getId(), paramb(args, 2, true));
            break;
        default:
            return false;
        }
        return true;
    }

    private String emailAddrs(List<ZEmailAddress> addrs) {
        StringBuilder sb = new StringBuilder();
        for (ZEmailAddress e : addrs) {
            if (sb.length() >0) sb.append(", ");
            sb.append(e.getDisplay());
        }
        return sb.toString();
    }

    private void doCreateFolder(String args[]) throws ServiceException {
        ZFolder cf = mMbox.createFolder(
                lookupFolderId(args[0], true), 
                ZMailbox.getBasePath(args[0]), 
                folderViewOpt(),
                folderColorOpt(),
                flagsOpt());
        System.out.println(cf.getId());
    }

    private void doCreateSearchFolder(String args[]) throws ServiceException {

        ZSearchFolder csf = mMbox.createSearchFolder(
                lookupFolderId(args[0], true), 
                ZMailbox.getBasePath(args[0]),
                args[1],
                typesOpt(),
                searchSortByOpt(),
                folderColorOpt());
        System.out.println(csf.getId());
    }

    private void doCreateMountpoint(String args[]) throws ServiceException {
        String cmPath = args[0];
        String cmOwner = args[1];
        String cmItem = args[2];
        
        ZMountpoint cm = mMbox.createMountpoint(
                    lookupFolderId(cmPath, true), 
                    ZMailbox.getBasePath(cmPath),
                    folderViewOpt(),
                    folderColorOpt(),
                    flagsOpt(),
                    (isId(cmOwner) ? OwnerBy.BY_ID : OwnerBy.BY_NAME),
                    cmOwner,
                    (isId(cmItem) ? SharedItemBy.BY_ID : SharedItemBy.BY_PATH),
                    cmItem);
        System.out.println(cm.getId());
    }

    
    private Stack<Cursor> mSearchCursors = new Stack<Cursor>();
    private Stack<Integer> mSearchOffsets = new Stack<Integer>();
    private Map<Integer, String> mIndexToId = new HashMap<Integer, String>();

    private void doSearch(String[] args) throws ServiceException, ArgException {
        mSearchParams = new ZSearchParams(args[0]);


//        [limit {limit}] [sortby {sortBy}] [types {types}]        
        
        String limitStr = mCommandLine.getOptionValue(O_LIMIT.getOpt());
        mSearchParams.setLimit(limitStr != null ? Integer.parseInt(limitStr) : 25);
        
        SearchSortBy sortBy = searchSortByOpt();
        mSearchParams.setSortBy(sortBy != null ?  sortBy : SearchSortBy.dateDesc);
            
        String types = typesOpt();
        mSearchParams.setTypes(types != null ? types : ZSearchParams.TYPE_CONVERSATION);        
        
        mSearchCursors.clear();
        mSearchOffsets.clear();
        mSearchOffsets.push(0);
        mIndexToId.clear();
        //System.out.println(result);
        dumpSearch(mMbox.search(mSearchParams), verboseOpt());                
    }
    
    private void doSearchCurrent(String[] args) throws ServiceException {
        ZSearchResult sr = mSearchResult;
        if (sr == null) return;
        dumpSearch(mSearchResult, verboseOpt());
    }

    private void doSearchNext(String[] args) throws ServiceException {
        ZSearchParams sp = mSearchParams;
        ZSearchResult sr = mSearchResult;
        if (sp == null || sr == null || !sr.hasMore())
            return;

        List<ZSearchHit> hits = sr.getHits();
        if (hits.size() == 0) return;
        ZSearchHit lastHit = hits.get(hits.size()-1);
        Cursor cursor = new Cursor(lastHit.getId(), lastHit.getSortFied());
        mSearchCursors.push(cursor);
        mSearchOffsets.push(mSearchOffsets.peek() + hits.size());
        sp.setCursor(cursor);
        dumpSearch(mMbox.search(sp), verboseOpt());
    }

    private void doSearchPrevious(String[] args) throws ServiceException {
        ZSearchParams sp = mSearchParams;
        ZSearchResult sr = mSearchResult;
        if (sp == null || sr == null || mSearchCursors.size() == 0)
            return;
        mSearchCursors.pop();
        mSearchOffsets.pop();
        sp.setCursor(mSearchCursors.size() > 0 ? mSearchCursors.peek() : null);
        dumpSearch(mMbox.search(sp), verboseOpt());
    }

    private int colWidth(int num) {
        int i = 1;
        while (num >= 10) {
            i++;
            num /= 10;
        }
        return i;
    }

    private void dumpSearch(ZSearchResult sr, boolean verbose) throws ServiceException {
        mSearchResult =  sr;
        if (verbose) {
            System.out.println(sr);
            return;
        }
        int offset = mSearchOffsets.peek();
        int first = offset+1;
        int last = offset+sr.getHits().size();

        System.out.printf("num: %d, more: %s, hits: %d - %d%n%n", sr.getHits().size(), sr.hasMore(), first, last);
        int width = colWidth(last);
        
        final int FROM_LEN = 20;
        
        Calendar c = Calendar.getInstance();
        String headerFormat = String.format("%%%d.%ds  %%10.10s    %%-20.20s  %%-50.50s  %%s%%n", width, width);
        //String headerFormat = String.format("%10.10s  %-20.20s  %-50.50s  %-6.6s  %s%n");
        
        String itemFormat = String.format("%%%d.%ds. %%10.10s    %%-20.20s  %%-50.50s  %%tD %%<tR%%n", width, width);
        //String itemFormat = "%10.10s  %-20.20s  %-50.50s  %-6.6s  %tD %5$tR%n";

        System.out.format(headerFormat, "", "Id", "From", "Subject", "Date");
        System.out.format(headerFormat, "", "----------", "--------------------", "--------------------------------------------------", "--------------");
        int i = first;
        for (ZSearchHit hit: sr.getHits()) {
            if (hit instanceof ZConversationHit) {
                ZConversationHit ch = (ZConversationHit) hit;
                c.setTimeInMillis(ch.getDate());
                String sub = ch.getSubject();
                String from = emailAddrs(ch.getRecipients());
                if (ch.getMessageCount() > 1) {
                    String numMsg = " ("+ch.getMessageCount()+")";
                    int space = FROM_LEN - numMsg.length();
                    from = ( (from.length() < space) ? from : from.substring(0, space)) + numMsg;
                }
                //if (ch.getFragment() != null || ch.getFragment().length() > 0)
                //    sub += " (" + ch.getFragment()+")";
                mIndexToId.put(i, ch.getId());
                System.out.format(itemFormat, i++, ch.getId(), from, sub, c);
            } else if (hit instanceof ZContactHit) {
                ZContactHit ch = (ZContactHit) hit;
                c.setTimeInMillis(ch.getMetaDataChangedDate());
                String sub = ch.getEmail();
                String from = ch.getFileAsStr();
                mIndexToId.put(i, ch.getId());
                System.out.format(itemFormat, i++, ch.getId(), from, sub, c);
                
            } else if (hit instanceof ZMessageHit) {
                ZMessageHit mh = (ZMessageHit) hit;
                c.setTimeInMillis(mh.getDate());
                String sub = mh.getSubject();
                String from = mh.getSender().getDisplay();
                mIndexToId.put(i, mh.getId());
                System.out.format(itemFormat, i++, mh.getId(), from, sub, c);
            }
        }
        System.out.println();
    }

    private void doGetAllTags(String[] args) throws ServiceException {
        if (verboseOpt()) {
            StringBuilder sb = new StringBuilder();            
            for (String tagName: mMbox.getAllTagNames()) {
                ZTag tag = mMbox.getTagByName(tagName);
                if (sb.length() > 0) sb.append(",\n");
                sb.append(tag);
            }
            System.out.format("[%n%s%n]%n", sb.toString());
        } else {
            if (mMbox.getAllTagNames().size() == 0) return;            
            String hdrFormat = "%10.10s  %10.10s  %10.10s  %s%n";
            System.out.format(hdrFormat, "Id", "Unread", "Color", "Name");
            System.out.format(hdrFormat, "----------", "----------", "----------", "----------");
            for (String tagName: mMbox.getAllTagNames()) {
                ZTag tag = mMbox.getTagByName(tagName); 
                System.out.format("%10.10s  %10d  %10.10s  %s%n",
                        tag.getId(), tag.getUnreadCount(), tag.getColor().name(), tag.getName());
            }
        }
    }        

    private void doDumpFolder(ZFolder folder, boolean verbose, boolean recurse) {
        if (verbose) {
            System.out.println(folder);
        } else {
            System.out.println(folder.getPath());
        }
        if (recurse) {
            for (ZFolder child : folder.getSubFolders()) {
                doDumpFolder(child, verbose, recurse);
            }
        }
    }

    private void doGetAllFolders(String[] args) throws ServiceException {
        doDumpFolder(mMbox.getUserRoot(), verboseOpt(), true);
    }        

    
    private void dumpContacts(List<ZContact> contacts) throws ServiceException {
        if (verboseOpt()) {
            System.out.println(contacts);
        } else {
            if (contacts.size() == 0) return;            
            String hdrFormat = "%10.10s  %s%n";
            System.out.format(hdrFormat, "Id", "FileAsStr");
            System.out.format(hdrFormat, "----------", "----------");
            for (ZContact cn: contacts) {
                System.out.format("%10.10s  %s%n", 
                        cn.getId(), Contact.getFileAsString(cn.getAttrs()));
            }
        }
    }

    private void doGetAllContacts(String[] args) throws ServiceException {
        dumpContacts(mMbox.getAllContacts(lookupFolderId(folderOpt()), null, true, getList(args, 0))); 
    }        

    private void doGetContacts(String[] args) throws ServiceException, ArgException {
        System.out.println(mMbox.getContacts(args[0], null, true, getList(args, 1)));
    }

    private void doDumpMountpoints(ZFolder folder, boolean verbose, boolean recurse) {
        for (ZMountpoint link: folder.getLinks()) {
            if (verbose) {
                System.out.println(link);
            } else {
                System.out.println(link.getPath());
            }
        }

        if (recurse) {
            for (ZFolder child : folder.getSubFolders()) {
                doDumpMountpoints(child, verbose, recurse);
            }
        }
    }

    private void doGetAllMountpoints(String[] args) throws ServiceException {
        doDumpMountpoints(mMbox.getUserRoot(), verboseOpt(), true);
    }        

    private void doGetConversation(String[] args) throws ServiceException {
        ZConversation conv = mMbox.getConversation(translateId(args[0]));
        if (verboseOpt()) {
            System.out.println(conv);
        } else {

            System.out.format("%nSubject: %s%nTags: %s%nFlags: %s%nNumber-of-Messages: %d%n%n",
                    conv.getSubject(), conv.getTagIds(), conv.getFlags(), conv.getMessageCount());
            
            System.out.format("%10.10s  %-12.12s  %-50.50s  %s%n", 
                    "Id", "Sender", "Fragment", "Date");
            System.out.format("%10.10s  %-12.12s  %-50.50s  %s%n", 
                    "----------", "------------", "--------------------------------------------------", "--------------");
            for (ZMessageSummary ms : conv.getMessageSummaries()) {
                System.out.format("%10.10s  %-12.12s  %-50.50s  %tD %4$tR%n", 
                        ms.getId(), ms.getSender().getDisplay(), ms.getFragment(), ms.getDate());
            }
            System.out.println();
        }
    }        

    private void doGetMessage(String[] args) throws ServiceException {
        ZMessage msg = mMbox.getMessage(args[0], true, false, false, null, null); // TODO: optionally pass in these args
        System.out.println(msg);
    }        
    
    private void doModifyContact(String[] args) throws ServiceException, ArgException {
        ZContact mc = mMbox.modifyContact(translateId(args[0]),  mCommandLine.hasOption('r'), getMap(args, 1));
        System.out.println(mc.getId());
    }

    private void dumpContact(GalContact contact) throws ServiceException {
        System.out.println("# name "+contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }
    
    private void dumpAttrs(Map<String, Object> attrsIn) {
        TreeMap<String, Object> attrs = new TreeMap<String, Object>(attrsIn);

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    System.out.println(name+": "+sv[i]);
                }
            } else if (value instanceof String){
                System.out.println(name+": "+value);
            }
        }
    }

    private Map<String, String> getMap(String[] args, int offset) throws ArgException {
        Map<String, String> attrs = new HashMap<String, String>();
        for (int i = offset; i < args.length; i+=2) {
            String n = args[i];
            if (i+1 >= args.length)
                throw new ArgException("not enough arguments");
            String v = args[i+1];
            attrs.put(n, v);
        }
        return attrs;
    }

    private List<String> getList(String[] args, int offset) {
        List<String> attrs = new ArrayList<String>();
        for (int i = offset; i < args.length; i++) {
            attrs.add(args[i]);
        }
        return attrs;
    }

    private void interactive() throws IOException {
        mInteractive = true;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print(mPrompt);
            String line = StringUtil.readLine(in);
            if (line == null || line.length() == -1)
                break;
            if (mGlobalVerbose) {
                System.out.println(line);
            }
            String args[] = StringUtil.parseLine(line);
            if (args.length == 0)
                continue;
            try {
                if (!execute(args)) {
                    System.out.println("Unknown command. Type: 'help commands' for a list");
                }
            } catch (ServiceException e) {
                Throwable cause = e.getCause();
                System.err.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                        (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
                if (mGlobalVerbose) e.printStackTrace(System.err);
            } catch (ArgException e) {
                    usage();
            }
        }
    }

    public static void main(String args[]) throws IOException, ParseException, SoapFaultException {
        Zimbra.toolSetup();
        
        ZMailboxUtil pu = new ZMailboxUtil();
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("h", "help", false, "display usage");
        options.addOption("f", "file", true, "use file as input stream"); 
        options.addOption("u", "url", true, "http[s]://host[:port] of server to connect to");
        options.addOption("a", "account", true, "account name (not used with --ldap)");
        options.addOption("p", "password", true, "password for account");
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption("d", "debug", false, "debug mode");        
        
        CommandLine cl = null;
        boolean err = false;
        
        try {
            cl = parser.parse(options, args, true);
        } catch (ParseException pe) {
            System.err.println("error: " + pe.getMessage());
            err = true;
        }
            
        if (err || cl.hasOption('h')) {
            pu.usage();
        }
        
        pu.setVerbose(cl.hasOption('v'));
        if (cl.hasOption('u')) pu.setUrl(cl.getOptionValue('u'));
        if (cl.hasOption('a')) pu.setAccount(cl.getOptionValue('a'));
        if (cl.hasOption('p')) pu.setPassword(cl.getOptionValue('p'));
        if (cl.hasOption('d')) pu.setDebug(true);

        args = cl.getArgs();
        
        try {
            pu.initMailbox();
            if (args.length < 1) {
                pu.interactive();
            } else {
                try {
                    if (!pu.execute(args))
                        pu.usage();
                } catch (ArgException e) {
                    pu.usage();
                }
            }
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            System.err.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                    (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));  
            System.exit(2);
        }
    }
    
    class ArgException extends Exception {
        ArgException(String msg) {
            super(msg);
        }
    }

    private void doHelp(String[] args) {
        Category cat = null;
        if (args != null && args.length >= 1) {
            String s = args[0].toUpperCase();
            try {
                cat = Category.valueOf(s);
            } catch (IllegalArgumentException e) {
                for (Category c : Category.values()) {
                    if (c.name().startsWith(s)) {
                        cat = c;
                        break;
                    }
                }
            }
        }

        if (args == null || args.length == 0 || cat == null) {
            System.out.println(" zmmailbox is used for mailbox management. Try:");
            System.out.println("");
            for (Category c: Category.values()) {
                System.out.printf("     zmmailbox help %-15s %s\n", c.name().toLowerCase(), c.getDescription());
            }
            
        }
        
        if (cat != null) {
            System.out.println("");
            for (Command c : Command.values()) {
                if (!c.hasHelp()) continue;
                if (cat == Category.COMMANDS || cat == c.getCategory()) {
                    if (verboseOpt())
                        System.out.print(c.getFullUsage());
                    else
                        System.out.print(c.getCommandHelp());
                }
            }
        
        }
        System.out.println();
    }

    private long mSendStart;
    
    public void receiveSoapMessage(Element envelope) {
        long end = System.currentTimeMillis();        
        System.out.printf("======== SOAP RECEIVE =========\n");
        System.out.println(envelope.prettyPrint());
        System.out.printf("=============================== (%d msecs)\n", end-mSendStart);
        
    }

    public void sendSoapMessage(Element envelope) {
        mSendStart = System.currentTimeMillis();
        System.out.println("========== SOAP SEND ==========");
        System.out.println(envelope.prettyPrint());
        System.out.println("===============================");
    }
}
