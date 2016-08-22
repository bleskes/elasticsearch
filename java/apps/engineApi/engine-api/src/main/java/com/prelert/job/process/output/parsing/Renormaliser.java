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

package com.prelert.job.process.output.parsing;

import org.apache.log4j.Logger;

import com.prelert.job.quantiles.Quantiles;

public interface Renormaliser
{
    /**
     * Update the anomaly score field on all previously persisted buckets
     * and all contained records
     * @param quantiles
     * @param logger
     */
    void renormalise(Quantiles quantiles, Logger logger);

    /**
     * Update the anomaly score field on all previously persisted buckets
     * and all contained records and aggregate records to the partition
     * level
     * @param quantiles
     * @param logger
     */
    void renormaliseWithPartition(Quantiles quantiles, Logger logger);


    /**
     * Blocks until the renormaliser is idle and no further normalisation tasks are pending.
     */
    void waitUntilIdle();

    /**
     * Shut down the renormaliser
     */
    boolean shutdown(Logger logger);
}
