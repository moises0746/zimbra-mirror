/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
/*
 * Created on Sep 19, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.Arrays;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class ColorItem extends RedoableOp {

    private int[] mIds;
    private byte mType;
    private byte mColor;

    public ColorItem() { }

    public ColorItem(long mailboxId, int[] ids, byte type, byte color) {
        setMailboxId(mailboxId);
        mIds = ids;
        mType = type;
        mColor = color;
    }

    @Override public int getOpCode() {
        return OP_COLOR_ITEM;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(Arrays.toString(mIds)).append(", color=").append(mColor);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(-1);
        out.writeByte(mType);
        out.writeByte(mColor);
        out.writeInt(mIds == null ? 0 : mIds.length);
        if (mIds != null) {
            for (int i = 0; i < mIds.length; i++)
                out.writeInt(mIds[i]);
        }
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        int id = in.readInt();
        if (id > 0)
            mIds = new int[] { id };
        mType = in.readByte();
        mColor = in.readByte();
        if (id <= 0) {
            mIds = new int[in.readInt()];
            for (int i = 0; i < mIds.length; i++)
                mIds[i] = in.readInt();
        }
    }

    @Override public void redo() throws Exception {
        long mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailbox.setColor(getOperationContext(), mIds, mType, mColor);
    }
}
