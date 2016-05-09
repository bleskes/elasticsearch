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
import com.prelert.job.results.Influencer;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.validation.PaginationParamsValidator;

@Path("/results")
public class Influencers extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Influencers.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "influencers";

    private static final String SORT_QUERY_PARAM = "sort";
    private static final String DESCENDING_ORDER = "desc";

    /**
     * Get influencers for the job
     *
     * @return Array of JSON objects string
     * @throws UnknownJobException
     */
    @GET
    @Path("/{jobId}/influencers")
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<Influencer> influencers(
            @PathParam("jobId") String jobId,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
            @DefaultValue(Influencer.ANOMALY_SCORE) @QueryParam(SORT_QUERY_PARAM) String sort,
            @DefaultValue("true") @QueryParam(DESCENDING_ORDER) boolean descending,
            @DefaultValue("0.0") @QueryParam(Influencer.ANOMALY_SCORE) double anomalyScoreFilter,
            @DefaultValue("false") @QueryParam(Buckets.INCLUDE_INTERIM_QUERY_PARAM)
                boolean includeInterim)
    throws UnknownJobException
    {
        LOGGER.debug(String.format("Get influencers for job %s. skip = %d, take = %d"
                + " start = '%s', end='%s', sort='%s' descending=%b"
                + ", anomaly score filter=%f, %s interim results",
                jobId, skip, take, start, end, sort, descending,
                anomalyScoreFilter, includeInterim ? "including" : "excluding"));

        new PaginationParamsValidator(skip, take).validate();

        long epochStartMs = paramToEpochIfValidOrThrow(START_QUERY_PARAM, start, LOGGER);
        long epochEndMs = paramToEpochIfValidOrThrow(END_QUERY_PARAM, end, LOGGER);

        JobManager manager = jobManager();

        QueryPage<Influencer> page = manager.influencers(jobId, skip, take, epochStartMs,
                epochEndMs, sort, descending, anomalyScoreFilter, includeInterim);
        Pagination<Influencer> results = paginationFromQueryPage(page, skip, take);

        setPagingUrls(ENDPOINT + "/" + jobId, results);

        LOGGER.debug(String.format("Returning %d of %d influencers",
                results.getDocuments().size(), results.getHitCount()));

        return results;
    }
}
