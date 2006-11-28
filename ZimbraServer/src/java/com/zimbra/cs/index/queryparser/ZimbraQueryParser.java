/* Generated By:JavaCC: Do not edit this line. ZimbraQueryParser.java */
package com.zimbra.cs.index.queryparser;

import java.util.TimeZone;
import java.util.Locale;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.*;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.mailbox.MailServiceException;

import org.apache.lucene.analysis.Analyzer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public final class ZimbraQueryParser implements ZimbraQueryParserConstants {

    private static Log mLog = LogFactory.getLog(ZimbraQueryParser.class);

    private static HashMap sFolderStrMap;

    private static abstract class GetQueryCallback {
        public abstract ZimbraQuery.BaseQuery execute(Mailbox mailbox, Analyzer analyzer, int modifier) throws ServiceException;
    }

    private static HashMap sIsStrMap;

    static {
        /* Well-known folder name string map */
        sFolderStrMap = new HashMap();

        sFolderStrMap.put("inbox",    new Integer(Mailbox.ID_FOLDER_INBOX));
        sFolderStrMap.put("trash",    new Integer(Mailbox.ID_FOLDER_TRASH));
        sFolderStrMap.put("junk",     new Integer(Mailbox.ID_FOLDER_SPAM));
        sFolderStrMap.put("sent",     new Integer(Mailbox.ID_FOLDER_SENT));
        sFolderStrMap.put("drafts",   new Integer(Mailbox.ID_FOLDER_DRAFTS));
        sFolderStrMap.put("contacts", new Integer(Mailbox.ID_FOLDER_CONTACTS));

        /* is: string map */
        sIsStrMap = new HashMap();

        // expressing this in java is soooo ugly.... <sigh>
        sIsStrMap.put("read",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.ReadQuery(mbx, analyze, modifier, true);
                      } } );

        sIsStrMap.put("unread",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.ReadQuery(mbx, analyze, modifier, false);
                      } } );

        sIsStrMap.put("flagged",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.FlaggedQuery(mbx, analyze, modifier, true);
                      } } );

        sIsStrMap.put("unflagged",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                              return new ZimbraQuery.FlaggedQuery(mbx, analyze, modifier, false);
                      } } );

        sIsStrMap.put("draft",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                              return new ZimbraQuery.DraftQuery(mbx, analyze, modifier, true);
                      } } );

        sIsStrMap.put("sent",  /* sent by me */
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.SentQuery(mbx, analyze, modifier, true);
                      } } );

        sIsStrMap.put("fromme", /* sent by me */
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.SentQuery(mbx, analyze, modifier, true);
                      } } );

        sIsStrMap.put("received",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.SentQuery(mbx, analyze, modifier, false);
                      } } );

        sIsStrMap.put("replied",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.RepliedQuery(mbx, analyze, modifier, true);
                      } } );

        sIsStrMap.put("unreplied",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.RepliedQuery(mbx, analyze, modifier, false);
                      } } );

        sIsStrMap.put("forwarded",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.ForwardedQuery(mbx, analyze, modifier, true);
                      } } );

        sIsStrMap.put("unforwarded",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return new ZimbraQuery.ForwardedQuery(mbx, analyze, modifier, false);
                      } } );

        sIsStrMap.put("invite",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return ZimbraQuery.DBTypeQuery.IS_INVITE(mbx, analyze, modifier);
                      } } );

        sIsStrMap.put("anywhere",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return ZimbraQuery.InQuery.Create(mbx, analyze, modifier, ZimbraQuery.InQuery.IN_ANY_FOLDER);
                      } } );

        sIsStrMap.put("local",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                          return ZimbraQuery.InQuery.Create(mbx, analyze, modifier, ZimbraQuery.InQuery.IN_LOCAL_FOLDER);
                      } } );

        sIsStrMap.put("remote",
                      new GetQueryCallback() { public ZimbraQuery.BaseQuery execute(Mailbox mbx, Analyzer analyze, int modifier) throws ServiceException {
                                  return ZimbraQuery.InQuery.Create(mbx, analyze, modifier, ZimbraQuery.InQuery.IN_REMOTE_FOLDER);
                      } } );
    }

    public ZimbraQuery.BaseQuery GetQuery(int modifier, int target, String tok) throws ParseException, ServiceException, MailServiceException
    {
        Integer folderId = null;
        ItemId iid = null;

        switch(target) {
          case HAS:
                if (!tok.equalsIgnoreCase("attachment")) {
                    return new ZimbraQuery.HasQuery(mAnalyzer, modifier, tok);
            }
            tok = "any";
            // otherwise FALL THROUGH to AttachmentQuery below!
          case ATTACHMENT:
            return new ZimbraQuery.AttachmentQuery(mAnalyzer, modifier,tok);
          case TYPE:
            return new ZimbraQuery.TypeQuery(mAnalyzer, modifier,tok);
          case ITEM:
            return ZimbraQuery.ItemQuery.Create(mAnalyzer, modifier, tok);
          case INID:
                  iid = new ItemId(tok, null);
              folderId = iid.getId();
              // FALL THROUGH TO BELOW!
          case IN:
            if (folderId == null)
                folderId = (Integer) sFolderStrMap.get(tok.toLowerCase());
            ZimbraQuery.BaseQuery inq;
            if (iid != null && !iid.belongsTo(mMailbox)) {
                inq = new ZimbraQuery.InQuery(mMailbox, mAnalyzer, modifier, iid);
            } else if (folderId != null) {
                inq = ZimbraQuery.InQuery.Create(mMailbox, mAnalyzer, modifier, folderId);
            } else {
                inq = ZimbraQuery.InQuery.Create(mMailbox, mAnalyzer, modifier, tok);
            }
            if (inq == null) {
                throw MailServiceException.NO_SUCH_FOLDER(tok);
            }
            return inq;
          case TAG:
            return new ZimbraQuery.TagQuery(mAnalyzer, modifier, mMailbox.getTagByName(tok), true);
          case IS:
            GetQueryCallback cback = (GetQueryCallback)sIsStrMap.get(tok.toLowerCase());
            if (cback != null) {
                return cback.execute(mMailbox, mAnalyzer, modifier);
            } else {
                throw new ParseException("Unknown text after is: in query string");
            }
          case CONV:
            if (tok.charAt(0) != '-') {
                return new ZimbraQuery.ConvQuery(mAnalyzer, modifier, tok);
            } else {
                // virtual-conversation: search for the item-id with id = -1*X
                return ZimbraQuery.ItemQuery.Create(mAnalyzer, modifier, tok.substring(1));
            }
          case DATE:
          case DAY:
          case WEEK:
          case MONTH:
          case YEAR:
          case AFTER:
          case BEFORE:
          case CONV_START:
          case CONV_END:
          {
              ZimbraQuery.DateQuery q = new ZimbraQuery.DateQuery(mAnalyzer, target);
              q.parseDate(modifier, tok, mTimeZone, mLocale);
              return q;
          }
          case TO:
          case FROM:
          case ENVTO:
          case ENVFROM:
          case CC:
            if (tok == null || tok.length() < 1) {
                throw new ParseException("Missing required text after a TO/FROM/CC");
            }
            if (tok.charAt(0) == '@') {
                return new ZimbraQuery.DomainQuery(mAnalyzer, modifier, target, tok);
            }
            return new ZimbraQuery.TextQuery(mMailbox, mAnalyzer, modifier,target,tok);
          case SIZE:
          case BIGGER:
          case SMALLER:
            return new ZimbraQuery.SizeQuery(mAnalyzer, modifier,target,tok);
          default:
            return new ZimbraQuery.TextQuery(mMailbox, mAnalyzer, modifier,target,tok);
        }
    }

    private Analyzer mAnalyzer = null;
    private Mailbox mMailbox = null;
    private TimeZone mTimeZone = null;
    private Locale mLocale = null;

        // the query string can OPTIONALLY have a "sortby:" element which will override the
        // sortBy specified in the <SearchRequest> xml...this is basically to allow people
        // to do more with cut-and-pasted search strings
    private String mSortByStr = null;
    public String getSortByStr() { return mSortByStr; }

    public void init(Analyzer analyzer, Mailbox mbx, TimeZone tz, Locale locale) {
       mAnalyzer = analyzer;
           mMailbox = mbx;
           mTimeZone = tz;
           mLocale = locale;
    }

    public ArrayList Parse() throws ServiceException, ParseException {
        try {
            return DoParse();
        } catch(TokenMgrError e) {
            throw new ParseException(e.getMessage());
        }
    }

    public static final void AddClause(ArrayList clauses, ZimbraQuery.BaseQuery q)
    {
        if (null != q) {
            if (clauses.size() > 0) {
                ZimbraQuery.BaseQuery prev = (ZimbraQuery.BaseQuery)clauses.get(clauses.size()-1);
                assert(prev.getNext() == null);
                prev.setNext(q);
            }
            clauses.add(q);
        }
    }

