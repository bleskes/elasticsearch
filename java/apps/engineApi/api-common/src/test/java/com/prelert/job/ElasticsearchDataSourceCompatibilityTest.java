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
package com.prelert.job;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ElasticsearchDataSourceCompatibilityTest
{
    @Test
    public void testFrom_GivenValidOptions()
    {
        assertEquals(ElasticsearchDataSourceCompatibility.V_1_7_X,
                ElasticsearchDataSourceCompatibility.from("1.7.x"));
        assertEquals(ElasticsearchDataSourceCompatibility.V_1_7_X,
                ElasticsearchDataSourceCompatibility.from("1.7.X"));
        assertEquals(ElasticsearchDataSourceCompatibility.V_2_X_X,
                ElasticsearchDataSourceCompatibility.from("2.x.x"));
        assertEquals(ElasticsearchDataSourceCompatibility.V_2_X_X,
                ElasticsearchDataSourceCompatibility.from("2.X.X"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFrom_GivenNull()
    {
        ElasticsearchDataSourceCompatibility.from(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFrom_GivenInvalidOption()
    {
        ElasticsearchDataSourceCompatibility.from("invalid");
    }
}
