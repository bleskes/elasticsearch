/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DataDescriptionTest
{
    @Test
    public void testHashCode_GivenEqual()
    {
        Map<String, Object> description = new HashMap<>();
        description.put(DataDescription.FORMAT, "JSON");
        description.put(DataDescription.TIME_FIELD_NAME, "timestamp");
        description.put(DataDescription.QUOTE_CHARACTER, "'");
        description.put(DataDescription.TIME_FORMAT, "timeFormat");
        description.put(DataDescription.FIELD_DELIMITER, ",");

        DataDescription dataDescription1 = new DataDescription(description);
        DataDescription dataDescription2 = new DataDescription(description);

        assertEquals(dataDescription1.hashCode(), dataDescription2.hashCode());
    }
}
