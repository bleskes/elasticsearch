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


import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.ListDocument;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.SingleDocument;


/**
 * Manage lists containing strings.
 */
@Path("/lists")
public class Lists extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Lists.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "lists";

    /**
     * Creates a list
     *
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createList(ListDocument list)
    {
        LOGGER.debug("Received request to create list: " + list.getId());
        if (jobManager().createList(list))
        {
            return Response.ok(new Acknowledgement()).build();
        }
        LOGGER.error("Failed to create list");
        return Response.serverError().build();
    }

    @GET
    @Path("/{listId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getList(@PathParam("listId") String listId)
    {
        Optional<ListDocument> list = jobManager().getList(listId);
        SingleDocument<ListDocument> listDoc = singleDocFromOptional(list, ListDocument.TYPE);
        return listDoc.isExists() ? Response.ok(listDoc).build()
                : Response.status(Response.Status.NOT_FOUND).entity(list).build();
    }
}
