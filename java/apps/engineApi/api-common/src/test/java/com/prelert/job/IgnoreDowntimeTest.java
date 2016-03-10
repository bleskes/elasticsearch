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

package com.prelert.job;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IgnoreDowntimeTest
{
    @Test
    public void testFromString_GivenLeadingWhitespace()
    {
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString(" \t ALWAYS"));
    }

    @Test
    public void testFromString_GivenTrailingWhitespace()
    {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("NEVER \t "));
    }

    @Test
    public void testFromString_GivenExactMatches()
    {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("NEVER"));
        assertEquals(IgnoreDowntime.ONCE, IgnoreDowntime.fromString("ONCE"));
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString("ALWAYS"));
    }

    @Test
    public void testFromString_GivenMixedCaseCharacters()
    {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("nevEr"));
        assertEquals(IgnoreDowntime.ONCE, IgnoreDowntime.fromString("oNce"));
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString("always"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFromString_GivenNonMatchingString()
    {
        IgnoreDowntime.fromString("nope");
    }
}
