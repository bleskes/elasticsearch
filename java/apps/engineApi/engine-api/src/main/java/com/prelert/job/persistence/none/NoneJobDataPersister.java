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
package com.prelert.job.persistence.none;

import java.util.List;
import java.util.Map;

import com.prelert.job.persistence.JobDataPersister;

/**
 * A 'do nothing' job data persister
 */
public class NoneJobDataPersister extends JobDataPersister
{
    /**
     * No point setting up the field mappings here
     */
    @Override
    public void setFieldMappings(List<String> fields,
            List<String> byFields, List<String> overFields,
            List<String> partitionFields, Map<String, Integer> fieldMap)
    {
        // Do nothing
    }

    @Override
    public void persistRecord(long epoch, String[] record)
    {
     // Do nothing
    }

    @Override
    public void flushRecords()
    {
     // Do nothing
    }

    @Override
    public boolean deleteData()
    {
        return false;
    }
}
