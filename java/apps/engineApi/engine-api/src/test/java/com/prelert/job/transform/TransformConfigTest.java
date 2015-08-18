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

package com.prelert.job.transform;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

public class TransformConfigTest
{
    @Test
    public void testVerify_GivenUnknownTransform()
    {
        TransformConfig tr = new TransformConfig();
        tr.setInputs(Arrays.asList("f1", "f2"));
        tr.setTransform("unknown+transform");

        try
        {
            TransformType type = tr.type();
            fail("Unknown transform type should throw an IllegalArgumentException " +
                    "Type = " + type);
        }
        catch (IllegalArgumentException e)
        {

        }
    }
}
