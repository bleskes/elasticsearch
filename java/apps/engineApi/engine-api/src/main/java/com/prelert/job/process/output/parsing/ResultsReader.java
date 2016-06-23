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

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.prelert.job.alert.AlertObserver;
import com.prelert.job.persistence.JobResultsPersister;
import com.prelert.utils.json.AutoDetectParseException;

/**
 * A runnable class that reads the autodetect process output
 * and writes the results via the {@linkplain JobResultsPersister}
 * passed in the constructor.
 *
 * Has methods to register and remove alert observers.
 * Also has a method to wait for a flush to be complete.
 */
public class ResultsReader implements Runnable
{
    private final InputStream m_Stream;
    private final Logger m_Logger;
    private final AutoDetectResultsParser m_Parser;
    private final Renormaliser m_Renormaliser;
    private final JobResultsPersister m_ResultsPersister;

    public ResultsReader(Renormaliser renormaliser, JobResultsPersister persister,
            InputStream stream, Logger logger)
    {
        m_Stream = stream;
        m_Logger = logger;
        m_Parser = new AutoDetectResultsParser();
        m_Renormaliser = renormaliser;
        m_ResultsPersister = persister;
    }

    @Override
    public void run()
    {
        try
        {
            m_Parser.parseResults(m_Stream, m_ResultsPersister, m_Renormaliser, m_Logger);
        }
        catch (AutoDetectParseException e)
        {
            m_Logger.info("Error parsing autodetect_api output", e);
        }
        finally
        {
            try
            {
                // read anything left in the stream before
                // closing the stream otherwise if the process
                // tries to write more after the close it gets
                // a SIGPIPE
                byte [] buff = new byte [512];
                while (m_Stream.read(buff) >= 0)
                {
                    // Do nothing
                }
                m_Stream.close();
            }
            catch (IOException e)
            {
                m_Logger.warn("Error closing result parser input stream", e);
            }

            // The renormaliser may have started another thread,
            // so give it a chance to shut this down
            m_Renormaliser.shutdown(m_Logger);
        }

        m_Logger.info("Parse results Complete");
    }

    public void addAlertObserver(AlertObserver ao)
    {
        m_Parser.addObserver(ao);
    }

    public boolean removeAlertObserver(AlertObserver ao)
    {
        return m_Parser.removeObserver(ao);
    }

    public void waitForFlushComplete(String flushId)
            throws InterruptedException
    {
        m_Parser.waitForFlushAcknowledgement(flushId);

        // We also have to wait for the normaliser to become idle so that we block
        // clients from querying results in the middle of normalisation.
        m_Renormaliser.waitUntilIdle();
    }
}

