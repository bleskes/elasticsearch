/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job.process.normaliser;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.prelert.job.process.output.parsing.NormalisedResultsParser;
import com.prelert.job.process.writer.LengthEncodedWriter;

public class NormaliserProcess
{
    private final Process m_Process;

    public NormaliserProcess(Process process)
    {
        m_Process = process;
    }

    public NormalisedResultsParser createNormalisedResultsParser(Logger logger)
    {
        return new NormalisedResultsParser(m_Process.getInputStream(), logger);
    }

    public LengthEncodedWriter createProcessWriter()
    {
        return new LengthEncodedWriter(m_Process.getOutputStream());
    }

    public void closeOutputStream() throws IOException
    {
        m_Process.getOutputStream().close();
    }
}
