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
package com.prelert.job.alert;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlertTypeTest {

    @Test
    public void testAlertTypes()
    {
        assertEquals("bucket", AlertType.BUCKET.toString());
        assertEquals("bucketinfluencer", AlertType.BUCKETINFLUENCER.toString());
        assertEquals("influencer", AlertType.INFLUENCER.toString());

        assertEquals(AlertType.BUCKET, AlertType.fromString("bucket"));
        assertEquals(AlertType.BUCKETINFLUENCER, AlertType.fromString("bucketinfluencer"));
        assertEquals(AlertType.INFLUENCER, AlertType.fromString("influencer"));

        boolean exception = false;
        try
        {
            AlertType.fromString("Non-Existent alert type here");
            assert(false);
        }
        catch (IllegalArgumentException ex)
        {
            exception = true;
        }
        assert(exception);
    }

}
