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

package com.prelert.rs.resources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.validation.PaginationParamsValidator;

/**
 * API record results end point.
 * Access anomaly records filtered by date with various sort options
 */
@Path("/results")
public class Records extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Records.class);

    /**
     * The name of the records endpoint
     */
    public static final String ENDPOINT = "records";

    /**
     * Should interim results be returned as well as final results?
     */
    public static final String INCLUDE_INTERIM_QUERY_PARAM = "includeInterim";

    /**
     * Sort field query parameter
     */
    public static final String SORT_QUERY_PARAM = "sort";
    /**
     * Sort direction
     */
    public static final String DESCENDING_ORDER = "desc";

    /**
     * Get all the records (in pages) for the job optionally filtered
     * by date.
     *
     * @param jobId
     * @param skip
     * @param take
     * @param start The filter start date see {@linkplain #paramToEpoch(String)}
     * for the format the date string should take
     * @param end The filter end date see {@linkplain #paramToEpoch(String)}
     * for the format the date string should take
     * @param includeInterim Include interim results - default is false
     * @return
     */
    @GET
    @Path("/{jobId}/records")
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<AnomalyRecord> records(
            @PathParam("jobId") String jobId,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
            @DefaultValue("false") @QueryParam(INCLUDE_INTERIM_QUERY_PARAM) boolean includeInterim,
            @DefaultValue(AnomalyRecord.NORMALIZED_PROBABILITY) @QueryParam(SORT_QUERY_PARAM) String sort,
            @DefaultValue("true") @QueryParam(DESCENDING_ORDER) boolean descending,
            @DefaultValue("0.0") @QueryParam(AnomalyRecord.ANOMALY_SCORE) double anomalyScoreFilter,
            @DefaultValue("0.0") @QueryParam(AnomalyRecord.NORMALIZED_PROBABILITY) double normalizedProbabilityFilter)
    throws NativeProcessRunException, UnknownJobException
    {
        LOGGER.debug(String.format("Get records for job %s. skip = %d, take = %d"
                + " start = '%s', end='%s', sort='%s' descending=%b"
                + ", anomaly score filter=%f, unsual score filter= %f, %s interim results",
                jobId, skip, take, start, end, sort, descending,
                normalizedProbabilityFilter, anomalyScoreFilter,
                includeInterim ? "including" : "excluding"));

        new PaginationParamsValidator(skip, take).validate();

        long epochStartMs = paramToEpochIfValidOrThrow(START_QUERY_PARAM, start, LOGGER);
        long epochEndMs = paramToEpochIfValidOrThrow(END_QUERY_PARAM, end, LOGGER);

        JobManager manager = jobManager();

        QueryPage<AnomalyRecord> queryResults;

        if (epochStartMs > 0 || epochEndMs > 0)
        {
            queryResults = manager.records(jobId, skip, take, epochStartMs, epochEndMs, includeInterim, sort,
                    descending, anomalyScoreFilter, normalizedProbabilityFilter);
        }
        else
        {
            queryResults = manager.records(jobId, skip, take, includeInterim, sort, descending,
                    anomalyScoreFilter, normalizedProbabilityFilter);
        }

        Pagination<AnomalyRecord> records = paginationFromQueryPage(queryResults, skip, take);

        // paging
        if (records.isAllResults() == false)
        {
            String path = new StringBuilder()
                                .append("/results/")
                                .append(jobId)
                                .append("/records/")
                                .toString();

            List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
            if (epochStartMs > 0)
            {
                queryParams.add(this.new KeyValue(START_QUERY_PARAM, start));
            }
            if (epochEndMs > 0)
            {
                queryParams.add(this.new KeyValue(END_QUERY_PARAM, end));
            }
            queryParams.add(this.new KeyValue(INCLUDE_INTERIM_QUERY_PARAM, Boolean.toString(includeInterim)));
            queryParams.add(this.new KeyValue(SORT_QUERY_PARAM, sort));
            queryParams.add(this.new KeyValue(DESCENDING_ORDER, Boolean.toString(descending)));
            queryParams.add(this.new KeyValue(AnomalyRecord.ANOMALY_SCORE, String.format("%2.1f", anomalyScoreFilter)));
            queryParams.add(this.new KeyValue(AnomalyRecord.NORMALIZED_PROBABILITY, String.format("%2.1f", normalizedProbabilityFilter)));

            setPagingUrls(path, records, queryParams);
        }

        LOGGER.debug(String.format("Return %d records for job %s",
                records.getDocumentCount(), jobId));

        return records;
    }

}