//////////////////////////////////////////////////////////////////////
///
/// Parser States
///
  final public ZimbraQuery.BaseQuery Conjunction() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case AND_TOKEN:
      jj_consume_token(AND_TOKEN);
                  {if (true) return new ZimbraQuery.ConjQuery(mAnalyzer, AND_TOKEN);}
      break;
    case OR_TOKEN:
      jj_consume_token(OR_TOKEN);
                  {if (true) return new ZimbraQuery.ConjQuery(mAnalyzer, OR_TOKEN);}
      break;
    default:
      jj_la1[0] = jj_gen;
      {if (true) return new ZimbraQuery.ConjQuery(mAnalyzer, AND_TOKEN);}
    }
    throw new Error("Missing return statement in function");
  }

  final public int Modifier() throws ParseException {
    Token mod = null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case PLUS:
      jj_consume_token(PLUS);
             {if (true) return PLUS;}
      break;
    case MINUS:
      jj_consume_token(MINUS);
                 {if (true) return MINUS;}
      break;
    case NOT_TOKEN:
      jj_consume_token(NOT_TOKEN);
                     {if (true) return MINUS;}
      break;
    default:
      jj_la1[1] = jj_gen;

        {if (true) return 0;}
    }
    throw new Error("Missing return statement in function");
  }

  final public int DateModifier() throws ParseException {
    Token mod = null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case NOT_TOKEN:
      jj_consume_token(NOT_TOKEN);
                  {if (true) return NOT_TOKEN;}
      break;
    default:
      jj_la1[2] = jj_gen;

        {if (true) return 0;}
    }
    throw new Error("Missing return statement in function");
  }

