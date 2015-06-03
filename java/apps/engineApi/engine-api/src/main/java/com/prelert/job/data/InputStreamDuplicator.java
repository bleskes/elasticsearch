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
package com.prelert.job.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Reads from a single InputStream and writes the data
 * to multiple OutputStreams.
 * Their is not thread safety on this class the method
 * {@linkplain #addOutput(OutputStream)} should not be
 * called after {@linkplain #duplicate()} has started
 */
public class InputStreamDuplicator
{
    private static final Logger LOGGER = Logger.getLogger(InputStreamDuplicator.class);

    private static final int BUFFER_SIZE = 131072; // 128K

    private InputStream m_Input;
    private List<OutputStream> m_Outputs;

    public InputStreamDuplicator(InputStream input)
    {
        m_Input = input;
        m_Outputs = new ArrayList<>();
    }

    /**
     * Add an output stream.
     * This method should not be called after {@linkplain #run()} has started
     * @param output
     */
    public void addOutput(OutputStream output)
    {
        m_Outputs.add(output);
    }


    public void duplicate()
    {
        int n;
        byte[] buffer = new byte[BUFFER_SIZE];
        try
        {
            while ((n = m_Input.read(buffer)) > -1)
            {
                writeToOutputs(buffer, n);
            }

            // close streams
            Iterator<OutputStream> iter = m_Outputs.iterator();
            while (iter.hasNext())
            {
                iter.next().close();
            }
        }
        catch (IOException e)
        {
            LOGGER.warn("IOException reading input", e);
        }

        LOGGER.info("Duplicate write stream finished");
    }

    private void writeToOutputs(byte[] buffer, int byteCount)
    {
        Iterator<OutputStream> iter = m_Outputs.iterator();
        while (iter.hasNext())
        {
            OutputStream os = iter.next();
            try
            {
                os.write(buffer, 0, byteCount);
            }
            catch (IOException e)
            {
                LOGGER.error("Exception writing duplicate stream. " +
                            "No more data will be written to the stream", e);
                iter.remove();
            }
        }
    }

}


