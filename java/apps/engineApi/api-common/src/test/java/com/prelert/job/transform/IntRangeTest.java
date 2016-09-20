/****************************************************************************
 *                                                                          *
 * Copyright 2016-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package com.prelert.job.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IntRangeTest
{
    @Test
    public void testSingleton_GivenThree()
    {
        IntRange range = IntRange.singleton(3);

        assertTrue(range.hasLowerBound());
        assertEquals(3, range.lower());
        assertTrue(range.hasUpperBound());
        assertEquals(3, range.upper());
        assertFalse(range.contains(2));
        assertTrue(range.contains(3));
        assertFalse(range.contains(4));
        assertEquals("[3‥3]", range.toString());
    }

    @Test
    public void testSingleton_GivenFour()
    {
        IntRange range = IntRange.singleton(4);

        assertTrue(range.hasLowerBound());
        assertEquals(4, range.lower());
        assertTrue(range.hasUpperBound());
        assertEquals(4, range.upper());
        assertFalse(range.contains(3));
        assertTrue(range.contains(4));
        assertFalse(range.contains(5));
        assertEquals("[4‥4]", range.toString());
    }

    @Test
    public void testClosed()
    {
        IntRange range = IntRange.closed(2, 5);
        assertTrue(range.hasLowerBound());
        assertEquals(2, range.lower());
        assertTrue(range.hasUpperBound());
        assertEquals(5, range.upper());
        assertFalse(range.contains(1));
        assertTrue(range.contains(2));
        assertTrue(range.contains(3));
        assertTrue(range.contains(4));
        assertTrue(range.contains(5));
        assertFalse(range.contains(6));
        assertEquals("[2‥5]", range.toString());
    }

    @Test
    public void testOpen()
    {
        IntRange range = IntRange.open(2, 5);
        assertTrue(range.hasLowerBound());
        assertEquals(2, range.lower());
        assertTrue(range.hasUpperBound());
        assertEquals(5, range.upper());
        assertFalse(range.contains(1));
        assertFalse(range.contains(2));
        assertTrue(range.contains(3));
        assertTrue(range.contains(4));
        assertFalse(range.contains(5));
        assertFalse(range.contains(6));
        assertEquals("(2‥5)", range.toString());
    }

    @Test
    public void testClosedOpen()
    {
        IntRange range = IntRange.closedOpen(2, 5);
        assertTrue(range.hasLowerBound());
        assertEquals(2, range.lower());
        assertTrue(range.hasUpperBound());
        assertEquals(5, range.upper());
        assertFalse(range.contains(1));
        assertTrue(range.contains(2));
        assertTrue(range.contains(3));
        assertTrue(range.contains(4));
        assertFalse(range.contains(5));
        assertFalse(range.contains(6));
        assertEquals("[2‥5)", range.toString());
    }

    @Test
    public void testOpenClosed()
    {
        IntRange range = IntRange.openClosed(2, 5);
        assertTrue(range.hasLowerBound());
        assertEquals(2, range.lower());
        assertTrue(range.hasUpperBound());
        assertEquals(5, range.upper());
        assertFalse(range.contains(1));
        assertFalse(range.contains(2));
        assertTrue(range.contains(3));
        assertTrue(range.contains(4));
        assertTrue(range.contains(5));
        assertFalse(range.contains(6));
        assertEquals("(2‥5]", range.toString());
    }

    @Test
    public void testAtLeast()
    {
        IntRange range = IntRange.atLeast(42);
        assertTrue(range.hasLowerBound());
        assertEquals(42, range.lower());
        assertFalse(range.hasUpperBound());
        assertEquals(Integer.MAX_VALUE, range.upper());
        assertFalse(range.contains(41));
        assertTrue(range.contains(42));
        assertTrue(range.contains(43));
        assertTrue(range.contains(1044));
        assertTrue(range.contains(Integer.MAX_VALUE));
        assertEquals("[42‥+∞)", range.toString());
    }
}