/***
 *
 * Text target but after we have a target (thing to the left of the :...)
 *
 **/
  final public ZimbraQuery.BaseQuery Rhs_Text(int target) throws ParseException, ServiceException {
    ArrayList clauses = new ArrayList();
    Token t;
    int modifier = 0;
    ZimbraQuery.BaseQuery clause = null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case LPAREN:
      jj_consume_token(LPAREN);
      modifier = Modifier();
      clause = Rhs_Text(target);
                                                               clause.setModifier(modifier);  AddClause(clauses,clause);
      label_1:
      while (true) {
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case AND_TOKEN:
        case OR_TOKEN:
        case NOT_TOKEN:
        case LPAREN:
        case PLUS:
        case MINUS:
        case TEXT_TOK:
        case QUOTED_TOK:
          ;
          break;
        default:
          jj_la1[3] = jj_gen;
          break label_1;
        }
        clause = Conjunction();
                                   AddClause(clauses,clause);
        modifier = Modifier();
        clause = Rhs_Text(target);
                                                          clause.setModifier(modifier); AddClause(clauses,clause);
      }
      jj_consume_token(RPAREN);
                       {if (true) return new ZimbraQuery.SubQuery(mAnalyzer, 0,clauses);}
      break;
    case TEXT_TOK:
    case QUOTED_TOK:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case TEXT_TOK:
        t = jj_consume_token(TEXT_TOK);
        break;
      case QUOTED_TOK:
        t = jj_consume_token(QUOTED_TOK);
        break;
      default:
        jj_la1[4] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
                                           {if (true) return GetQuery(modifier, target, t.image);}
      break;
    default:
      jj_la1[5] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

