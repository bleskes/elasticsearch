/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import javax.ws.rs.Path;


/**
 * Streaming data endpoint
 *
 * <pre>curl -X POST 'http://localhost:8080/api/data/<jobid>/' --data @<filename></pre>
 * <br>
 * Binary gzipped files must be POSTed with the --data-binary option
 * <pre>curl -X POST 'http://localhost:8080/api/data/<jobid>/' --data-binary @<filename.gz></pre>
 *
 */
@Path("/data")
public class Data extends AbstractDataLoad
{
    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "data";

    @Override
    protected boolean shouldPersist()
    {
        return false;
    }
}
