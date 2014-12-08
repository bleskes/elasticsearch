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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.input.LengthEncodedWriter;

/**
 * A writer for sending control messages to the C++ autodetect process.
 * The data written to output is length encoded.
 */
public class ControlMsgToProcessWriter
{
    /**
     * This should be the same size as the buffer in the C++ autodetect process.
     */
    private final static int FLUSH_SPACES_LENGTH = 8192;

    /**
     * This must match the code defined in the api::CAnomalyDetector C++ class.
     */
    private final static String INTERIM_MESSAGE_CODE = "i";

    private final LengthEncodedWriter m_LengthEncodedWriter;
    private final AnalysisConfig m_AnalysisConfig;

    public ControlMsgToProcessWriter(OutputStream os,
            AnalysisConfig analysisConfig)
    {
        m_LengthEncodedWriter = new LengthEncodedWriter(os);
        m_AnalysisConfig = Objects.requireNonNull(analysisConfig);
    }


    /**
     * Send an instruction to calculate interim results to the C++ autodetect
     * process.
     * @throws IOException
     */
    public void writeCalcInterimMessage() throws IOException
    {
        writeMessage(INTERIM_MESSAGE_CODE);
        m_LengthEncodedWriter.flush();
    }


    /**
     * Send a flush message to the C++ autodetect process.
     * @throws IOException
     */
    public void writeFlushMessage() throws IOException
    {
        char[] spaces = new char[FLUSH_SPACES_LENGTH];
        Arrays.fill(spaces, ' ');
        writeMessage(new String(spaces));
        m_LengthEncodedWriter.flush();
    }


    /**
     * Transform the supplied control message to length encoded values and
     * write to the OutputStream.
     * The number of blank fields to make up a full record is deduced from
     * <code>m_AnalysisConfig</code>.
     * @param message The control message to write.
     * @throws IOException
     */
    private void writeMessage(String message) throws IOException
    {
        List<String> analysisFields = m_AnalysisConfig.analysisFields();

        m_LengthEncodedWriter.writeNumFields(analysisFields.size() + 2);

        // Write blank values for all analysis fields and the time
        for (int i = -1; i < analysisFields.size(); ++i)
        {
            m_LengthEncodedWriter.writeField("");
        }

        // The control field comes last
        m_LengthEncodedWriter.writeField(message);
    }

}
