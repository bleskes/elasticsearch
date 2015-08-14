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

package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.prelert.job.ModelDebugConfig;

public class ModelDebugConfigTest
{
    @Test
    public void testEquals()
    {
        assertFalse(new ModelDebugConfig().equals(null));
        assertFalse(new ModelDebugConfig().equals("a string"));
        assertFalse(new ModelDebugConfig(80.0, "").equals(new ModelDebugConfig(81.0, "")));
        assertFalse(new ModelDebugConfig(80.0, "foo").equals(new ModelDebugConfig(80.0, "bar")));

        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        assertTrue(modelDebugConfig.equals(modelDebugConfig));
        assertTrue(new ModelDebugConfig().equals(new ModelDebugConfig()));
        assertTrue(new ModelDebugConfig(80.0, "foo").equals(new ModelDebugConfig(80.0, "foo")));
    }

    @Test
    public void testHashCode()
    {
        assertEquals(new ModelDebugConfig(80.0, "foo").hashCode(),
                new ModelDebugConfig(80.0, "foo").hashCode());
    }
}
