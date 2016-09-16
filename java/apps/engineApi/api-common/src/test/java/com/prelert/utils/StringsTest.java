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
package com.prelert.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringsTest
{
    @Test
    public void testIsNullOrEmpty_GivenNull()
    {
        assertTrue(Strings.isNullOrEmpty(null));
    }

    @Test
    public void testIsNullOrEmpty_GivenEmpty()
    {
        assertTrue(Strings.isNullOrEmpty(""));
    }

    @Test
    public void testIsNullOrEmpty_GivenNonEmpty()
    {
        assertFalse(Strings.isNullOrEmpty(" "));
    }

    @Test
    public void testNullToEmpty_GivenNull()
    {
        assertEquals("", Strings.nullToEmpty(null));
    }

    @Test
    public void testNullToEmpty_GivenNonNull()
    {
        assertEquals("a", Strings.nullToEmpty("a"));
    }
}
