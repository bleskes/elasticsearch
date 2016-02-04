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


import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.Detector;
import com.prelert.job.config.verification.DetectorVerifier;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.rs.data.Acknowledgement;


/**
 * Validation REST paths are operations that allow the user to validate
 * sub-sections of configurations without having to create a job.
 */
@Path("/validate")
public class Validate extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Validate.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "validate";

    /**
     * Validates JSON representing a single detector.
     *
     * @param detector The detector to be validated
     * @return
     * @throws JobConfigurationException
     */
    @POST
    @Path("/detector")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateDetector(Detector detector) throws JobConfigurationException
    {
        LOGGER.trace("Received request to validate detector");
        DetectorVerifier.verify(detector, false);
        return Response.ok(new Acknowledgement()).build();
    }
}
