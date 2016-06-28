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

package com.prelert.data.extractors.elasticsearch;

import java.util.List;

import org.apache.log4j.Logger;

public interface IndexSelector
{
    /**
     * <p>Selects the indices that contain data in the
     * given time range [{@code startMs}, {@code endMs}).
     * If {@code startMs} == {@code endMs}, indices that contain
     * {@code startMs} will be selected.
     *
     * <p>Implementations may use a cache so that subsequent calls to this
     * method on intervals that are sub-intervals of a previous one
     * may be handled more efficiently
     *
     * @param startMs the interval start as epoch millis (inclusive)
     * @param endMs the interval end as epoch millis (exclusive)
     * @param logger the job logger
     * @return a list that contains the index names for indices that
     * contain data in the given time range.
     */
    List<String> selectByTime(long startMs, long endMs, Logger logger);

    /**
     * Clears any cached information.
     */
    void clearCache();
}