/***
 *
 * item:  target
 *
 **/
  final public ZimbraQuery.BaseQuery Rhs_Item(int target) throws ParseException, ServiceException {
    ArrayList clauses = new ArrayList();
    Token t;
    int modifier = 0;
    ZimbraQuery.BaseQuery clause = null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case LPAREN:
      jj_consume_token(LPAREN);
      modifier = Modifier();
      clause = Rhs_Text(target);
                                                               clause.setModifier(modifier);  AddClause(clauses,clause);
      label_2:
      while (true) {
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case AND_TOKEN:
        case OR_TOKEN:
        case NOT_TOKEN:
        case LPAREN:
        case PLUS:
        case MINUS:
        case TEXT_TOK:
        case BRACES_TOK:
        case QUOTED_TOK:
          ;
          break;
        default:
          jj_la1[6] = jj_gen;
          break label_2;
        }
        clause = Conjunction();
                                   AddClause(clauses,clause);
        modifier = Modifier();
        clause = Rhs_Item(target);
                                                          clause.setModifier(modifier); AddClause(clauses,clause);
      }
      jj_consume_token(RPAREN);
                       {if (true) return new ZimbraQuery.SubQuery(mAnalyzer, 0,clauses);}
      break;
    case TEXT_TOK:
    case BRACES_TOK:
    case QUOTED_TOK:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case TEXT_TOK:
        t = jj_consume_token(TEXT_TOK);
        break;
      case QUOTED_TOK:
        t = jj_consume_token(QUOTED_TOK);
        break;
      case BRACES_TOK:
        t = jj_consume_token(BRACES_TOK);
        break;
      default:
        jj_la1[7] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
                                                          {if (true) return GetQuery(modifier, target, t.image);}
      break;
    default:
      jj_la1[8] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

/***
 *
 * special date target (because dates allow starting - (minus) signs, and we don't want to interpret those as a not, like
 * we do in other cases
 *
 **/
  final public ZimbraQuery.BaseQuery Rhs_Date(int target) throws ParseException, ServiceException {
    ArrayList clauses = new ArrayList();
    Token t,u;
    int modifier = 0;
    ZimbraQuery.BaseQuery clause = null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case LPAREN:
      jj_consume_token(LPAREN);
      modifier = DateModifier();
      clause = Rhs_Date(target);
                                                                   if (modifier == NOT_TOKEN) { clause.setModifier(MINUS); } AddClause(clauses,clause);
      label_3:
      while (true) {
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case AND_TOKEN:
        case OR_TOKEN:
        case NOT_TOKEN:
        case LPAREN:
        case PLUS:
        case MINUS:
        case TEXT_TOK:
        case QUOTED_TOK:
          ;
          break;
        default:
          jj_la1[9] = jj_gen;
          break label_3;
        }
        clause = Conjunction();
                                   AddClause(clauses,clause);
        modifier = DateModifier();
        clause = Rhs_Date(target);
                                                              if (modifier == NOT_TOKEN) { clause.setModifier(MINUS); } AddClause(clauses,clause);
      }
      jj_consume_token(RPAREN);
                       {if (true) return new ZimbraQuery.SubQuery(mAnalyzer, 0,clauses);}
      break;
    case PLUS:
    case MINUS:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case MINUS:
        u = jj_consume_token(MINUS);
        break;
      case PLUS:
        u = jj_consume_token(PLUS);
        break;
      default:
        jj_la1[10] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case TEXT_TOK:
        t = jj_consume_token(TEXT_TOK);
        break;
      case QUOTED_TOK:
        t = jj_consume_token(QUOTED_TOK);
        break;
      default:
        jj_la1[11] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
                                                               {if (true) return GetQuery(0, target, u.image+t.image);}
      break;
    case TEXT_TOK:
    case QUOTED_TOK:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case TEXT_TOK:
        t = jj_consume_token(TEXT_TOK);
        break;
      case QUOTED_TOK:
        t = jj_consume_token(QUOTED_TOK);
        break;
      default:
        jj_la1[12] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
                                           {if (true) return GetQuery(0, target, t.image);}
      break;
    default:
      jj_la1[13] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

