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

package com.prelert.distributed;

import java.util.List;
import java.util.Map;

public interface EngineApiHosts
{
    /**
     * Get the list of Engine API hosts participating
     * in this cluster
     * @return
     */
    List<String> engineApiHosts();

    /**
     * Map of the job ID to Engine API host it is running on.
     * Only running jobs are present in the result map
     * @return job -> host
     */
    Map<String, String> hostByRunningJob();

    /**
     * Map of the scheduled job ID to Engine API host it is running on.
     * Only scheduled jobs are present in the result map
     * @return scheduled job -> host
     */
    Map<String, String> hostByScheduledJob();

    /**
     * Job ID to Engine host map for ALL jobs scheduled and running
     * @return
     */
    default Map<String, String> hostByJob()
    {
        Map<String, String> result = hostByRunningJob();
        result.putAll(hostByScheduledJob());
        return result;
    }
}
