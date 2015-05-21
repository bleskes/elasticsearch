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
package com.prelert.transforms;

import static org.junit.Assert.*;

import org.junit.Test;

import com.prelert.job.transform.TransformConfigurationException;

public class OperationTest {

    @Test
    public void testFromString() throws TransformConfigurationException
    {
        assertEquals(Operation.fromString("gt"), Operation.GT);
        assertEquals(Operation.fromString("Gt"), Operation.GT);
        assertEquals(Operation.fromString("EQ"), Operation.EQ);
        assertEquals(Operation.fromString("eq"), Operation.EQ);
        assertEquals(Operation.fromString("lte"), Operation.LTE);
        assertEquals(Operation.fromString("lt"), Operation.LT);
        assertEquals(Operation.fromString("GTE"), Operation.GTE);
    }

    @Test
    public void testTest()
    {
        assertTrue(Operation.GT.test(1.0, 0.0));
        assertFalse(Operation.GT.test(0.0, 1.0));

        assertTrue(Operation.GTE.test(1.0, 0.0));
        assertTrue(Operation.GTE.test(1.0, 1.0));
        assertFalse(Operation.GTE.test(0.0, 1.0));

        assertTrue(Operation.EQ.test(0.0, 0.0));
        assertFalse(Operation.EQ.test(1.0, 0.0));

        assertTrue(Operation.LT.test(0.0, 1.0));
        assertFalse(Operation.LT.test(0.0, 0.0));

        assertTrue(Operation.LTE.test(0.0, 1.0));
        assertTrue(Operation.LTE.test(1.0, 1.0));
        assertFalse(Operation.LTE.test(1.0, 0.0));
    }

}