//////////////////////////////
//
// Main grammar
//
  final public ZimbraQuery.BaseQuery Clause() throws ParseException, ServiceException {
    Token t = null;

    ZimbraQuery.BaseQuery q = null;
    ArrayList subExp = null;
    int target;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case LPAREN:
      jj_consume_token(LPAREN);
      subExp = Query();
      jj_consume_token(RPAREN);
                                       {if (true) return new ZimbraQuery.SubQuery(mAnalyzer, 0, subExp);}
      break;
    case CONTENT:
    case SUBJECT:
    case FROM:
    case MSGID:
    case ENVTO:
    case ENVFROM:
    case TO:
    case CC:
    case IN:
    case INID:
    case HAS:
    case FILENAME:
    case TYPE:
    case ATTACHMENT:
    case IS:
    case DATE:
    case DAY:
    case WEEK:
    case MONTH:
    case YEAR:
    case AFTER:
    case BEFORE:
    case SIZE:
    case BIGGER:
    case SMALLER:
    case TAG:
    case MESSAGE:
    case MY:
    case CONV:
    case CONV_COUNT:
    case CONV_MINM:
    case CONV_MAXM:
    case CONV_START:
    case CONV_END:
    case AUTHOR:
    case TITLE:
    case KEYWORDS:
    case COMPANY:
    case METADATA:
    case ITEM:
    case TEXT_TOK:
    case QUOTED_TOK:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case FROM:
      case MSGID:
      case ENVTO:
      case ENVFROM:
      case TO:
      case CC:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case MSGID:
          t = jj_consume_token(MSGID);
          break;
        case ENVFROM:
          t = jj_consume_token(ENVFROM);
          break;
        case ENVTO:
          t = jj_consume_token(ENVTO);
          break;
        case FROM:
          t = jj_consume_token(FROM);
          break;
        case TO:
          t = jj_consume_token(TO);
          break;
        case CC:
          t = jj_consume_token(CC);
          break;
        default:
          jj_la1[14] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        q = Rhs_Text(t.kind);
        break;
      case ITEM:
        t = jj_consume_token(ITEM);
        q = Rhs_Item(t.kind);
        break;
      case CONTENT:
      case SUBJECT:
      case IN:
      case INID:
      case HAS:
      case FILENAME:
      case TYPE:
      case ATTACHMENT:
      case MESSAGE:
      case AUTHOR:
      case TITLE:
      case KEYWORDS:
      case COMPANY:
      case METADATA:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case CONTENT:
          t = jj_consume_token(CONTENT);
          break;
        case MESSAGE:
          t = jj_consume_token(MESSAGE);
          break;
        case SUBJECT:
          t = jj_consume_token(SUBJECT);
          break;
        case IN:
          t = jj_consume_token(IN);
          break;
        case INID:
          t = jj_consume_token(INID);
          break;
        case TYPE:
          t = jj_consume_token(TYPE);
          break;
        case ATTACHMENT:
          t = jj_consume_token(ATTACHMENT);
          break;
        case HAS:
          t = jj_consume_token(HAS);
          break;
        case FILENAME:
          t = jj_consume_token(FILENAME);
          break;
        case AUTHOR:
          t = jj_consume_token(AUTHOR);
          break;
        case TITLE:
          t = jj_consume_token(TITLE);
          break;
        case KEYWORDS:
          t = jj_consume_token(KEYWORDS);
          break;
        case COMPANY:
          t = jj_consume_token(COMPANY);
          break;
        case METADATA:
          t = jj_consume_token(METADATA);
          break;
        default:
          jj_la1[15] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        q = Rhs_Text(t.kind);
        break;
      case IS:
      case TAG:
      case MY:
      case CONV:
      case CONV_COUNT:
      case CONV_MINM:
      case CONV_MAXM:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case MY:
          t = jj_consume_token(MY);
          break;
        case IS:
          t = jj_consume_token(IS);
          break;
        case TAG:
          t = jj_consume_token(TAG);
          break;
        case CONV:
          t = jj_consume_token(CONV);
          break;
        case CONV_COUNT:
          t = jj_consume_token(CONV_COUNT);
          break;
        case CONV_MINM:
          t = jj_consume_token(CONV_MINM);
          break;
        case CONV_MAXM:
          t = jj_consume_token(CONV_MAXM);
          break;
        default:
          jj_la1[16] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        q = Rhs_Text(t.kind);
        break;
      case DATE:
      case DAY:
      case WEEK:
      case MONTH:
      case YEAR:
      case AFTER:
      case BEFORE:
      case CONV_START:
      case CONV_END:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case DATE:
          t = jj_consume_token(DATE);
          break;
        case DAY:
          t = jj_consume_token(DAY);
          break;
        case MONTH:
          t = jj_consume_token(MONTH);
          break;
        case WEEK:
          t = jj_consume_token(WEEK);
          break;
        case YEAR:
          t = jj_consume_token(YEAR);
          break;
        case AFTER:
          t = jj_consume_token(AFTER);
          break;
        case BEFORE:
          t = jj_consume_token(BEFORE);
          break;
        case CONV_START:
          t = jj_consume_token(CONV_START);
          break;
        case CONV_END:
          t = jj_consume_token(CONV_END);
          break;
        default:
          jj_la1[17] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        q = Rhs_Date(t.kind);
        break;
      case SIZE:
      case BIGGER:
      case SMALLER:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case SIZE:
          t = jj_consume_token(SIZE);
          break;
        case BIGGER:
          t = jj_consume_token(BIGGER);
          break;
        case SMALLER:
          t = jj_consume_token(SMALLER);
          break;
        default:
          jj_la1[18] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        q = Rhs_Text(t.kind);
        break;
      case TEXT_TOK:
      case QUOTED_TOK:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case TEXT_TOK:
          t = jj_consume_token(TEXT_TOK);
          break;
        case QUOTED_TOK:
          t = jj_consume_token(QUOTED_TOK);
          break;
        default:
          jj_la1[19] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
                                              {if (true) return GetQuery(0,CONTENT,t.image);}
        break;
      default:
        jj_la1[20] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
        {if (true) return q;}
      break;
    default:
      jj_la1[21] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public void SortBy() throws ParseException {
    Token t = null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case SORTBY:
    case SORT:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case SORTBY:
        jj_consume_token(SORTBY);
        break;
      case SORT:
        jj_consume_token(SORT);
        break;
      default:
        jj_la1[22] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      t = jj_consume_token(TEXT_TOK);
                                    mSortByStr = t.image;
      break;
    default:
      jj_la1[23] = jj_gen;

    }
  }

  final public ArrayList Query() throws ParseException, ServiceException {
    ZimbraQuery.BaseQuery clause = null;
    ArrayList clauses = new ArrayList();
    int modifier;
    SortBy();
    modifier = Modifier();
    clause = Clause();
    SortBy();
                                                            if (clause != null) { clause.setModifier(modifier); AddClause(clauses,clause); }
    label_4:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case AND_TOKEN:
      case OR_TOKEN:
      case NOT_TOKEN:
      case LPAREN:
      case CONTENT:
      case SUBJECT:
      case FROM:
      case MSGID:
      case ENVTO:
      case ENVFROM:
      case TO:
      case CC:
      case IN:
      case INID:
      case HAS:
      case FILENAME:
      case TYPE:
      case ATTACHMENT:
      case IS:
      case DATE:
      case DAY:
      case WEEK:
      case MONTH:
      case YEAR:
      case AFTER:
      case BEFORE:
      case SIZE:
      case BIGGER:
      case SMALLER:
      case TAG:
      case MESSAGE:
      case MY:
      case CONV:
      case CONV_COUNT:
      case CONV_MINM:
      case CONV_MAXM:
      case CONV_START:
      case CONV_END:
      case AUTHOR:
      case TITLE:
      case KEYWORDS:
      case COMPANY:
      case METADATA:
      case ITEM:
      case PLUS:
      case MINUS:
      case TEXT_TOK:
      case QUOTED_TOK:
        ;
        break;
      default:
        jj_la1[24] = jj_gen;
        break label_4;
      }
      clause = Conjunction();
                               AddClause(clauses,clause);
      modifier = Modifier();
      clause = Clause();
      SortBy();
                                                       if (clause != null) { clause.setModifier(modifier); AddClause(clauses,clause); }
      SortBy();

    }

        {if (true) return clauses;}
    throw new Error("Missing return statement in function");
  }

  final public ArrayList DoParse() throws ParseException, ServiceException {
    ArrayList clauses;
    clauses = Query();
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 0:
    case 65:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 65:
        jj_consume_token(65);
        break;
      case 0:
        jj_consume_token(0);
        break;
      default:
        jj_la1[25] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    default:
      jj_la1[26] = jj_gen;
      ;
    }
        {if (true) return clauses;}
    throw new Error("Missing return statement in function");
  }

  public ZimbraQueryParserTokenManager token_source;
  JavaCharStream jj_input_stream;
  public Token token, jj_nt;
  private int jj_ntk;
  private int jj_gen;
  final private int[] jj_la1 = new int[27];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static private int[] jj_la1_2;
  static {
      jj_la1_0();
      jj_la1_1();
      jj_la1_2();
   }
   private static void jj_la1_0() {
      jj_la1_0 = new int[] {0x60,0x80,0x80,0x1e0,0x0,0x100,0x1e0,0x0,0x100,0x1e0,0x0,0x0,0x0,0x100,0x3f000,0xfc0c00,0x1000000,0xfe000000,0x0,0x0,0xfffffc00,0xfffffd00,0x0,0x0,0xfffffde0,0x1,0x1,};
   }
   private static void jj_la1_1() {
      jj_la1_1 = new int[] {0x0,0xc00000,0x0,0x1c00000,0x1000000,0x1000000,0x21c00000,0x21000000,0x21000000,0x1c00000,0xc00000,0x1000000,0x1000000,0x1c00000,0x0,0x7c040,0xfa0,0x3000,0x13,0x1000000,0x10ffff3,0x10ffff3,0x300000,0x300000,0x1cffff3,0x0,0x0,};
   }
   private static void jj_la1_2() {
      jj_la1_2 = new int[] {0x0,0x0,0x0,0x1,0x1,0x1,0x1,0x1,0x1,0x1,0x0,0x1,0x1,0x1,0x0,0x0,0x0,0x0,0x0,0x1,0x1,0x1,0x0,0x0,0x1,0x2,0x2,};
   }

  public ZimbraQueryParser(java.io.InputStream stream) {
    jj_input_stream = new JavaCharStream(stream, 1, 1);
    token_source = new ZimbraQueryParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 27; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.InputStream stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 27; i++) jj_la1[i] = -1;
  }

  public ZimbraQueryParser(java.io.Reader stream) {
    jj_input_stream = new JavaCharStream(stream, 1, 1);
    token_source = new ZimbraQueryParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 27; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 27; i++) jj_la1[i] = -1;
  }

  public ZimbraQueryParser(ZimbraQueryParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 27; i++) jj_la1[i] = -1;
  }

  public void ReInit(ZimbraQueryParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 27; i++) jj_la1[i] = -1;
  }

  final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  final private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.Vector jj_expentries = new java.util.Vector();
  private int[] jj_expentry;
  private int jj_kind = -1;

  public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[66];
    for (int i = 0; i < 66; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 27; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
          if ((jj_la1_1[i] & (1<<j)) != 0) {
            la1tokens[32+j] = true;
          }
          if ((jj_la1_2[i] & (1<<j)) != 0) {
            la1tokens[64+j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 66; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[])jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  final public void enable_tracing() {
  }

  final public void disable_tracing() {
  }

}
