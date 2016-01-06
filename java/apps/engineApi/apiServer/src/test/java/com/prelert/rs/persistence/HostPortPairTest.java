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

package com.prelert.rs.persistence;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HostPortPairTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testOfList_GivenOneValidPair()
    {
        List<HostPortPair> pairs = HostPortPair.ofList("localhost:9300");
        assertEquals(1, pairs.size());
        assertEquals("localhost", pairs.get(0).getHost().getHostName());
        assertEquals(9300, pairs.get(0).getPort());
    }

    @Test
    public void testOfList_GivenTwoValidPairs()
    {
        List<HostPortPair> pairs = HostPortPair.ofList("0.0.0.0:9300,0.0.0.1:9400");
        assertEquals(2, pairs.size());
        assertEquals("0.0.0.0", pairs.get(0).getHost().getHostAddress());
        assertEquals(9300, pairs.get(0).getPort());
        assertEquals("0.0.0.1", pairs.get(1).getHost().getHostAddress());
        assertEquals(9400, pairs.get(1).getPort());
    }

    @Test
    public void testOfList_GivenInvalidHost()
    {
        m_ExpectedException.expect(IllegalArgumentException.class);
        m_ExpectedException.expectMessage(
                "Failed to parse host: 'invalid host' cannot be resolved.");

        HostPortPair.ofList("localhost:9300,invalid host:9400");
    }

    @Test
    public void testOfList_GivenInvalidPort()
    {
        m_ExpectedException.expect(IllegalArgumentException.class);
        m_ExpectedException.expectMessage(
                "Failed to parse port: 'invalidPort' is not a valid port number.");

        HostPortPair.ofList("localhost:invalidPort");
    }

    @Test
    public void testOfList_GivenInvalidHostPortPair()
    {
        m_ExpectedException.expect(IllegalArgumentException.class);
        m_ExpectedException.expectMessage(
                "Failed to parse host/port pair: 'localhost|9300' is not in the form <address:port>.");

        HostPortPair.ofList("localhost:9300,localhost|9300");
    }
}
