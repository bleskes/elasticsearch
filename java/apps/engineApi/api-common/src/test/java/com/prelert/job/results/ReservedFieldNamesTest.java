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
package com.prelert.job.results;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.TreeSet;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

public class ReservedFieldNamesTest
{
    @Test
    public void testContent()
    {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(ReservedFieldNamesTest.class);

        logger.info("Reserved field names are: " + new TreeSet<String>(ReservedFieldNames.RESERVED_FIELD_NAMES));

        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("description"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("dest"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("dst"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("host"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("instance"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("region"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("source"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("src"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("status"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("type"));
        assertFalse(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("id"));

        assertTrue(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("jobId"));
        assertTrue(ReservedFieldNames.RESERVED_FIELD_NAMES.contains("@timestamp"));
    }
}
