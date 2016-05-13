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
import java.util.Optional;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.reader.JobDataReader;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.validation.PaginationParamsValidator;

/**
 * API bucket results end point.
 * Access buckets and anomaly records, use the <pre>expand</pre> query argument
 * to get buckets and anomaly records in one query.
 * Buckets can be filtered by date.
 */
@Path("/results")
public class Buckets extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Buckets.class);

    /**
     * The name of the endpoint
     */
    public static final String ENDPOINT = "buckets";

    private static final String TIMESTAMP_PARAM = "timestamp";
    public static final String EXPAND_QUERY_PARAM = "expand";
    public static final String INCLUDE_INTERIM_QUERY_PARAM = "includeInterim";

    /**
     * Get all the bucket results (in pages) for the job optionally filtered
     * by date.
     *
     * @param jobId
     * @param expand Return anomaly records in-line with the results,
     *  default is false
     * @param includeInterim Include interim results - default is false
     * @param skip
     * @param take
     * @param start The filter start date see {@linkplain #paramToEpoch(String)}
     * for the format the date string should take
     * @param end The filter end date see {@linkplain #paramToEpoch(String)}
     * for the format the date string should take
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    @GET
    @Path("/{jobId}/buckets")
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<Bucket> buckets(
            @PathParam("jobId") String jobId,
            @DefaultValue("false") @QueryParam(EXPAND_QUERY_PARAM) boolean expand,
            @DefaultValue("false") @QueryParam(INCLUDE_INTERIM_QUERY_PARAM) boolean includeInterim,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
            @DefaultValue("0.0") @QueryParam(Bucket.ANOMALY_SCORE) double anomalySoreFilter,
            @DefaultValue("0.0") @QueryParam(Bucket.MAX_NORMALIZED_PROBABILITY) double normalizedProbabilityFilter)
    throws UnknownJobException
    {
        LOGGER.debug(String.format("Get %sbuckets for job %s. skip = %d, take = %d"
                + " start = '%s', end='%s', anomaly score filter=%f, unsual score filter= %f, %s interim results",
                expand ? "expanded " : "", jobId, skip, take, start, end,
                anomalySoreFilter, normalizedProbabilityFilter,
                includeInterim ? "including" : "excluding"));

        new PaginationParamsValidator(skip, take).validate();

        long epochStart = paramToEpochIfValidOrThrow(START_QUERY_PARAM, start, LOGGER);
        long epochEnd = paramToEpochIfValidOrThrow(END_QUERY_PARAM, end, LOGGER);

        JobDataReader jobReader = jobReader();
        QueryPage<Bucket> page;

        if (epochStart > 0 || epochEnd > 0)
        {
            page = jobReader.buckets(jobId, expand, includeInterim, skip, take, epochStart, epochEnd,
                    anomalySoreFilter, normalizedProbabilityFilter);
        }
        else
        {
            page = jobReader.buckets(jobId, expand, includeInterim, skip, take,
                    anomalySoreFilter, normalizedProbabilityFilter);
        }

        Pagination<Bucket> buckets = paginationFromQueryPage(page, skip, take);

        // paging
        if (buckets.isAllResults() == false)
        {
            String path = new StringBuilder()
                                .append("/results/")
                                .append(jobId)
                                .append("/buckets")
                                .toString();

            List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
            if (epochStart > 0)
            {
                queryParams.add(this.new KeyValue(START_QUERY_PARAM, start));
            }
            if (epochEnd > 0)
            {
                queryParams.add(this.new KeyValue(END_QUERY_PARAM, end));
            }
            queryParams.add(this.new KeyValue(EXPAND_QUERY_PARAM, Boolean.toString(expand)));
            queryParams.add(this.new KeyValue(INCLUDE_INTERIM_QUERY_PARAM, Boolean.toString(includeInterim)));
            queryParams.add(this.new KeyValue(Bucket.ANOMALY_SCORE, String.format("%2.1f", anomalySoreFilter)));
            queryParams.add(this.new KeyValue(Bucket.MAX_NORMALIZED_PROBABILITY, String.format("%2.1f", normalizedProbabilityFilter)));

            setPagingUrls(path, buckets, queryParams);
        }

        LOGGER.debug(String.format("Return %d buckets for job %s",
                buckets.getDocumentCount(), jobId));

        return buckets;
    }


    /**
     * Get an individual bucket and optionally the expanded results.
     *
     *
     * @param jobId
     * @param timestamp
     * @param expand Return anomaly records in-line with the bucket,
     * default is false
     * @param includeInterim Include interim results - default is false
     * @return
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     */
    @GET
    @Path("/{jobId}/buckets/{timestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bucket(@PathParam("jobId") String jobId,
            @PathParam(TIMESTAMP_PARAM) String timestamp,
            @DefaultValue("false") @QueryParam(EXPAND_QUERY_PARAM) boolean expand,
            @DefaultValue("false") @QueryParam(INCLUDE_INTERIM_QUERY_PARAM) boolean includeInterim)
    throws NativeProcessRunException, UnknownJobException
    {
        LOGGER.debug(String.format("Get %sbucket %s for job %s, %s interim results",
                expand ? "expanded " : "", timestamp, jobId,
                includeInterim ? "including" : "excluding"));

        JobDataReader jobReader = jobReader();
        long timestampMillis = paramToEpochIfValidOrThrow(TIMESTAMP_PARAM, timestamp, LOGGER);
        Optional<Bucket> b = jobReader.bucket(jobId, timestampMillis, expand, includeInterim);
        SingleDocument<Bucket> bucket = singleDocFromOptional(b, Bucket.TYPE);

        if (bucket.isExists())
        {
            LOGGER.debug(String.format("Returning bucket %s for job %s", timestamp, jobId));
        }
        else
        {
            LOGGER.debug(String.format("Cannot find bucket %s for job %s", timestamp, jobId));
            return Response.status(Response.Status.NOT_FOUND).entity(bucket).build();
        }

        return Response.ok(bucket).build();
    }
}
