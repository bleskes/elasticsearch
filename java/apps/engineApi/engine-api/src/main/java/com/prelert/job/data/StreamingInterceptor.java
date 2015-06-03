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

package com.prelert.job.data;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

/**
 * This class is designed to sit invisibly in the middle of an inputstream
 * reading the input and writing a copy of the gzipped data to file
 * before forwarding it on. The {@link StreamingInterceptor#pump(InputStream)}
 * method reads from the input stream, gzips a copy of the data and writes it
 * to disk then passes the data on through a PipedOutputStream/InputStream pair.
 */
public class StreamingInterceptor
{
    private static final Logger LOGGER = Logger.getLogger(StreamingInterceptor.class);

    private static final int BUFFER_SIZE = 131072; // 128K

    private Path m_FileSink;
    private PipedOutputStream m_OutputStream;

    /**
     * The file to write the intercepted data to
     * @param sink
     */
    public StreamingInterceptor(Path fileSink)
    {
        m_FileSink = fileSink;
    }

    /**
     * Create the InputStream data will be made available on when
     * {@link StreamingInterceptor#pump(InputStream)} is called.
     * @return
     */
    public InputStream createStream()
    {
        PipedInputStream is = new PipedInputStream();

        try
        {
            m_OutputStream = new PipedOutputStream(is);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to create stream", e);
        }

        return is;
    }

    /**
     * Read from input and write a copy gzipped to disk then write to
     * an {@link PipedOutputStream} connected to the {@link InputStream}
     * returned by {@link #createStream()}
     * @param input
     */
    public void pump(InputStream input)
    {
        if (m_OutputStream == null)
        {
            throw new IllegalStateException("StreamingInterceptor cannot run the "
                    + "pump(InputStream) method before createStream() has been called");
        }



        try (OutputStream fileOutput = new GZIPOutputStream(
                new BufferedOutputStream(Files.newOutputStream(m_FileSink))))
                {
            int n;

            byte[] buffer = new byte[BUFFER_SIZE];
            while ((n = input.read(buffer)) > -1)
            {
                fileOutput.write(buffer, 0, n);
                m_OutputStream.write(buffer, 0, n);
            }
                }
        catch (FileNotFoundException e)
        {
            LOGGER.error("File not found", e);
        }
        catch (IOException e)
        {
            LOGGER.error("IoException in pump()", e);
        }
        finally
        {
            try
            {
                m_OutputStream.close();
            }
            catch (IOException e)
            {
                LOGGER.error("Exception closing the PipedOutputStream", e);
            }
        }
    }
}
