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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Reads from a single InputStream and writes the data
 * to multiple OutputStreams.
 * IMPORTANT: All outputs must be read in separate threads,
 * if the output isn't read the duplicate function will block
 * when the stream buffer fills up. If a single output isn't
 * being read none of the others will be.
 *
 * This is not a thread safe on class the method
 * {@linkplain #createDuplicateStream()} should not be
 * called after {@linkplain #duplicate()} has started
 */
public class InputStreamDuplicator
{
    private static final Logger LOGGER = Logger.getLogger(InputStreamDuplicator.class);

    private static final int BUFFER_SIZE = 131072; // 128K

    private final InputStream m_Input;
    private final List<OutputStream> m_Outputs;

    public InputStreamDuplicator(InputStream input)
    {
        m_Input = input;
        m_Outputs = new ArrayList<>();
    }

    /**
     * Create an input stream that the duplicate data is read from
     * This method should not be called after {@linkplain #duplicate()}
     * has started
     * @param output
     * @throws IOException
     */
    public InputStream createDuplicateStream() throws IOException
    {
        PipedInputStream pipedIn = new PipedInputStream();
        OutputStream pipedOut = new PipedOutputStream(pipedIn);
        m_Outputs.add(pipedOut);

        return pipedIn;
    }


    /**
     * Read the input and write to outputs.
     * Runs until all the input has been read
     */
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


