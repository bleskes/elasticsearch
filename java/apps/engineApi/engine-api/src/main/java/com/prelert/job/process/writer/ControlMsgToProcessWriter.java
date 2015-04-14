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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;

/**
 * A writer for sending control messages to the C++ autodetect process.
 * The data written to output is length encoded.
 */
public class ControlMsgToProcessWriter
{
    /**
     * This should be the same size as the buffer in the C++ autodetect process.
     */
    private static final int FLUSH_SPACES_LENGTH = 8192;

    /**
     * This must match the code defined in the api::CAnomalyDetector C++ class.
     */
    private static final String FLUSH_MESSAGE_CODE = "f";

    /**
     * This must match the code defined in the api::CAnomalyDetector C++ class.
     */
    private static final String INTERIM_MESSAGE_CODE = "i";

    /**
     * This must match the code defined in the api::CAnomalyDetector C++ class.
     */
    private static final String RESET_BUCKETS_MESSAGE_CODE = "r";

    /**
     * An number to uniquely identify each flush so that subsequent code can
     * wait for acknowledgement of the correct flush.
     */
    private static AtomicLong ms_FlushNumber = new AtomicLong(1);

    private final LengthEncodedWriter m_LengthEncodedWriter;
    private final AnalysisConfig m_AnalysisConfig;

    public static ControlMsgToProcessWriter create(OutputStream os, AnalysisConfig analysisConfig)
    {
        return new ControlMsgToProcessWriter(new LengthEncodedWriter(os), analysisConfig);
    }

    ControlMsgToProcessWriter(LengthEncodedWriter outputStreamWriter, AnalysisConfig analysisConfig)
    {
        m_LengthEncodedWriter = Objects.requireNonNull(outputStreamWriter);
        m_AnalysisConfig = Objects.requireNonNull(analysisConfig);
    }

    /**
     * Send an instruction to calculate interim results to the C++ autodetect
     * process.
     * @param interimResultsParams Parameters indicating whether interim resuls should be written
     * and for which buckets
     * @throws IOException
     */
    public void writeCalcInterimMessage(InterimResultsParams interimResultsParams) throws IOException
    {
        if (interimResultsParams.shouldCalculate())
        {
            writeControlCodeFollowedByTimeRange(INTERIM_MESSAGE_CODE,
                    interimResultsParams.getStart(), interimResultsParams.getEnd());
        }
    }

    /**
     * Send a flush message to the C++ autodetect process.
     * This actually consists of two messages: one to carry the flush ID and the
     * other (which might not be processed until much later) to fill the buffers
     * and force prior messages through.
     * @return an ID for this flush that will be echoed back by the C++
     * autodetect process once it is complete.
     * @throws IOException
     */
    public String writeFlushMessage() throws IOException
    {
        String flushId = Long.toString(ms_FlushNumber.getAndIncrement());
        writeMessage(FLUSH_MESSAGE_CODE + flushId);

        char[] spaces = new char[FLUSH_SPACES_LENGTH];
        Arrays.fill(spaces, ' ');
        writeMessage(new String(spaces));

        m_LengthEncodedWriter.flush();
        return flushId;
    }


    public void writeResetBucketsMessage(DataLoadParams params) throws IOException
    {
        writeControlCodeFollowedByTimeRange(RESET_BUCKETS_MESSAGE_CODE, params.getStart(),
                params.getEnd());
    }

    private void writeControlCodeFollowedByTimeRange(String code, String start, String end)
            throws IOException
    {
        StringBuilder message = new StringBuilder(code);
        if (start.isEmpty() == false)
        {
            message.append(start);
            message.append(' ');
            message.append(end);
        }
        writeMessage(message.toString());
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

        // The fields consist of all the analysis fields plus the time and the
        // control field, hence + 2
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
