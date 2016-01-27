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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import org.junit.Test;

public class GreedyRangeSelectorTest
{
    @Test
    public void testAddItems()
    {
        GreedyRangeSelector<String> results = new GreedyRangeSelector<>(10L);

        // Data is going to come out at half bucket intervals: 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150
        results.add("snow", 100.0, 150L);
        results.add("its", 90.0, 125L);
        results.add("a", 80.0, 110L);
        results.add("lamb", 70.0, 120L);
        results.add("little", 60.0, 115L);
        results.add("white", 50.0, 140L);
        results.add("fleece", 40.0, 130L);
        results.add("as", 30.0, 135L);
        results.add("had", 20.0, 105L);
        results.add("as", 10.0, 145L);        
        results.add("Mary", 0.0, 100L);

        ArrayList<String> expected = new ArrayList<>();
        expected.add("snow");
        expected.add("its");
        expected.add("a");
        expected.add("white");
        expected.add("Mary");

        assertEquals(expected, results.getList());
    }

    @Test
    public void testAddItemsUnsorted()
    {
        GreedyRangeSelector<String> results = new GreedyRangeSelector<>(10L);

        // Data is going to come out at half bucket intervals: 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150
        // The time-ordered sort should be preserved
        results.add("Mary", 0.0, 100L);
        results.add("had", 20.0, 105L);
        results.add("a", 80.0, 110L);
        results.add("little", 60.0, 115L);
        results.add("lamb", 70.0, 120L);
        results.add("its", 90.0, 125L);
        results.add("fleece", 40.0, 130L);
        results.add("as", 30.0, 135L);
        results.add("white", 50.0, 140L);
        results.add("as", 10.0, 145L);
        results.add("snow", 100.0, 150L);
        results.add("rain", 55.0, 150L);
        results.add("sleet", 120.0, 150L);
        
        ArrayList<String> expected = new ArrayList<>();
        expected.add("Mary");
        expected.add("a");
        expected.add("its");
        expected.add("white");
        expected.add("snow");
        expected.add("rain");
        expected.add("sleet");

        assertEquals(expected, results.getList());
    }

    @Test
    public void testAddItemsCoalesce()
    {
        GreedyCoalescingRangeSelector<String> results = new GreedyCoalescingRangeSelector<>(10L);
        results.add("snow", 100.0, 150L);
        results.add("its", 90.0, 125L);
        results.add("a", 80.0, 110L);
        results.add("lamb", 70.0, 120L);
        results.add("little", 60.0, 115L);
        results.add("white", 50.0, 140L);
        results.add("fleece", 40.0, 130L);
        results.add("as", 30.0, 135L);
        results.add("had", 20.0, 105L);
        results.add("as", 10.0, 145L);        
        results.add("Mary", 0.0, 100L);

        ArrayList<String> expected = new ArrayList<>();
        expected.add("snow");
        expected.add("its");
        expected.add("a");
        expected.add("white");
        expected.add("Mary");

        assertEquals(expected, results.getList());
    }

    @Test
    public void testAddItemsCoalesceUnsorted()
    {
        GreedyCoalescingRangeSelector<String> results = new GreedyCoalescingRangeSelector<>(10L);
        results.add("Mary", 0.0, 100L);
        results.add("had", 20.0, 105L);
        results.add("a", 80.0, 110L);
        results.add("little", 60.0, 115L);
        results.add("lamb", 70.0, 120L);
        results.add("its", 90.0, 125L);
        results.add("fleece", 40.0, 130L);
        results.add("as", 30.0, 135L);
        results.add("white", 50.0, 140L);
        results.add("as", 10.0, 145L);
        results.add("snow", 100.0, 150L);

        ArrayList<String> expected = new ArrayList<>();
        expected.add("Mary");
        expected.add("a");
        expected.add("its");
        expected.add("white");
        expected.add("snow");

        assertEquals(expected, results.getList());
    }
}
