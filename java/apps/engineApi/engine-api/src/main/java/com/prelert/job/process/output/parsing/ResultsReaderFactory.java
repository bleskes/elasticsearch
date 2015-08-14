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

import java.io.InputStream;

import org.apache.log4j.Logger;

import com.prelert.job.persistence.JobProviderFactory;
import com.prelert.job.persistence.JobResultsPeristerFactory;

/**
 * Factory method for creating new {@linkplain ResultsReader} objects
 * to parse the autodetect output.
 * Requires 2 other factories for creating the
 *
 */
public class ResultsReaderFactory
{
    private JobResultsPeristerFactory m_PersisterFactory;
    private JobProviderFactory m_ProviderFactory;

    public ResultsReaderFactory(JobProviderFactory providerFactory,
                                JobResultsPeristerFactory persisterFactory)
    {
        m_ProviderFactory = providerFactory;
        m_PersisterFactory = persisterFactory;
    }

	public ResultsReader newResultsParser(String jobId, InputStream autoDetectOutputStream,
			Logger logger)
	{
	    return new ResultsReader(jobId,
	                             m_PersisterFactory.jobResultsPersister(jobId),
	                             m_ProviderFactory.jobProvider(),
	                             autoDetectOutputStream,
	                             logger);
    }
}
