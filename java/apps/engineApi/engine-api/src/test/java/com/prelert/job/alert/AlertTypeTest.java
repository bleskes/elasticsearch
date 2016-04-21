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

package com.prelert.job.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.EnumSet;

import org.junit.Test;

public class AlertTypeTest
{
    @Test
    public void testToString()
    {
        assertEquals("bucket", AlertType.BUCKET.toString());
        assertEquals("bucketinfluencer", AlertType.BUCKETINFLUENCER.toString());
        assertEquals("influencer", AlertType.INFLUENCER.toString());
    }

    @Test
    public void testFromString()
    {
        for (AlertType at : EnumSet.allOf(AlertType.class))
        {
            AlertType fromString =  AlertType.fromString(at.toString());
            assertSame(at,  fromString);
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFromString_GivenInvalid()
    {
        AlertType.fromString("invalid");
    }
}
