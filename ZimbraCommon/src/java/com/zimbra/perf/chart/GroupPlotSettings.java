/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013 Zimbra Software, LLC.
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

package com.zimbra.perf.chart;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GroupPlotSettings extends PlotSettings {

    private final String mGroupBy;
    private final String mIgnore;
    private final Set<String> mIgnoreSet;
    public GroupPlotSettings(String groupBy, String ignore, String infile,
            String dataCol, boolean showRaw,
            boolean showMovingAvg, int movingAvgPoints,
            double multiplier, double divisor,
            boolean nonNegative, boolean percentTime,
            String dataFunction, String aggFunction,
            boolean optional) {
        super(groupBy, infile, dataCol, showRaw, showMovingAvg,
                movingAvgPoints, multiplier, divisor, nonNegative,
                percentTime, dataFunction, aggFunction, optional, null, null);
        mGroupBy = groupBy;
        mIgnore = ignore;
        mIgnoreSet = new HashSet<String>();
        if (mIgnore != null) {
            String[] ignores = mIgnore.split("\\s*,\\s*");
            if (ignores.length > 0)
                mIgnoreSet.addAll(Arrays.asList(ignores));
        }
    }

    public String getGroupBy() {
        return mGroupBy;
    }
    
    public String getIgnore() {
        return mIgnore;
    }
    
    public Set<String> getIgnoreSet() {
        return mIgnoreSet;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<groupplot\n");
        sb.append("  ").append("legend=\"").append(getLegend()).append("\"\n");
        sb.append("  ").append("infile=\"").append(getInfile()).append("\"\n");
        sb.append("  ").append("data=\"").append(getDataColumn()).append("\"\n");
        sb.append("  ").append("showRaw=\"").append(getShowRaw()).append("\"\n");
        sb.append("  ").append("showMovingAvg=\"").append(getShowMovingAvg()).append("\"\n");
        sb.append("  ").append("movingAvgPoints=\"").append(getMovingAvgPoints()).append("\"\n");
        sb.append("  ").append("multiplier=\"").append(getMultiplier()).append("\"\n");
        sb.append("  ").append("divisor=\"").append(getDivisor()).append("\"\n");
        sb.append("  ").append("nonNegative=\"").append(getNonNegative()).append("\"\n");
        sb.append("  ").append("percentTime=\"").append(getPercentTime()).append("\"\n");
        sb.append("  ").append("dataFunction=\"").append(getDataFunction()).append("\"\n");
        sb.append("  ").append("aggregateFunction=\"").append(getAggregateFunction()).append("\"\n");
        sb.append("  ").append("optional=\"").append(getOptional()).append("\"\n");
        sb.append("  ").append("groupBy=\"").append(mGroupBy).append("\"\n");
        sb.append("  ").append("ignore=\"").append(mIgnore).append("\"\n");
        sb.append("/>\n");
        return sb.toString();
    }
}
