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

package com.prelert.rs.data.extraction;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import com.prelert.data.extractor.elasticsearch.ElasticsearchDataExtractor;
import com.prelert.job.JobDetails;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.data.extraction.DataExtractorFactory;

public class DataExtractorFactoryImpl implements DataExtractorFactory
{
    @Override
    public DataExtractor newExtractor(JobDetails job)
    {
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        if (schedulerConfig.getDataSource() == DataSource.ELASTICSEARCH)
        {
            return createElasticsearchDataExtractor(job);
        }
        throw new IllegalArgumentException();
    }

    private DataExtractor createElasticsearchDataExtractor(JobDetails job)
    {
        String timeField = job.getDataDescription().getTimeField();
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        return ElasticsearchDataExtractor.create(schedulerConfig.getBaseUrl(),
                schedulerConfig.getIndexes(), schedulerConfig.getTypes(),
                stringifyElasticsearchQuery(schedulerConfig.getQuery()),
                stringifyElasticsearchAggregations(schedulerConfig.getAggregations(), schedulerConfig.getAggs()),
                timeField);
    }

    @VisibleForTesting
    String stringifyElasticsearchQuery(Map<String, Object> queryMap)
    {
        String queryStr = writeObjectAsJson(queryMap);
        if (queryStr.startsWith("{") && queryStr.endsWith("}"))
        {
            return queryStr.substring(1, queryStr.length() - 1);
        }
        return queryStr;
    }

    @VisibleForTesting
    String stringifyElasticsearchAggregations(Map<String, Object> aggregationsMap,
            Map<String, Object> aggsMap)
    {
        if (aggregationsMap != null)
        {
            return "\"" + SchedulerConfig.AGGREGATIONS + "\": " + writeObjectAsJson(aggregationsMap);
        }
        if (aggsMap != null)
        {
            return "\"" + SchedulerConfig.AGGS + "\": " + writeObjectAsJson(aggsMap);
        }
        return null;
    }

    private static String writeObjectAsJson(Object obj)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try
        {
            return objectMapper.writeValueAsString(obj);
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException(e);
        }
    }

}
