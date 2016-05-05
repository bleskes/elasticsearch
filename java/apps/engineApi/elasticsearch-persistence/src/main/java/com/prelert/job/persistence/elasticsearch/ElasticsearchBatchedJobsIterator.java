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

package com.prelert.job.persistence.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobDetails;

class ElasticsearchBatchedJobsIterator extends ElasticsearchBatchedDocumentsIterator<JobDetails>
{
    private final ElasticsearchJobDetailsMapper m_JobMapper;

    public ElasticsearchBatchedJobsIterator(Client client, String index,
            ObjectMapper objectMapper)
    {
        super(client, index, objectMapper);
        m_JobMapper = new ElasticsearchJobDetailsMapper(client, objectMapper);
    }

    @Override
    protected String getType()
    {
        return JobDetails.TYPE;
    }

    @Override
    protected JobDetails map(ObjectMapper objectMapper, SearchHit hit)
    {
        try
        {
            return m_JobMapper.map(hit.getSource());
        }
        catch (CannotMapJobFromJson e)
        {
            return null;
        }
    }
}
