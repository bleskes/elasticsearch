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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.NoSuchModelSnapshotException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.CannotDeleteSnapshotException;
import com.prelert.job.manager.DescriptionAlreadyUsedException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
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
    public static final String DELETE_INTERVENING_RESULTS_PARAM = "deleteInterveningResults";

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

        // The quantiles can be large, and totally dominate the output - it's
        // clearer to remove them
        if (modelSnapshots.getDocuments() != null)
        {
            for (ModelSnapshot modelSnapshot : modelSnapshots.getDocuments())
            {
                modelSnapshot.setQuantiles(null);
            }
        }

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
     * @param snapshotId the snapshot ID of the snapshot to revert to
     * @param description the description of the snapshot to revert to
     * @param deleteInterveningResults should we reset the results back to the time
     *          of the snapshot?
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
            @DefaultValue("") @QueryParam(ModelSnapshot.SNAPSHOT_ID) String snapshotId,
            @DefaultValue("") @QueryParam(ModelSnapshot.DESCRIPTION) String description,
            @DefaultValue("false") @QueryParam(DELETE_INTERVENING_RESULTS_PARAM) boolean deleteInterveningResults)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException
    {
        LOGGER.debug("Received request to revert to time '" + time +
                "' description '" + description + "' snapshot id '" +
                snapshotId + "' for job '" + jobId + "', deleting intervening " +
                " results: " + deleteInterveningResults);

        if (time.isEmpty() && snapshotId.isEmpty() && description.isEmpty())
        {
            throw new InvalidParametersException(Messages.getMessage(Messages.REST_INVALID_REVERT_PARAMS),
                    ErrorCodes.INVALID_REVERT_PARAMS);
        }
        long timeEpochMs = paramToEpochIfValidOrThrow(TIME_QUERY_PARAM, time, LOGGER);
        // Time ranges are open above, so add 1 millisecond to convert the time
        // to the end of a range
        long endEpochMs = (timeEpochMs > 0) ? (timeEpochMs + 1) : 0;

        ModelSnapshot revertedTo = jobManager().revertToSnapshot(jobId, endEpochMs,
                snapshotId, description, deleteInterveningResults);

        if (deleteInterveningResults == true)
        {
            Date revertedLatestRecordTime = revertedTo.getLatestRecordTimeStamp();
            Date revertedLatestResultTime = revertedTo.getLatestResultTimeStamp();
            LOGGER.debug("Removing intervening records: last record: " + revertedLatestRecordTime +
                    ", last result: " + revertedLatestResultTime);

            jobManager().deleteBucketsAfter(jobId, revertedLatestResultTime);
            jobManager().resetLatestRecordTime(jobId, revertedLatestRecordTime);
        }

        // The quantiles can be large, and totally dominate the output - it's
        // clearer to remove them
        revertedTo.setQuantiles(null);

        SingleDocument<ModelSnapshot> doc = new SingleDocument<>();
        doc.setDocument(revertedTo);
        doc.setType(ModelSnapshot.TYPE);

        return Response.ok(doc).build();
    }

    /**
     * Attempt to update the description of the specified snapshot.
     *
     * @param jobId
     * @param snapshotId snapshot ID of the snapshot whose description is to be updated
     * @param updateJson JSON containing the new snapshot description
     * @return
     * @throws UnknownJobException
     * @throws NoSuchModelSnapshotException
     */
    @PUT
    @Path("/{jobId}/{snapshotId}/description")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDescription(@PathParam("jobId") String jobId,
            @PathParam("snapshotId") String snapshotId,
            @DefaultValue("") String updateJson)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            DescriptionAlreadyUsedException, MalformedJsonException
    {
        LOGGER.debug("Received request to change model snapshot description using '"
                + updateJson + "' for snapshot ID '" + snapshotId +
                "' for job '" +jobId + "'");

        String newDescription = parseDescriptionFromJson(updateJson);

        if (snapshotId.isEmpty() || newDescription.isEmpty())
        {
            throw new InvalidParametersException(Messages.getMessage(Messages.REST_INVALID_DESCRIPTION_PARAMS),
                    ErrorCodes.INVALID_DESCRIPTION_PARAMS);
        }

        ModelSnapshot updatedDesc = jobManager().updateModelSnapshotDescription(jobId, snapshotId, newDescription);

        // The quantiles can be large, and totally dominate the output - it's
        // clearer to remove them
        updatedDesc.setQuantiles(null);

        SingleDocument<ModelSnapshot> doc = new SingleDocument<>();
        doc.setDocument(updatedDesc);
        doc.setType(ModelSnapshot.TYPE);

        return Response.ok(doc).build();
    }

    /**
     * Attempt to delete the specified snapshot.
     * Deleting the snapshot with the highest restore priority is not allowed.
     *
     * @param jobId
     * @param snapshotId snapshot ID of the snapshot to delete
     * @return
     * @throws UnknownJobException
     * @throws NoSuchModelSnapshotException
     */
    @DELETE
    @Path("/{jobId}/{snapshotId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteModelSnapshot(@PathParam("jobId") String jobId,
            @PathParam("snapshotId") String snapshotId)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            CannotDeleteSnapshotException
    {
        LOGGER.debug("Received request to delete model snapshot '" + snapshotId +
                "' for job '" +jobId + "'");

        jobManager().deleteModelSnapshot(jobId, snapshotId);

        return Response.ok().entity(new Acknowledgement()).build();
    }

    /**
     * Given a string representing description update JSON, get the description
     * out of it.  Irrelevant junk in the JSON document is tolerated.
     */
    private String parseDescriptionFromJson(String updateJson) throws MalformedJsonException
    {
        if (updateJson != null && !updateJson.isEmpty())
        {
            try
            {
                ObjectNode objNode = new ObjectMapper().readValue(updateJson, ObjectNode.class);
                JsonNode descNode = objNode.get(ModelSnapshot.DESCRIPTION);
                if (descNode != null)
                {
                    return descNode.asText();
                }
            }
            catch (IOException e)
            {
                throw new MalformedJsonException(e);
            }
        }
        return "";
    }
}
