/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.rs.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class GreedyCoalescingRangeSelector<T>
{
    private Long m_BucketSpan;

    private List<SortableRecord<T> > m_List;

    private Long m_LastIndex;

    public GreedyCoalescingRangeSelector(Long bucketSpan)
    {
        m_BucketSpan = bucketSpan;
        m_List = new ArrayList<>();
        m_LastIndex = 0L;
    }

    public void add(T item, double score, Long time)
    {
        m_List.add(new SortableRecord<T>(score, m_LastIndex++, item, time));
    }

    public List<T> getList()
    {
        // Sort the records by score descending
        Collections.sort(m_List, (r1, r2) -> r2.m_Score.compareTo(r1.m_Score));

        List<SortableRecord<T> > newList = new ArrayList<>();
        TreeSet<TimeRange> times = new TreeSet<>();

        for (SortableRecord<T> record : m_List)
        {
            Long time = record.m_Time;

            // Is this item's time already taken within the tree?
            // If not, add to the list, add time to the tree
            TimeRange incoming = new TimeRange(time, time);
            TimeRange timeAfter = times.ceiling(incoming);
            TimeRange timeBefore = times.floor(incoming);

            // Can only add if we are between the two ranges
            if ((timeBefore == null || (timeBefore.getRight() + m_BucketSpan <= time)) &&
                    (timeAfter == null || (timeAfter.getLeft() - m_BucketSpan) >= time))
            {
                newList.add(record);
                
                // Decide whether to add a new entry, or coalesce existing entries
                
                // Merge right?
                if ((timeAfter != null) && (timeAfter.getLeft() - (m_BucketSpan * 1.5) <= time))
                {
                    times.remove(timeAfter);
                    incoming.setRight(timeAfter.getRight());            
                }
                
                // Merge left?
                if ((timeBefore != null) && (timeBefore.getRight() + (m_BucketSpan * 0.5) >= time))
                {
                    times.remove(timeBefore);
                    incoming.setLeft(timeBefore.getLeft());
                }
                
                times.add(incoming);
            }
        }

        // Sort the records by their original index
        Collections.sort(newList, (r1, r2) -> r1.m_OriginalIndex.compareTo(r2.m_OriginalIndex));

        List<T> finalList = new ArrayList<>();
        for (SortableRecord<T> record : newList)
        {
            finalList.add(record.m_Record);
        }
        return finalList;
    }
}

