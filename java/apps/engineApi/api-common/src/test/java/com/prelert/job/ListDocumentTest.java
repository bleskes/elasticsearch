/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
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
package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ListDocumentTest
{
    @Test
    public void testConstructor()
    {
        ListDocument list = new ListDocument("myList", Arrays.asList("a", "b"));
        assertEquals("myList", list.getId());
        assertEquals(Arrays.asList("a", "b"), list.getItems());
    }

    @Test
    public void testEquals_GivenNull()
    {
        ListDocument list = new ListDocument("myList", Arrays.asList("a", "b"));
        assertFalse(list.equals(null));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        ListDocument list = new ListDocument("myList", Arrays.asList("a", "b"));
        assertFalse(list.equals("a string"));
    }

    @Test
    public void testEquals_GivenSameReference()
    {
        ListDocument list = new ListDocument("myList", Arrays.asList("a", "b"));
        assertTrue(list.equals(list));
    }

    @Test
    public void testEquals_GivenDifferentId()
    {
        ListDocument list1 = new ListDocument("myList1", Arrays.asList("a", "b"));
        ListDocument list2 = new ListDocument("myList2", Arrays.asList("a", "b"));
        assertFalse(list1.equals(list2));
        assertFalse(list2.equals(list1));
    }

    @Test
    public void testEquals_GivenDifferentItems()
    {
        ListDocument list1 = new ListDocument("myList", Arrays.asList("a", "b"));
        ListDocument list2 = new ListDocument("myList", Arrays.asList("a", "c"));
        assertFalse(list1.equals(list2));
        assertFalse(list2.equals(list1));
    }

    @Test
    public void testEquals_GivenEqualList()
    {
        ListDocument list1 = new ListDocument("myList", Arrays.asList("a", "b"));
        ListDocument list2 = new ListDocument("myList", Arrays.asList("a", "b"));
        assertTrue(list1.equals(list2));
        assertTrue(list2.equals(list1));
        assertEquals(list1.hashCode(), list2.hashCode());
    }
}
