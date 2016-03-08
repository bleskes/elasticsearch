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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.manager.DescriptionAlreadyUsedException;
import com.prelert.job.manager.NoSuchModelSnapshotException;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.QueryPage;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.exception.InvalidParametersException;
import com.prelert.rs.validation.PaginationParamsValidator;

/**
 * API model snapshot management end point.
 * List, revert and update model snapshots.
 */
@Path("/modelsnapshots")
public class ModelSnapshots extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(ModelSnapshots.class);

    /**
     * The name of the endpoint
     */
    public static final String ENDPOINT = "modelsnapshots";

    public static final String SORT_QUERY_PARAM = "sort";
    public static final String TIME_QUERY_PARAM = "time";
    public static final String OLD_PARAM = "old";
    public static final String NEW_PARAM = "new";

    /**
     * Get the model snapshot results (in pages) for the job.  Optionally filtering
     * may be applied.
     *
     * @param jobId
     * @param skip
     * @param take
     * @param start The filter start date see {@linkplain #paramToEpoch(String)}
     * for the format the date string should take
     * @param end The filter end date see {@linkplain #paramToEpoch(String)}
     * for the format the date string should take
     * @param sort Name of the field to sort on
     * @param description Description to filter on
     * @return
     * @throws UnknownJobException
     */
    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<ModelSnapshot> modelSnapshots(
            @PathParam("jobId") String jobId,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
            @DefaultValue("") @QueryParam(SORT_QUERY_PARAM) String sortField,
            @DefaultValue("") @QueryParam(ModelSnapshot.DESCRIPTION) String description)
    throws UnknownJobException
    {
        LOGGER.debug(String.format("Get model snapshots for job %s. skip = %d, take = %d"
                + " start = '%s', end='%s', sort=%s, description filter=%s",
                jobId, skip, take, start, end,
                sortField, description));

        new PaginationParamsValidator(skip, take).validate();

        long epochStart = paramToEpochIfValidOrThrow(START_QUERY_PARAM, start, LOGGER);
        long epochEnd = paramToEpochIfValidOrThrow(END_QUERY_PARAM, end, LOGGER);

        JobManager manager = jobManager();
        QueryPage<ModelSnapshot> page;

        page = manager.modelSnapshots(jobId, skip, take, epochStart, epochEnd,
                sortField, description);

        Pagination<ModelSnapshot> modelSnapshots = paginationFromQueryPage(page, skip, take);

        // paging
        if (modelSnapshots.isAllResults() == false)
        {
            String path = new StringBuilder()
                                .append("/modelsnapshots/")
                                .append(jobId)
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
            if (!sortField.isEmpty())
            {
                queryParams.add(this.new KeyValue(SORT_QUERY_PARAM, sortField));
            }
            // It should be impossible to have more pages if a description was specified

            setPagingUrls(path, modelSnapshots, queryParams);
        }

        LOGGER.debug(String.format("Return %d model snapshots for job %s",
                modelSnapshots.getDocumentCount(), jobId));

        return modelSnapshots;
    }

    /**
     * Attempt to revert to the most recent snapshot matching specified criteria.
     *
     * @param jobId
     * @param time revert to a snapshot with a timestamp no later than this time
     * @param description the description of the snapshot to revert to
     * @return
     * @throws JobInUseException
     * @throws UnknownJobException
     * @throws NoSuchModelSnapshotException
     */
    @POST
    @Path("/{jobId}/revert")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertToSnapshot(@PathParam("jobId") String jobId,
            @DefaultValue("") @QueryParam(TIME_QUERY_PARAM) String time,
            @DefaultValue("") @QueryParam(ModelSnapshot.DESCRIPTION) String description)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException
    {
        LOGGER.debug("Received request to revert to time '" + time
                + "' description '" + description + "'");

        if (time.isEmpty() && description.isEmpty())
        {
            throw new InvalidParametersException(Messages.getMessage(Messages.REST_INVALID_REVERT_PARAMS),
                    ErrorCodes.INVALID_REVERT_PARAMS);
        }

        long timeEpochMs = paramToEpochIfValidOrThrow(TIME_QUERY_PARAM, time, LOGGER);
        // Time ranges are open above, so add 1 millisecond to convert the time
        // to the end of a range
        long endEpochMs = (timeEpochMs > 0) ? (timeEpochMs + 1) : 0;

        jobManager().revertToSnapshot(jobId, endEpochMs, description);
        return Response.ok(new Acknowledgement()).build();
    }

    /**
     * Attempt to revert to the most recent snapshot matching specified criteria.
     *
     * @param jobId
     * @param oldDescription current description of model snapshot to be changed
     * @param newDescription new description to set
     * @return
     * @throws UnknownJobException
     * @throws NoSuchModelSnapshotException
     */
    @PUT
    @Path("/{jobId}/description")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDescription(@PathParam("jobId") String jobId,
            @DefaultValue("") @QueryParam(OLD_PARAM) String oldDescription,
            @DefaultValue("") @QueryParam(NEW_PARAM) String newDescription)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            DescriptionAlreadyUsedException
    {
        LOGGER.debug("Received request to change model snapshot description from '"
                + oldDescription + "' to '" + newDescription + "'");

        if (oldDescription.isEmpty() || newDescription.isEmpty())
        {
            throw new InvalidParametersException(Messages.getMessage(Messages.REST_INVALID_DESCRIPTION_PARAMS),
                    ErrorCodes.INVALID_DESCRIPTION_PARAMS);
        }

        jobManager().updateModelSnapshotDescription(jobId, oldDescription, newDescription);
        return Response.ok(new Acknowledgement()).build();
    }

}
