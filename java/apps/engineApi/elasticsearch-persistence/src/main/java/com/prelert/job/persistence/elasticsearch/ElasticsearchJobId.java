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
 ***********************************************************/

package com.prelert.job.persistence.elasticsearch;

import java.util.Objects;

/**
 * The job identification needed from the Elasticsearch classes.
 * It contains the jobId and the index name.
 */
class ElasticsearchJobId
{
    /**
     * If this is changed, ProcessCtrl.ES_INDEX_PREFIX should also be changed
     */
    public static final String INDEX_PREFIX = "prelertresults-";

    private final String m_JobId;
    private final String m_IndexName;

    public ElasticsearchJobId(String jobId)
    {
        m_JobId = Objects.requireNonNull(jobId);
        m_IndexName = INDEX_PREFIX + jobId;
    }

    String getId()
    {
        return m_JobId;
    }

    String getIndex()
    {
        return m_IndexName;
    }

    @Override
    public String toString()
    {
        return m_JobId;
    }
}
