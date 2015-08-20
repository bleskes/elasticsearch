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
package com.prelert.job.results;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

public class InfluencerTest
{
    @Test
    public void testEquals()
    {
        Influencer inf = new Influencer();
        inf.setTimestamp(new Date(123));
        inf.setInfluencerFieldName("a");
        inf.setInfluencerFieldValue("f");
        inf.setProbability(0.1);
        inf.setInitialScore(2.0);

        Influencer inf2 = new Influencer("a", "f");
        inf2.setTimestamp(new Date(123));
        inf2.setProbability(0.1);
        inf2.setInitialScore(2.0);

        assertEquals(inf, inf2);

        inf.setTimestamp(new Date(321));
        assertFalse(inf.equals(inf2));
    }

    @Test
    public void testHash()
    {
        Influencer inf = new Influencer();
        inf.setTimestamp(new Date(123));
        inf.setInfluencerFieldName("a");
        inf.setInfluencerFieldValue("f");
        inf.setProbability(0.1);
        inf.setInitialScore(2.0);

        Influencer inf2 = new Influencer("a", "f");
        inf2.setTimestamp(new Date(123));
        inf2.setProbability(0.1);
        inf2.setInitialScore(2.0);

        assertEquals(inf.hashCode(), inf2.hashCode());

        inf.setTimestamp(new Date(321));
        assertFalse(inf.hashCode() == inf2.hashCode());
    }

}
