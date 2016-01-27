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

public class GreedyRangeSelector<T>
{
    private Long m_BucketSpan;

    private List<SortableRecord<T> > m_List;

    private Long m_LastIndex;

    public GreedyRangeSelector(Long bucketSpan)
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
        TreeSet<Long> times = new TreeSet<>();

        for (SortableRecord<T> record : m_List)
        {
            Long time = record.m_Time;
            // Is this item's time already taken within the tree?
            // If not, add to the list, add time to the tree
            if (times.contains(time))
            {
                newList.add(record);
            }
            else
            {
                Long timeAfter = times.ceiling(time);
                Long timeBefore = times.floor(time);

                if ((timeBefore == null || (timeBefore + m_BucketSpan <= time)) &&
                        (timeAfter == null || (timeAfter - m_BucketSpan) >= time))
                {
                    newList.add(record);
                    times.add(time);
                }
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




