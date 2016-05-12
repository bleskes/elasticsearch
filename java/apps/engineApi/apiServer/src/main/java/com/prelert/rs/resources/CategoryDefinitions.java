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
import com.prelert.job.results.CategoryDefinition;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.validation.PaginationParamsValidator;

/**
 * API bucket results end point.
 * Access buckets and anomaly records, use the <pre>expand</pre> query argument
 * to get buckets and anomaly records in one query.
 * Buckets can be filtered by date.
 */
@Path("/results")
public class CategoryDefinitions extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(CategoryDefinitions.class);

    /**
     * The name of the endpoint
     */
    public static final String ENDPOINT = "categorydefinitions";

    /**
     * Get all the bucket results (in pages) for the job optionally filtered
     * by date.
     *
     * @param jobId
     * @param take
     * @return
     * @throws UnknownJobException
     */
    @GET
    @Path("/{jobId}/categorydefinitions")
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<CategoryDefinition> categoryDefinitions(
            @PathParam("jobId") String jobId,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take)
    throws UnknownJobException
    {
        LOGGER.debug(String.format("Get category definitions for job %s. take = %d", jobId, take));

        new PaginationParamsValidator(skip, take).validate();

        QueryPage<CategoryDefinition> queryResults =
                jobReader().categoryDefinitions(jobId, skip, take);

        Pagination<CategoryDefinition> categoryDefinitions =
                    paginationFromQueryPage(queryResults, skip, take);

        if (categoryDefinitions.isAllResults() == false)
        {
            String path = new StringBuilder()
                                .append("/results/")
                                .append(jobId)
                                .append("/categorydefinitions")
                                .toString();

            setPagingUrls(path, categoryDefinitions, new ArrayList<>());
        }

        LOGGER.debug(String.format("Return %d category definitions for job %s",
                categoryDefinitions.getDocumentCount(), jobId));

        return categoryDefinitions;
    }

    /**
     * Get all the bucket results (in pages) for the job optionally filtered
     * by date.
     *
     * @param jobId
     * @param take
     * @return
     * @throws UnknownJobException
     */
    @GET
    @Path("/{jobId}/categorydefinitions/{categoryId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response categoryDefinition(@PathParam("jobId") String jobId,
            @PathParam("categoryId") String categoryId)
    throws UnknownJobException
    {
        LOGGER.debug(String.format("Get category definition for job %s with id %s.", jobId, categoryId));

        Optional<CategoryDefinition> category = jobReader().categoryDefinition(jobId, categoryId);

        if (category.isPresent())
        {
            LOGGER.debug(String.format("Returning category definition %s for job %s",
                    categoryId, jobId));
        }
        else
        {
            LOGGER.debug(String.format("Cannot find category definition %s for job %s",
                    categoryId, jobId));

            return Response.status(Response.Status.NOT_FOUND)
                    .entity(singleDocFromOptional(category, CategoryDefinition.TYPE)).build();
        }

        return Response.ok(singleDocFromOptional(category, CategoryDefinition.TYPE)).build();
    }
}
