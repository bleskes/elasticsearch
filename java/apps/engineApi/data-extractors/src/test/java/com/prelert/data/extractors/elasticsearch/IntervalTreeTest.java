/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.data.extractors.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Range;
import com.google.common.collect.Sets;

public class IntervalTreeTest
{
    private static final long MAX_LOWER_BOUND = 20000;
    private static final long MAX_RANGE_SPAN = 1000;

    @Test
    public void testEmptyTree()
    {
        IntervalTree<Long, String> tree = new IntervalTree<>();
        assertTrue(tree.isEmpty());
        assertEquals(0, tree.height());
        assertTrue(tree.getIntersectingValues(Range.closed(Long.MIN_VALUE, Long.MAX_VALUE)).isEmpty());
    }

    @Test
    public void testClear()
    {
        IntervalTree<Long, String> tree = new IntervalTree<>();
        tree.put(Range.closed(42L, 142L), "foo");
        assertFalse(tree.isEmpty());
        assertEquals(1, tree.height());

        tree.clear();

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.height());
    }

    @Test
    public void testGetIntersectingValues_GivenOverlappingIntervalsWithDisconnect()
    {
        IntervalTree<Long, Integer> tree = new IntervalTree<>();

        tree.put(Range.closed(0L, 4L), 1);
        tree.put(Range.closed(1L, 1L), 2);
        tree.put(Range.closed(1L, 5L), 3);
        tree.put(Range.closed(1L, 3L), 4);
        tree.put(Range.closed(2L, 4L), 5);
        tree.put(Range.closed(5L, 9L), 6);

        Set<Integer> expected = Sets.newHashSet(1, 3, 4, 5);
        Set<Integer> actual = new HashSet<>();

        actual.addAll(tree.getIntersectingValues(Range.closedOpen(2L, 5L)));

        assertEquals(expected, actual);
    }

    @Test
    public void testGetIntersectingValues_GivenDuplicateIntervals()
    {
        IntervalTree<Long, Integer> tree = new IntervalTree<>();

        tree.put(Range.closed(1L, 2L), 1);
        tree.put(Range.closed(1L, 2L), 2);
        tree.put(Range.closed(1L, 2L), 3);
        tree.put(Range.closed(5L, 5L), 4);
        tree.put(Range.closed(5L, 5L), 5);

        Set<Integer> expected = Sets.newHashSet(1, 2, 3, 4, 5);
        Set<Integer> actual = new HashSet<>();

        actual.addAll(tree.getIntersectingValues(Range.closedOpen(1L, 6L)));

        assertEquals(expected, actual);
    }

    @Test
    public void testGetIntersectingValues_GivenRandomValues()
    {
        Random random = new Random(5319741);
        int runsCount = 20;
        String valueTemplate = "foo_%s";

        for (int runId = 0; runId < runsCount; runId++)
        {
            int size = random.nextInt(400);
            Map<String, Range<Long>> rangeByValue = new HashMap<>();
            IntervalTree<Long, String> tree = new IntervalTree<>();

            for (int i = 0; i < size; i++)
            {
                String value = String.format(valueTemplate, i);
                Range<Long> range = createRange(random);
                rangeByValue.put(value, range);
                tree.put(range, value);
            }

            Range<Long> maxRange = Range.closed(0L, MAX_LOWER_BOUND + MAX_RANGE_SPAN);
            assertEquals(size, tree.getIntersectingValues(maxRange).size());

            int searches = 10000;
            for (int i = 0; i < searches; i++)
            {
                Set<String> linearResult = new HashSet<>();
                Set<String> treeResult = new HashSet<>();
                Range<Long> targetRange = createRange(random);

                // First the linear search to provide a straight-forward baseline
                for (Entry<String, Range<Long>> entry : rangeByValue.entrySet())
                {
                    Range<Long> range = entry.getValue();
                    if (range.isConnected(targetRange) && !range.intersection(targetRange).isEmpty())
                    {
                        linearResult.add(entry.getKey());
                    }
                }

                treeResult.addAll(tree.getIntersectingValues(targetRange));

                assertEquals(linearResult, treeResult);
            }
        }
    }

    @Test
    public void testTreeHeightRespectsRedBlackTreeUpperBound()
    {
        // Guards that the tree is balanced by checking its height is never
        // greater than the red-black trees upper bound of 2 * log(n + 1).
        // This test also serves as a performance guard.

        int runsCount = 100;
        Random random = new Random(42);
        for (int runId = 0; runId < runsCount; runId++)
        {
            IntervalTree<Long, String> tree = new IntervalTree<>();
            int treeSize = random.nextInt(20000) + 1;

            for (int i = 0; i < treeSize; i++)
            {
                tree.put(createRange(random), "foo");
            }
            int maxHeightAllowed = (int) (2 * Math.log(treeSize + 1));
            int treeHeight = tree.height();
            assertTrue(treeHeight > 0);
            assertTrue(treeHeight <= maxHeightAllowed);
        }
    }

    private static Range<Long> createRange(Random random)
    {
        long lower = random.nextInt((int) MAX_LOWER_BOUND);
        long upper = lower + random.nextInt((int) MAX_RANGE_SPAN);
        return Range.closed(lower, upper);
    }
}
